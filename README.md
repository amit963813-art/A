# My Personal AI Assistant (Native Android - Kotlin & Jetpack Compose)

A genuine, customizable native Android AI Assistant and PagarBook-style Attendance & Salary Manager built with **Kotlin**, **Jetpack Compose**, **Room SQLite Database**, **Jetpack Preferences DataStore**, **AndroidX BiometricPrompt**, **TextToSpeech / SpeechRecognizer**, and **Firebase AI Logic (Gemini)**.

---

## Key Features

1. **Project Ownership & Export Options (Step 1)**
   - **Connect GitHub**: Push directly to a private or public GitHub repository from the AI Studio toolbar or verify repository connection status inside the Project Hub.
   - **Download Source Code & Backups**: Export the full project ZIP from AI Studio, or export/restore complete local data backups (JSON/CSV) and project documentation directly from your phone.
   - **Continue Without Connecting**: Full functionality works immediately without requiring GitHub credentials.

2. **Customizable Assistant Identity (Step 2)**
   - Never hardcodes any assistant name. Choose any Hindi, Hinglish, or English name (e.g., *Aarya*, *Raja*, *Veer*, *Nova*) during first launch or in **Settings > Personalization > Assistant Name**.
   - Changing the name updates all greetings, spoken responses, lock-screen notifications, and wake phrases while preserving 100% of your chat history, memories, and attendance data.

3. **Hindi, Hinglish & English AI Chat + Voice (Step 4)**
   - Powered by **Firebase AI Logic** (`gemini-3.8-flash` default, `gemini-3.5-flash-lite`, `gemini-4-argon`) secured via Firebase App Check (no secret API keys hardcoded in source code).
   - Spoken responses via Android native `TextToSpeech` (`hi-IN` & `en-IN`) and voice input via `SpeechRecognizer`.
   - Works in **Offline Local Command Mode** for supported phone commands, math calculations, time/date checks, and attendance queries when internet or AI is unavailable.

4. **Configurable Wake Phrase & Lock-Screen Assistant Button (Steps 5 & 6)**
   - Configure custom wake phrases (e.g., *"Hey Raja"*, *"Namaste Aarya"*).
   - Includes a modular `WakePhraseDetector` architecture and a foreground listening mode using Android `SpeechRecognizer`, with clear status indicators, a one-tap **Stop Listening** button, and transparent documentation on hardware DSP wake-word limits and battery usage.
   - Includes `AssistantForegroundService` displaying a persistent lock-screen notification with a **"Talk to [Assistant Name]"** button that respects device PIN/biometric security.

5. **Editable Custom Phone Commands (Step 7)**
   - Pre-configured with Hindi/Hinglish commands:
     - *"YouTube kholo"*
     - *"Camera kholo"*
     - *"Calculator kholo"*
     - *"Settings kholo"*
     - *"Time batao"*
     - *"Mera assistant kholo"*
     - *"Mujhe jawab bolkar do"*
     - *"Aaj ki attendance batao"*
   - Create, preview, edit, enable/disable, and delete custom commands or bind phrases to any installed launchable app on your phone.

6. **Real AndroidX Biometric Authentication (Step 8)**
   - Uses `androidx.biometric.BiometricPrompt` (`BIOMETRIC_STRONG | DEVICE_CREDENTIAL`).
   - Never simulates fingerprint scanning with fake graphics and never touches raw biometric templates.

7. **Personal Memory Vault (Step 9)**
   - Local-first Room storage for notes, preferences, and personal facts.
   - Prompts for user permission before saving new facts discovered in chat.
   - Chat history can be cleared independently without erasing saved memories.

8. **PagarBook-Style Attendance & Salary Module (Step 10)**
   - Single-employee daily attendance with optional Biometric Clock IN and Clock OUT.
   - Automatic working-hour and overtime calculation.
   - Configurable monthly base salary, daily standard hours, hourly overtime wage, and **Wednesday as the default weekly holiday**.
   - Tracks advances, bonuses, and deductions with CSV and JSON export/restore.

---

## Mobile-Friendly Build & Installation Instructions

### Option A: Install APK / Preview Directly from AI Studio
1. Open the project in your mobile browser in Google AI Studio Build.
2. Use the **Streaming Android Emulator** to preview and test live.
3. Tap the **Settings / Export** menu in the top bar and select **Generate APK** or **Download ZIP** to install the signed APK on your Android phone.

### Option B: Build Locally or in Termux / Android Studio / Code Assistant APK
1. Download and extract the project ZIP file.
2. Ensure JDK 17+ and Android SDK (API 36) are configured in your build environment.
3. Run the debug build:
   ```bash
   gradle :app:assembleDebug
   ```
4. The installable APK will be generated at:
   `app/build/outputs/apk/debug/app-debug.apk`
