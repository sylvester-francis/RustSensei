```
██████╗ ██╗   ██╗███████╗████████╗███████╗███████╗███╗   ██╗███████╗███████╗██╗
██╔══██╗██║   ██║██╔════╝╚══██╔══╝██╔════╝██╔════╝████╗  ██║██╔════╝██╔════╝██║
██████╔╝██║   ██║███████╗   ██║   ███████╗█████╗  ██╔██╗ ██║███████╗█████╗  ██║
██╔══██╗██║   ██║╚════██║   ██║   ╚════██║██╔══╝  ██║╚██╗██║╚════██║██╔══╝  ██║
██║  ██║╚██████╔╝███████║   ██║   ███████║███████╗██║ ╚████║███████║███████╗██║
╚═╝  ╚═╝ ╚═════╝ ╚══════╝   ╚═╝   ╚══════╝╚══════╝╚═╝  ╚═══╝╚══════╝╚══════╝╚═╝
```

**An offline Android app that teaches Rust programming through an on-device AI tutor, interactive exercises, quizzes, and a structured book — all running locally via LiteRT.**

> No internet required after the initial model download. No data leaves your device.

[![GitHub release](https://img.shields.io/github/v/release/sylvester-francis/RustSensei)](https://github.com/sylvester-francis/RustSensei/releases)
![Kotlin](https://img.shields.io/badge/Kotlin-2.1-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?logo=jetpackcompose&logoColor=white)
![LiteRT](https://img.shields.io/badge/LiteRT-GPU_Accelerated-FF6F00?logo=tensorflow&logoColor=white)
![Android](https://img.shields.io/badge/Android-8.0+-3DDC84?logo=android&logoColor=white)
![License](https://img.shields.io/badge/License-Apache_2.0-blue)

[Download APK](#install) · [Features](#features) · [Model](#model) · [Architecture](#architecture) · [Build](#build-from-source) · [Privacy](#privacy)

---

## Why RustSensei?

Most programming tutors require a constant internet connection and send your data to cloud servers. RustSensei takes a different approach:

**Everything runs on your device.** A fine-tuned 1B parameter LLM runs locally via Google's LiteRT with GPU acceleration. Your conversations, progress, and learning data never leave your phone. Pair that with a full Rust curriculum — book, exercises, quizzes, flashcards — and you have a complete Rust learning environment in your pocket.

## Features

- **On-Device AI Tutor** — fine-tuned 1B parameter LLM with GPU-accelerated streaming inference via LiteRT
- **Context-Aware Help** — ask about a book section or get help with an exercise, and the AI has full context
- **19-Chapter Rust Book** — from Getting Started to Macros, with code examples and reading progress tracking
- **97 Coding Exercises** — Rustlings-style challenges with syntax-highlighted editor, hints, and AI-powered code review
- **5 Topic Quizzes** (32 questions) — multiple choice, true/false, and code completion with score rings and detailed feedback
- **110+ Spaced Repetition Flashcards** — SM2-scheduled review system for long-term retention
- **5 Guided Learning Paths** — structured step-by-step progression through Rust concepts
- **12 Reference Guides** — cheat sheets, compiler errors, language comparisons, design patterns, glossary
- **Study Streaks** — animated flame, weekly activity dots, and daily goals for reading, exercises, and quizzes
- **Achievement Badges** — unlock milestones for reading, coding, quizzes, and streaks
- **Activity Heatmap** — GitHub-style weekly visualization of your learning activity
- **Conversation History** — persisted locally, accessible from a navigation drawer
- **Dark Terminal Aesthetic** — blue-tinted near-black surfaces with Rust Orange accent, monospace headings, neon accents

## Install

Download the latest signed APK from [GitHub Releases](https://github.com/sylvester-francis/RustSensei/releases):

1. Download `app-release.apk` from the latest release
2. On your Android device, enable **Settings > Install from Unknown Sources** for your browser
3. Open the downloaded APK and tap **Install**
4. Launch RustSensei and download the AI model from the **Settings** tab (~1.2 GB, one-time)

All learning content (book, exercises, quizzes, flashcards) works immediately — the model is only needed for the AI chat tutor.

## Model

Downloads from Hugging Face on first use:

| Model | Parameters | Quantization | Size | RAM |
|-------|-----------|-------------|------|-----|
| [Rust Mentor 1B](https://huggingface.co/sylvester-francis/rust-mentor-1b-mobile-LiteRT) | 1B | Q8 | ~1.2 GB | ~3 GB |

The model runs entirely on-device using the GPU via LiteRT's OpenCL delegate. No data is sent to any server.

## Requirements

| Requirement | Minimum |
|-------------|---------|
| Android | 8.0+ (API 26) |
| GPU | Required (tested on Pixel 8 Pro / Tensor G3) |
| Storage | ~1.2 GB for the model |
| RAM | ~3 GB available for inference |

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

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin 2.1 |
| UI | Jetpack Compose (100% Compose, no XML) |
| Design System | Material 3 (`androidx.compose.material3`) |
| AI Inference | LiteRT (formerly TFLite) — GPU-accelerated via OpenCL |
| Database | Room (chat, progress, flashcards, notes) |
| Navigation | Navigation Compose with nested tab navigation |
| Async | Coroutines + Flow for streaming inference and reactive UI |
| Network | OkHttp for model download with resume support |

## Build from Source

**Prerequisites:** Android Studio, JDK 11+

```bash
git clone https://github.com/sylvester-francis/RustSensei.git
cd RustSensei
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Acknowledgments

| Project | License | Usage |
|---------|---------|-------|
| [The Rust Programming Language](https://github.com/rust-lang/book) | MIT / Apache 2.0 | Book content adapted for mobile |
| [Rustlings](https://github.com/rust-lang/rustlings) | MIT | Exercise concepts and structure |
| [Google LiteRT](https://github.com/google-ai-edge/LiteRT) | Apache 2.0 | On-device GPU-accelerated inference |

## Privacy

RustSensei collects no data. No analytics, no tracking, no accounts, no telemetry. Everything runs on-device. See [Privacy Policy](PRIVACY_POLICY.md).

## Author

**Sylvester Ranjith Francis**

[![GitHub](https://img.shields.io/badge/GitHub-sylvester--francis-181717?logo=github)](https://github.com/sylvester-francis)
[![Hugging Face](https://img.shields.io/badge/Hugging_Face-sylvester--francis-FFD21E?logo=huggingface&logoColor=black)](https://huggingface.co/sylvester-francis)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-sylvesterranjith-0A66C2?logo=linkedin)](https://www.linkedin.com/in/sylvesterranjith/)
[![Substack](https://img.shields.io/badge/Substack-techwithsyl-FF6719?logo=substack&logoColor=white)](https://techwithsyl.substack.com/)
[![Medium](https://img.shields.io/badge/Medium-sylvesterranjithfrancis-000000?logo=medium)](https://medium.com/@sylvesterranjithfrancis)
[![Instagram](https://img.shields.io/badge/Instagram-techwithsyl-E4405F?logo=instagram&logoColor=white)](https://www.instagram.com/techwithsyl)

## License

Apache License 2.0 — see [LICENSE](LICENSE) for details.
