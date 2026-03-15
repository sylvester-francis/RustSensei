# RustSensei

An offline Rust programming tutor for Android that runs a GGUF language model locally on-device using llama.cpp. No internet required after the initial model download.

## Features

- **Fully offline inference** — runs a Q4_K_M quantized GGUF model on-device via llama.cpp
- **Streaming chat** — token-by-token output as the model generates
- **Rust-focused tutoring** — system prompt tailored for developers coming from Go, Python, and TypeScript
- **Syntax-highlighted code blocks** — Rust keyword, type, string, comment, and macro highlighting with copy-to-clipboard
- **Markdown rendering** — bold, italic, inline code, bullet lists in responses
- **Conversation history** — persisted with Room, accessible from a navigation drawer
- **Configurable inference** — temperature, max tokens, and context length controls
- **Material 3 UI** — Rust-orange accent with dynamic color and dark/light theme support
- **16KB page size compatible** — native libraries aligned for Android 15+

## Architecture

```
app/src/main/
├── cpp/
│   ├── CMakeLists.txt              # Fetches + builds llama.cpp via FetchContent
│   └── llama-android.cpp           # JNI bridge (load, generate, stop, unload)
├── java/com/sylvester/rustsensei/
│   ├── MainActivity.kt
│   ├── RustSenseiApp.kt            # Navigation graph (Setup → Chat → Settings)
│   ├── RustSenseiApplication.kt    # Application class with Room DB
│   ├── data/
│   │   ├── AppDatabase.kt          # Room database
│   │   ├── ChatDao.kt              # DAO with Flow-based queries
│   │   ├── ChatMessage.kt          # Message entity
│   │   ├── ChatRepository.kt       # Repository layer
│   │   └── Conversation.kt         # Conversation entity
│   ├── llm/
│   │   ├── ChatTemplateFormatter.kt # Qwen3 <|im_start|> chat template
│   │   ├── InferenceConfig.kt      # Temperature, topP, maxTokens
│   │   ├── LlamaEngine.kt          # Kotlin JNI wrapper with Flow-based streaming
│   │   ├── ModelForegroundService.kt
│   │   └── ModelManager.kt         # HuggingFace download with progress
│   ├── ui/
│   │   ├── components/
│   │   │   ├── CodeBlock.kt        # Syntax-highlighted code with copy button
│   │   │   ├── InputBar.kt         # Text input + send/stop button
│   │   │   ├── MessageBubble.kt    # Chat bubbles with markdown parsing
│   │   │   ├── QuickPromptChips.kt # Suggested prompt chips
│   │   │   └── StreamingIndicator.kt
│   │   ├── screens/
│   │   │   ├── ChatScreen.kt       # Main chat with conversation drawer
│   │   │   ├── ModelSetupScreen.kt # Download + load flow
│   │   │   └── SettingsScreen.kt   # Inference config + data management
│   │   └── theme/
│   │       ├── Theme.kt            # Material 3 with Rust-orange accent
│   │       └── Type.kt
│   └── viewmodel/
│       ├── ChatViewModel.kt        # Chat state + inference orchestration
│       └── ModelViewModel.kt       # Download/load state machine
```

## Tech Stack

- **Kotlin** + **Jetpack Compose** (Material 3)
- **llama.cpp** compiled via CMake/NDK for arm64-v8a
- **Room** for conversation persistence
- **OkHttp** for model download
- **Coroutines + Flow** for streaming inference

## Model

Downloads from [sylvester-francis/rust-mentor-4b-GGUF](https://huggingface.co/sylvester-francis/rust-mentor-4b-GGUF) on first launch:

| File | Quantization | Size |
|------|-------------|------|
| `qwen3-4b.Q4_K_M.gguf` | Q4_K_M | ~2.5 GB |

## Requirements

- Android 8.0+ (API 26)
- arm64-v8a device (tested on Pixel 8 Pro)
- ~2.5 GB free storage for the model
- NDK 26+ installed in Android SDK

## Build

```bash
# Clone
git clone git@github.com:sylvester-francis/RustSensei.git
cd RustSensei

# Build (first build fetches llama.cpp source via CMake FetchContent)
./gradlew assembleDebug

# Install
adb install app/build/outputs/apk/debug/app-debug.apk
```

The first CMake build will download and compile llama.cpp — this takes a few minutes.

## Usage

1. Launch the app
2. Download the model (~2.5 GB, one-time)
3. Wait for the model to load into memory
4. Start chatting about Rust

The app is fully offline after the model download. No data leaves your device.

## License

MIT
