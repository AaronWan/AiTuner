# AI Tuner 🎤

<div align="center">

![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20Kotlin- green?style=flat-square&logo=android)
![Language](https://img.shields.io/badge/Language-EN%20%7C%20ZH%20%7C%20ZH--TW%20%7C%20KO%20%7C%20FR-blue?style=flat-square&logo=googletranslate)
![License](https://img.shields.io/badge/License-MIT-lightgrey?style=flat-square)
![Min SDK](https://img.shields.io/badge/Min%20SDK-26%20%28Android%208.0%29-orange?style=flat-square)

**AI-powered vocal training companion — real-time pitch detection, singing scoring, and intelligent feedback.**

[Download APK](#download) · [Features](#features) · [Screenshots](#screenshots) · [Build from Source](#build-from-source)

</div>

---

## What is AI Tuner?

AI Tuner is a mobile app that helps singers practice more effectively. It listens to your voice in real time, detects the exact pitch you're singing, and gives you instant visual feedback. After each practice session, an AI coach analyzes your performance and gives personalized tips — no expensive vocal lessons required.

---

## Features <a name="features"></a>

### 🎯 Real-Time Tuner
- Displays cents deviation from the nearest note on an analog-style dial
- Supports **C major scale** and **dozenal numbered notation** (简谱)
- Configurable reference pitch (A4 = 440 Hz by default)
- Works offline for tuning; no network required

### 🎤 Singing Practice with AI Scoring
- Play an AI-generated melody or load your own score
- Sing along and get real-time pitch feedback on screen
- After finishing, the AI analyzes your performance and gives a **score + detailed feedback**
- Supports **streamed responses** for instant AI coaching mid-session

### 🤖 AI Chat (Music Theory Tutor)
- Ask anything about music theory, technique, or ear training
- Powered by any **OpenAI-compatible API** — use your own API key
- Built-in presets for popular providers: OpenAI, Claude, Ollama, or any custom endpoint
- Fully **privacy-respecting**: all data stays on your device; AI calls go directly from your phone to your chosen provider

### 🌐 Multi-Language Support
| Locale | Language |
|--------|----------|
| `en` | English |
| `zh` | 简体中文 |
| `zh-rTW` | 繁體中文 |
| `ko` | 한국어 |
| `fr` | Français |

---

## Screenshots <a name="screenshots"></a>

> TODO: Add screenshots here

<!--
| Tuner | Singing Practice | AI Chat |
|:-----:|:----------------:|:-------:|
| ![Tuner](docs/screens/tuner.png) | ![Practice](docs/screens/practice.png) | ![Chat](docs/screens/chat.png) |
-->

---

## Download <a name="download"></a>

Latest APK: **[AiTuner-v1.1.apk](./AiTuner-v1.1.apk)** (24 MB, Android 8.0+)

> Install: `adb install AiTuner-v1.1.apk` or transfer to your phone and open the APK.

---

## Build from Source <a name="build-from-source"></a>

### Requirements
- **JDK 17**
- **Android SDK** (API 34)
- **Gradle 8.2** (wrapper included)

### Quick Build

```bash
git clone https://github.com/AaronWan/AiTuner.git
cd AiTuner
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

---

## Project Structure

```
app/src/main/java/com/example/aituner/
├── MainActivity.kt              # Entry point + permission handling
├── AiTunerApp.kt                # Application class, crash/log helpers
├── audio/
│   ├── NoteFrequency.kt         # Note ↔ frequency conversion (12-TET)
│   ├── PitchDetector.kt         # YIN algorithm pitch detection from mic
│   └── TunerEngine.kt           # Combines detector + scoring logic
├── ai/
│   ├── AiProvider.kt            # Provider interface + config data classes
│   ├── AiProviderFactory.kt     # Factory to instantiate providers
│   ├── AiSettingsRepository.kt  # DataStore-backed AI config persistence
│   └── provider/
│       ├── OpenAIProvider.kt    # OpenAI-compatible /v1/chat/completions
│       ├── ClaudeProvider.kt    # Anthropic /v1/messages
│       └── OllamaProvider.kt    # Ollama local /v1/chat/completions
├── ui/
│   ├── screen/
│   │   ├── TunerScreen.kt       # Tuner UI
│   │   ├── TunerViewModel.kt
│   │   ├── SingPracticeScreen.kt # Singing practice + AI scoring
│   │   ├── SingPracticeViewModel.kt
│   │   ├── AiChatScreen.kt      # AI chat UI
│   │   ├── AiChatViewModel.kt
│   │   ├── SettingsScreen.kt    # AI provider + API key config
│   │   ├── ScoreModel.kt        # Score parsing from AI response
│   │   └── JianpuConverter.kt   # 简谱 → note name converter
│   ├── component/
│   │   └── TuningDial.kt        # Animated analog-style tuning dial
│   └── theme/
│       ├── Color.kt
│       └── Theme.kt
└── di/
    └── AppModule.kt             # Hilt dependency injection
```

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | **Kotlin 1.9** |
| UI | **Jetpack Compose** + Material 3 |
| Architecture | **MVVM** + **Hilt** DI |
| Async | **Kotlin Coroutines + Flow** |
| Pitch Detection | **YIN algorithm** (pure Kotlin) |
| AI Streaming | **OkHttp SSE** + Kotlin Flow |
| Local Storage | **DataStore Preferences** |
| Min Android | **API 26** (Android 8.0) |

---

## AI Provider Setup

The app supports any OpenAI-compatible API. After installing, go to **Settings** and enter:

| Field | Description |
|-------|-------------|
| Base URL | Your API endpoint (e.g. `https://api.openai.com/v1`) |
| API Key | Your secret key |
| Model | Model name (e.g. `gpt-4o-mini`, `claude-3-haiku`) |

No provider is pre-configured — everything is stored locally on your device.

---

## Contributing

Issues and pull requests welcome! Please read the project structure above before submitting PRs.

---

## License

MIT
