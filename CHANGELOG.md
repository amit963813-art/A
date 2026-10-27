# Changelog

All notable changes to the **Personal AI Assistant** Android project are documented in this file.

## [1.0.0] - 2026-10-27

### Added (Minimum Version Foundation)
- **Project Saving & Ownership Hub**: Added first-launch and in-app options for Connect GitHub, Download Source Code / Data ZIP Backup, and Continue Without Connecting.
- **Dynamic Assistant Identity**: Added customizable Assistant Name supporting any Hindi, Hinglish, or English name stored in Jetpack DataStore without affecting Room records.
- **Hindi/Hinglish Chat & Voice Engine**: Added multi-session chat history in Room, native Android `SpeechRecognizer` voice input, and `TextToSpeech` spoken output with adjustable speed and pitch.
- **Editable Custom Commands**: Pre-loaded 8 Hindi/Hinglish voice commands (*YouTube kholo*, *Camera kholo*, *Calculator kholo*, *Settings kholo*, *Time batao*, *Mera assistant kholo*, *Mujhe jawab bolkar do*, *Aaj ki attendance batao*) with an installed-app picker and action preview before saving.

### Added (Advanced Version Features)
- **Firebase AI Logic Integration**: Integrated `gemini-3.8-flash`, `gemini-3.5-flash-lite`, and `gemini-4-argon` with App Check attestation and automatic fallback to the Offline Local Command Engine.
- **Lock-Screen Assistant Foreground Service**: Added `AssistantForegroundService` with persistent notification and *"Talk to [Assistant Name]"* lock-screen button.
- **Configurable Wake Phrase**: Added custom wake-phrase detection during active voice listening with a modular `WakePhraseDetector` interface and one-tap Stop Listening controls.
- **Native Biometric Security**: Integrated `AndroidX BiometricPrompt` for protecting Memory Vault, Attendance Clock IN/OUT, and sensitive settings.
- **Personal Memory Vault**: Added local Room storage for facts, preferences, and notes with explicit save-permission prompts.
- **PagarBook Attendance & Salary Module**: Added daily IN/OUT tracking, working hours, overtime, advances, deductions, Wednesday default weekly holiday, and CSV/JSON backup & restore.
