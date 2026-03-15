#include <jni.h>
#include <android/log.h>
#include <string>
#include <thread>
#include <atomic>
#include <mutex>
#include <algorithm>
#include <chrono>
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

// Thread configuration: separate counts for generation vs batch processing
static int n_threads_gen = 4;    // generation: use performance cores only
static int n_threads_batch = 6;  // batch/prefill: use all available cores

static void configure_threads() {
    int n_cores = (int)std::thread::hardware_concurrency();
    // Generation (sequential token decode): use big cores only
    // Pixel 8 Pro: 1x X3 + 4x A715 = 5 big/medium cores
    n_threads_gen = std::max(2, std::min(n_cores / 2 + 1, 6));
    // Batch processing (parallel prefill): use ALL cores aggressively
    n_threads_batch = std::max(4, n_cores - 1);
    LOG_I("Hardware cores: %d, gen threads: %d, batch threads: %d",
          n_cores, n_threads_gen, n_threads_batch);
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
        cached_tokens.clear();
        last_temperature = -1.0f;
        last_top_p = -1.0f;
    }

    const char *path = env->GetStringUTFChars(model_path, nullptr);
    LOG_I("Loading model from: %s (context: %d)", path, context_size);

    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = 0;
    model_params.use_mmap = true;

    model = llama_model_load_from_file(path, model_params);
    env->ReleaseStringUTFChars(model_path, path);

    if (!model) {
        LOG_E("Failed to load model");
        return JNI_FALSE;
    }

    configure_threads();

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = context_size;
    ctx_params.n_batch = 2048;          // PERF: large batch for fast prefill
    ctx_params.n_ubatch = 512;          // PERF: micro-batch for memory efficiency
    ctx_params.n_threads = n_threads_gen;
    ctx_params.n_threads_batch = n_threads_batch;  // PERF: more threads for prefill
    ctx_params.flash_attn = true;

    // KV cache quantization: q8_0 halves memory, minimal quality loss
    ctx_params.type_k = GGML_TYPE_Q8_0;
    ctx_params.type_v = GGML_TYPE_Q8_0;

    // NOTE: flash_attn on ARM CPU can be SLOWER than standard attention on some SoCs
    // (Tensor G3, older Snapdragon). Disable by default for CPU-only inference.
    ctx_params.flash_attn = false;

    LOG_I("Creating context: KV cache=q8_0, flash_attn=off...");
    ctx = llama_init_from_model(model, ctx_params);

    if (!ctx) {
        // Fall back to f16 KV cache
        LOG_I("q8_0 KV failed, falling back to f16...");
        ctx_params.type_k = GGML_TYPE_F16;
        ctx_params.type_v = GGML_TYPE_F16;
        ctx = llama_init_from_model(model, ctx_params);
    }

    if (!ctx) {
        LOG_E("Failed to create context with any configuration");
        llama_model_free(model);
        model = nullptr;
        return JNI_FALSE;
    }

    smpl = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(smpl, llama_sampler_init_top_p(0.9f, 1));
    llama_sampler_chain_add(smpl, llama_sampler_init_temp(0.7f));
    llama_sampler_chain_add(smpl, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

    bool has_kv_quant = (ctx_params.type_k != GGML_TYPE_F16);
    LOG_I("Model loaded: ctx=%d, batch=2048, gen_t=%d, batch_t=%d, kv=%s",
          context_size, n_threads_gen, n_threads_batch,
          has_kv_quant ? "q8_0" : "f16");
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_com_sylvester_rustsensei_llm_LlamaEngine_generateNative(
        JNIEnv *env, jobject thiz,
        jstring prompt, jint max_tokens, jfloat temperature, jfloat top_p) {
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

    // Sampler reuse
    if (temperature != last_temperature || top_p != last_top_p) {
        if (smpl) { llama_sampler_free(smpl); }
        smpl = llama_sampler_chain_init(llama_sampler_chain_default_params());
        llama_sampler_chain_add(smpl, llama_sampler_init_top_p(top_p, 1));
        llama_sampler_chain_add(smpl, llama_sampler_init_temp(temperature));
        llama_sampler_chain_add(smpl, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));
        last_temperature = temperature;
        last_top_p = top_p;
    } else {
        llama_sampler_reset(smpl);
    }

    // Tokenize
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

    auto t_start = std::chrono::high_resolution_clock::now();

    // KV cache reuse: find common prefix
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
        llama_kv_self_seq_rm(ctx, 0, common_len, -1);
        LOG_I("KV cache reuse: %d/%d tokens cached, decoding %d new",
              common_len, n_tokens, n_tokens - common_len);
    } else if (common_len == 0) {
        llama_kv_self_clear(ctx);
        LOG_I("KV cache miss: decoding all %d prompt tokens", n_tokens);
    } else {
        LOG_I("KV cache hit: all %d tokens cached", common_len);
    }

    // Prefill: decode new prompt tokens in large batches
    int decode_start = common_len;
    const int n_batch = 2048;  // PERF: match context batch size
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
            cached_tokens.assign(tokens.begin(), tokens.begin() + i + n_chunk);
            env->CallVoidMethod(thiz, onComplete);
            return;
        }
    }

    auto t_prefill = std::chrono::high_resolution_clock::now();
    auto prefill_ms = std::chrono::duration_cast<std::chrono::milliseconds>(t_prefill - t_start).count();
    int tokens_decoded = n_tokens - decode_start;
    float prefill_speed = tokens_decoded > 0 ? (tokens_decoded * 1000.0f / prefill_ms) : 0;
    LOG_I("Prefill: %d tokens in %lld ms (%.1f tok/s)", tokens_decoded, prefill_ms, prefill_speed);

    // Generation: decode one token at a time
    int n_cur = n_tokens;
    int n_generated = 0;
    char token_buf[256];
    std::vector<llama_token> generated_tokens;

    for (int i = 0; i < max_tokens; i++) {
        if (stop_generation.load()) {
            LOG_I("Generation stopped by user at token %d", i);
            break;
        }

        llama_token new_token = llama_sampler_sample(smpl, ctx, -1);

        if (llama_vocab_is_eog(vocab, new_token)) {
            LOG_I("EOG at token %d", i);
            break;
        }

        generated_tokens.push_back(new_token);
        n_generated++;

        int n = llama_token_to_piece(vocab, new_token, token_buf, sizeof(token_buf), 0, true);
        if (n > 0) {
            token_buf[n] = '\0';
            jstring tokenStr = env->NewStringUTF(token_buf);
            if (tokenStr) {
                env->CallVoidMethod(thiz, onToken, tokenStr);
                env->DeleteLocalRef(tokenStr);
            }
        }

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

    auto t_gen = std::chrono::high_resolution_clock::now();
    auto gen_ms = std::chrono::duration_cast<std::chrono::milliseconds>(t_gen - t_prefill).count();
    float gen_speed = n_generated > 0 ? (n_generated * 1000.0f / gen_ms) : 0;
    LOG_I("Generation: %d tokens in %lld ms (%.1f tok/s)", n_generated, gen_ms, gen_speed);
    LOG_I("Total: prefill %.1f tok/s + generation %.1f tok/s", prefill_speed, gen_speed);

    // Update cache for next reuse
    cached_tokens = tokens;
    cached_tokens.insert(cached_tokens.end(), generated_tokens.begin(), generated_tokens.end());

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
    std::lock_guard<std::mutex> lock(model_mutex);
    if (ctx) {
        llama_kv_self_clear(ctx);
        cached_tokens.clear();
        LOG_I("KV cache cleared");
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
