#include <jni.h>
#include <android/log.h>
#include <string>
#include <thread>
#include <atomic>
#include <mutex>
#include <algorithm>
#include "llama.h"

#define TAG "RustSensei-LLM"
#define LOG_I(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOG_E(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// Inline batch helpers (avoid linking to llama.cpp's common library)
static void batch_add(llama_batch & batch, llama_token token, llama_pos pos,
                      const std::vector<llama_seq_id> & seq_ids, bool logits) {
    batch.token[batch.n_tokens]  = token;
    batch.pos[batch.n_tokens]    = pos;
    batch.n_seq_id[batch.n_tokens] = (int32_t)seq_ids.size();
    for (size_t j = 0; j < seq_ids.size(); j++) {
        batch.seq_id[batch.n_tokens][j] = seq_ids[j];
    }
    batch.logits[batch.n_tokens] = logits ? 1 : 0;
    batch.n_tokens++;
}

static void batch_clear(llama_batch & batch) {
    batch.n_tokens = 0;
}

static llama_model * model = nullptr;
static llama_context * ctx = nullptr;
static llama_sampler * smpl = nullptr;
static std::atomic<bool> stop_generation{false};
static std::mutex model_mutex;

// KV cache reuse: track tokens currently in the cache
static std::vector<llama_token> cached_tokens;

// Sampler reuse: track last parameters to avoid recreation
static float last_temperature = -1.0f;
static float last_top_p = -1.0f;

// Detect optimal thread count: use performance cores (big + medium)
static int get_optimal_threads() {
    int n_cores = (int)std::thread::hardware_concurrency();
    // On typical ARM big.LITTLE: use half the cores (the big ones)
    // Pixel 6: 2xX1 + 2xA76 + 4xA55 = 8 cores, use 4 big ones
    // Pixel 8 Pro: 1xX3 + 4xA715 + 3xA510 = 8 cores, use 5 big ones
    // Heuristic: use n_cores - 2, clamped to [2, 6]
    int n_threads = std::max(2, std::min(n_cores - 2, 6));
    LOG_I("Hardware cores: %d, using %d threads", n_cores, n_threads);
    return n_threads;
}

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_sylvester_rustsensei_llm_LlamaEngine_loadModelNative(
        JNIEnv *env, jobject /* this */,
        jstring model_path, jint context_size) {
    std::lock_guard<std::mutex> lock(model_mutex);

    if (model != nullptr) {
        LOG_I("Model already loaded, unloading first");
        if (ctx) { llama_free(ctx); ctx = nullptr; }
        if (smpl) { llama_sampler_free(smpl); smpl = nullptr; }
        llama_model_free(model);
        model = nullptr;
    }

    const char *path = env->GetStringUTFChars(model_path, nullptr);
    LOG_I("Loading model from: %s", path);

    // Model params — use mmap for faster loading and lower memory
    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = 0;
    model_params.use_mmap = true;

    model = llama_model_load_from_file(path, model_params);
    env->ReleaseStringUTFChars(model_path, path);

    if (!model) {
        LOG_E("Failed to load model");
        return JNI_FALSE;
    }

    int n_threads = get_optimal_threads();

    // Context params — enable flash attention for speed
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = context_size;
    ctx_params.n_batch = 512;
    ctx_params.n_threads = n_threads;
    ctx_params.n_threads_batch = n_threads;
    ctx_params.flash_attn = true;

    ctx = llama_init_from_model(model, ctx_params);
    if (!ctx) {
        // Flash attention might not be supported, retry without it
        LOG_I("Flash attention failed, retrying without it");
        ctx_params.flash_attn = false;
        ctx = llama_init_from_model(model, ctx_params);
    }

    if (!ctx) {
        LOG_E("Failed to create context");
        llama_model_free(model);
        model = nullptr;
        return JNI_FALSE;
    }

    // Initialize sampler (will be reconfigured before each generation)
    smpl = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(smpl, llama_sampler_init_top_p(0.9f, 1));
    llama_sampler_chain_add(smpl, llama_sampler_init_temp(0.7f));
    llama_sampler_chain_add(smpl, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

    LOG_I("Model loaded successfully, context size: %d, threads: %d", context_size, n_threads);
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_com_sylvester_rustsensei_llm_LlamaEngine_generateNative(
        JNIEnv *env, jobject thiz,
        jstring prompt, jint max_tokens, jfloat temperature, jfloat top_p) {
    // P0 Fix #2: mutex protects cached_tokens and ctx from concurrent clearCache
    std::lock_guard<std::mutex> lock(model_mutex);
    if (!model || !ctx) {
        jclass cls = env->GetObjectClass(thiz);
        jmethodID onError = env->GetMethodID(cls, "onNativeError", "(Ljava/lang/String;)V");
        jstring err = env->NewStringUTF("Model not loaded");
        env->CallVoidMethod(thiz, onError, err);
        return;
    }

    stop_generation.store(false);

    const char *prompt_str = env->GetStringUTFChars(prompt, nullptr);
    std::string prompt_cpp(prompt_str);
    env->ReleaseStringUTFChars(prompt, prompt_str);

    jclass cls = env->GetObjectClass(thiz);
    jmethodID onToken = env->GetMethodID(cls, "onNativeToken", "(Ljava/lang/String;)V");
    jmethodID onComplete = env->GetMethodID(cls, "onNativeComplete", "()V");
    jmethodID onError = env->GetMethodID(cls, "onNativeError", "(Ljava/lang/String;)V");

    // Sampler reuse: only recreate when parameters change
    if (temperature != last_temperature || top_p != last_top_p) {
        if (smpl) { llama_sampler_free(smpl); }
        smpl = llama_sampler_chain_init(llama_sampler_chain_default_params());
        llama_sampler_chain_add(smpl, llama_sampler_init_top_p(top_p, 1));
        llama_sampler_chain_add(smpl, llama_sampler_init_temp(temperature));
        llama_sampler_chain_add(smpl, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));
        last_temperature = temperature;
        last_top_p = top_p;
        LOG_I("Sampler recreated: temp=%.2f top_p=%.2f", temperature, top_p);
    } else {
        // Reset sampler state without recreating
        llama_sampler_reset(smpl);
    }

    // Tokenize the prompt
    const llama_vocab * vocab = llama_model_get_vocab(model);
    const int n_prompt_max = (int)prompt_cpp.length() + 256;
    std::vector<llama_token> tokens(n_prompt_max);
    const int n_tokens = llama_tokenize(vocab, prompt_cpp.c_str(), (int32_t)prompt_cpp.length(),
                                         tokens.data(), n_prompt_max, true, true);
    if (n_tokens < 0) {
        jstring err = env->NewStringUTF("Failed to tokenize prompt");
        env->CallVoidMethod(thiz, onError, err);
        return;
    }
    tokens.resize(n_tokens);

    // KV cache reuse: find common prefix with cached tokens
    int common_len = 0;
    int min_len = std::min((int)cached_tokens.size(), n_tokens);
    for (int i = 0; i < min_len; i++) {
        if (cached_tokens[i] == tokens[i]) {
            common_len++;
        } else {
            break;
        }
    }

    if (common_len > 0 && common_len < (int)cached_tokens.size()) {
        // Remove KV cache entries after the common prefix
        llama_kv_self_seq_rm(ctx, 0, common_len, -1);
        LOG_I("KV cache reuse: %d common tokens, removed %d stale entries",
              common_len, (int)cached_tokens.size() - common_len);
    } else if (common_len == 0) {
        // No common prefix, clear entire cache
        llama_kv_self_clear(ctx);
        LOG_I("KV cache cleared: no common prefix");
    } else {
        LOG_I("KV cache fully reused: %d tokens", common_len);
    }

    // Only decode tokens from common_len onward
    int decode_start = common_len;
    int tokens_to_decode = n_tokens - decode_start;
    LOG_I("Prompt: %d tokens, decoding %d new tokens (skipped %d cached)",
          n_tokens, tokens_to_decode, decode_start);

    const int n_batch = 512;
    llama_batch batch = llama_batch_init(n_batch, 0, 1);

    for (int i = decode_start; i < n_tokens; i += n_batch) {
        batch_clear(batch);
        int n_chunk = std::min(n_batch, n_tokens - i);
        for (int j = 0; j < n_chunk; j++) {
            batch_add(batch, tokens[i + j], i + j, { 0 }, (i + j == n_tokens - 1));
        }

        if (llama_decode(ctx, batch) != 0) {
            llama_batch_free(batch);
            jstring err = env->NewStringUTF("Failed to decode prompt");
            env->CallVoidMethod(thiz, onError, err);
            return;
        }

        if (stop_generation.load()) {
            llama_batch_free(batch);
            // Update cached_tokens with what we decoded so far
            cached_tokens.assign(tokens.begin(), tokens.begin() + i + n_chunk);
            env->CallVoidMethod(thiz, onComplete);
            return;
        }
    }

    // Generate tokens
    int n_cur = n_tokens;
    char token_buf[256];
    std::vector<llama_token> generated_tokens;

    for (int i = 0; i < max_tokens; i++) {
        if (stop_generation.load()) {
            LOG_I("Generation stopped by user");
            break;
        }

        llama_token new_token = llama_sampler_sample(smpl, ctx, -1);

        // Check for end of generation
        if (llama_vocab_is_eog(vocab, new_token)) {
            LOG_I("End of generation token received");
            break;
        }

        generated_tokens.push_back(new_token);

        // Convert token to text
        int n = llama_token_to_piece(vocab, new_token, token_buf, sizeof(token_buf), 0, true);
        if (n > 0) {
            token_buf[n] = '\0';
            jstring tokenStr = env->NewStringUTF(token_buf);
            if (tokenStr) {
                env->CallVoidMethod(thiz, onToken, tokenStr);
                env->DeleteLocalRef(tokenStr);
            }
        }

        // Prepare batch for next token
        batch_clear(batch);
        batch_add(batch, new_token, n_cur, { 0 }, true);

        if (llama_decode(ctx, batch) != 0) {
            jstring err = env->NewStringUTF("Failed to decode token");
            env->CallVoidMethod(thiz, onError, err);
            llama_batch_free(batch);
            return;
        }
        n_cur++;
    }

    // Update cached_tokens with prompt + generated tokens for next reuse
    cached_tokens = tokens;
    cached_tokens.insert(cached_tokens.end(), generated_tokens.begin(), generated_tokens.end());
    LOG_I("Cache updated: %d tokens total (%d prompt + %d generated)",
          (int)cached_tokens.size(), n_tokens, (int)generated_tokens.size());

    llama_batch_free(batch);
    env->CallVoidMethod(thiz, onComplete);
}

JNIEXPORT void JNICALL
Java_com_sylvester_rustsensei_llm_LlamaEngine_stopGenerationNative(
        JNIEnv * /* env */, jobject /* this */) {
    LOG_I("Stop generation requested");
    stop_generation.store(true);
}

JNIEXPORT void JNICALL
Java_com_sylvester_rustsensei_llm_LlamaEngine_clearCacheNative(
        JNIEnv * /* env */, jobject /* this */) {
    // P0 Fix #2: mutex protects concurrent access with generateNative
    std::lock_guard<std::mutex> lock(model_mutex);
    if (ctx) {
        llama_kv_self_clear(ctx);
        cached_tokens.clear();
        LOG_I("KV cache and token cache cleared");
    }
}

JNIEXPORT void JNICALL
Java_com_sylvester_rustsensei_llm_LlamaEngine_unloadModelNative(
        JNIEnv * /* env */, jobject /* this */) {
    std::lock_guard<std::mutex> lock(model_mutex);
    LOG_I("Unloading model");
    if (smpl) { llama_sampler_free(smpl); smpl = nullptr; }
    if (ctx) { llama_free(ctx); ctx = nullptr; }
    if (model) { llama_model_free(model); model = nullptr; }
    cached_tokens.clear();
    last_temperature = -1.0f;
    last_top_p = -1.0f;
}

JNIEXPORT jboolean JNICALL
Java_com_sylvester_rustsensei_llm_LlamaEngine_isModelLoadedNative(
        JNIEnv * /* env */, jobject /* this */) {
    return (model != nullptr && ctx != nullptr) ? JNI_TRUE : JNI_FALSE;
}

} // extern "C"
