# RustSensei

An offline Android app that teaches Rust programming through an on-device AI tutor, interactive exercises, quizzes, and a structured book — all running locally via LiteRT. No internet required after the initial model download.

## Features

### AI Tutor
- **On-device LLM** — runs a fine-tuned 1B parameter model via Google's LiteRT (GPU-accelerated)
- **Streaming chat** — token-by-token output with inference stats (tok/s, latency)
- **Context-aware** — ask about a book section or get help with an exercise, and the AI has full context
- **Conversation history** — persisted locally, accessible from a navigation drawer

### Learning Content
- **19-chapter Rust book** — from Getting Started to Macros, with code examples and reading progress tracking
- **97 exercises** — Rustlings-style coding challenges with syntax-highlighted editor, hints, and AI-powered code review
- **5 topic quizzes** (32 questions) — multiple choice, true/false, and code completion with score rings, animated transitions, and detailed feedback
- **Spaced repetition flashcards** — SM2-scheduled review system with 110+ cards
- **5 guided learning paths** — structured step-by-step progression through Rust concepts
- **12 reference guides** — cheat sheets, compiler errors, language comparisons, design patterns, glossary

### Gamification
- **Study streaks** with animated flame and weekly activity dots
- **Daily goals** — read, exercise, and quiz targets with progress tracking
- **Achievement badges** — unlock milestones for reading, coding, quizzes, and streaks
- **Activity heatmap** — GitHub-style weekly visualization
- **Progress rings** — chapter, exercise, and quiz completion at a glance

### Design
- **Dark terminal aesthetic** — blue-tinted near-black surfaces with Rust Orange accent
- **Monospace headings** — code tutor identity throughout
- **5-tab navigation** — Home, Learn, Chat (full-screen), Practice, Settings
- **Material 3** — proper color tokens, responsive layout, accessibility support
- **Neon accents** — cyan for links/streaks, amber for warnings/XP, green for success

## Tech Stack

- **Kotlin** + **Jetpack Compose** (100% Compose, no XML)
- **Material 3** (`androidx.compose.material3`)
- **LiteRT** (formerly TFLite) for on-device GPU-accelerated inference
- **Room** for persistence (chat, progress, flashcards, notes)
- **Navigation Compose** with nested tab navigation
- **Coroutines + Flow** for streaming inference and reactive UI
- **OkHttp** for model download with resume support

## Model

Downloads from HuggingFace on first use:

| Model | Parameters | Quantization | Size | RAM |
|-------|-----------|-------------|------|-----|
| [Rust Mentor 1B](https://huggingface.co/sylvester-francis/rust-mentor-1b-mobile-LiteRT) | 1B | Q8 | ~1.2 GB | ~3 GB |

The model runs entirely on-device using the GPU via LiteRT's OpenCL delegate.

## Requirements

- Android 8.0+ (API 26)
- Device with GPU (tested on Pixel 8 Pro with Tensor G3)
- ~1.2 GB free storage for the model
- ~3 GB RAM available for inference

## Build

```bash
git clone git@github.com:sylvester-francis/RustSensei.git
cd RustSensei
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Usage

1. Launch the app
2. Go to **Settings** tab and download the AI model (~1.2 GB, one-time)
3. All learning features (book, exercises, quizzes, flashcards) work immediately without a model
4. Once downloaded, the **Chat** tab enables AI tutoring

Everything runs offline after the model download. No data leaves your device.

## Architecture

```
app/src/main/java/com/sylvester/rustsensei/
├── data/                    # Room database, DAOs, repositories
├── content/                 # Bundled JSON content (book, exercises, quizzes, reference)
├── llm/
│   ├── LiteRtEngine.kt     # LiteRT GPU inference with Flow-based streaming
│   ├── ModelManager.kt      # HuggingFace download with resume + SHA256 verification
│   └── ModelForegroundService.kt
├── ui/
│   ├── theme/               # Color system, typography, Material 3 theme
│   ├── components/          # 20 reusable composables (CodeBlock, InputBar, ProgressRing, etc.)
│   └── screens/             # 12 screens (Dashboard, Chat, Book, Exercises, Quiz, etc.)
└── viewmodel/               # 10 ViewModels with StateFlow
```

## Author

**Sylvester Ranjith Francis**

- [GitHub](https://github.com/sylvester-francis)
- [Hugging Face](https://huggingface.co/sylvester-francis)
- [LinkedIn](https://www.linkedin.com/in/sylvesterranjith/)
- [Substack](https://techwithsyl.substack.com/)
- [Medium](https://medium.com/@sylvesterranjithfrancis)
- [Instagram](https://www.instagram.com/techwithsyl)

## License

Apache License 2.0 — see [LICENSE](LICENSE) for details.
