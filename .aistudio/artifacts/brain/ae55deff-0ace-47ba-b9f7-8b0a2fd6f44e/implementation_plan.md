# My Personal AI Assistant — Native Android AI Companion & PagarBook Suite

A genuine, modular, native Android application built with Kotlin and Jetpack Compose that delivers a customizable Hindi/Hinglish AI voice and text assistant, custom phone-control voice commands, lock-screen foreground activation, user-controlled memory vault, native biometric security, and a complete PagarBook-style attendance and salary tracker.

### User Review & Critical Decisions

> [!IMPORTANT]
> Please review the architectural and product choices below before we begin building the native Android project.

- **Step 1 Project Saving & Source Ownership**: We will build and preview the full native Android project immediately ("Continue Without Connecting" flow so you can test right away without blocking on credentials), while including a dedicated **Project Hub & Export Center** screen inside the app. From the AI Studio top bar (Settings/Export menu), you can push directly to a private GitHub repository or download the complete source ZIP and APK at any time; inside the app, you can also generate and export full JSON/CSV data backups, inspect the live `README.md` and `CHANGELOG.md` mobile build guides, and verify project status without ever exposing secrets or tokens in code.
- **Custom Identity & First-Launch Onboarding**: The assistant name is never hardcoded. On first launch, a welcoming onboarding card lets you enter any Hindi, Hinglish, or English assistant name (e.g., *Raja*, *Aarya*, *Veer*, *Nova*) and automatically configures your wake phrase (e.g., *"Hey Raja"*). Renaming the assistant later in **Settings > Personalization** updates greetings, voice responses, and wake phrases immediately without affecting chat history, memories, or attendance records.
- **AI Backend (`Firebase AI Logic` with `gemini-3.8-flash` / `gemini-3.5-flash-lite`) + Offline Local Command Engine**: We will provision **Firebase AI Logic** (secured by App Check debug/release attestation—zero hardcoded API keys in the APK) with model selection (`gemini-3.8-flash` default, `gemini-3.5-flash-lite` fast mode, `gemini-4-argon` reasoning mode) and transparent connection status. When offline or when AI is disabled, an on-device **Hindi/Hinglish Local Command & Utility Engine** handles app launching, time/date queries, attendance summaries, calculator expressions, and custom commands without internet access.
- **Wake Phrase & Lock-Screen Activation Boundaries**: Continuous low-power hardware DSP wake-word detection requires device-specific proprietary voice binaries, so the app implements an honest, modular `WakePhraseEngine` interface paired with an active **Foreground Voice Listening Service** (`SpeechRecognizer` + persistent lock-screen notification with a **"Talk to [Assistant Name]"** action button). It clearly displays microphone status, provides a one-tap **Stop Listening** button, and respects Android lock-screen and battery rules without secretly recording audio.

---

### 1. Overview & Core Concept

- **What It Does**: Acts as an all-in-one personal AI assistant and daily utility suite for Android. Users can converse in Hindi, Hinglish, or English via text or voice (`SpeechRecognizer` + `TextToSpeech`), trigger custom voice commands to launch installed apps or check daily stats, manage a private local Memory Vault with explicit save consent, protect sensitive actions with real `AndroidX BiometricPrompt`, and track single-employee daily attendance, overtime, advances, deductions, and monthly salary with Wednesday set as the default weekly holiday.
- **Target Audience / Persona**: Mobile-first Android power users and builders in India who want a transparent, customizable Hindi/Hinglish voice assistant combined with practical daily attendance/salary tracking and full source-code ownership.
- **Key Value**: Genuine native Android implementation—no fake biometric scanners, no hardcoded assistant names, no hidden cloud recording, and complete separation between local private data (Room + DataStore) and cloud AI inference.

---

### 2. User Experience & Visual Design

- **Key User Flows**:
  1. **First-Launch Project & Identity Setup**: Choose project saving mode (*Connect GitHub / Download Source ZIP Guide / Continue to Preview*) and set your custom Assistant Name, preferred language (*Hinglish*, *Hindi*, or *English*), voice pitch/speed, and theme preset.
  2. **AI Chat & Voice Studio**: Switch seamlessly between typing and tapping the animated microphone orb. Spoken responses play via Android `TextToSpeech` with a live waveform/speaking indicator. Includes multi-session chat history (create, rename, delete, clear separately from memory) and instant execution of offline local commands (e.g., *"YouTube kholo"*, *"Time batao"*, *"Aaj ki attendance batao"*).
  3. **Custom Commands & Wake/Lock-Screen Center**: Create, preview, edit, toggle, or delete Hindi/Hinglish voice triggers mapped to installed launchable apps, system intents (Camera, Calculator, Settings), or internal assistant actions. Start or stop the Foreground Lock-Screen Assistant Service with full visibility into microphone and notification status.
  4. **Personal Memory Vault**: View, search, add, edit, and delete personal facts, notes, and preferences. When the assistant detects a memorable fact during chat, it asks permission via an inline confirmation card before saving to the local database.
  5. **PagarBook Attendance & Salary Module**: Clock IN and Clock OUT with optional real `BiometricPrompt` verification. Automatically calculates daily working hours, overtime, advances, and deductions against configurable monthly/hourly wages (with Wednesday as the default weekly holiday), and exports/restores full CSV or JSON reports.
- **Visual Identity & Theme**:
  - *Aesthetic Direction*: **Cyber-Indigo & Warm Saffron Studio**—a high-craft, modern native Android interface with user-selectable theme presets (*Cyber Indigo Dark*, *Obsidian Saffron*, and *Daylight Studio*) plus dynamic Material 3 color support.
  - *Color Palette & Mood*: Deep obsidian-slate surfaces (`#0B0F19`, `#131A2A`), electric indigo primary accents (`#6366F1`), warm saffron/amber highlights (`#F59E0B`) for voice/wake activity and holiday badges, and emerald (`#10B981`) for active connection and attendance Clock-IN states.
  - *Typography & Hierarchy*: Local bundled Google Fonts via `font-util`—**Space Grotesk** for display headers, assistant branding, and salary metrics paired with **Plus Jakarta Sans** for clean bilingual Hindi/Hinglish/English chat readability and **JetBrains Mono** for code blocks, calculations, and command previews.
  - *Component Styling & Layout*: Edge-to-edge Material 3 layout with a 5-tab bottom `NavigationBar` (*Assistant*, *Commands*, *Memory*, *Attendance*, *Settings & Project*), layered tonal cards, pill status badges for AI/Mic/Biometric state, and adaptive `WindowInsets` padding.
- **Interactive Feedback & Motion**: Pulsing radial canvas rings around the voice microphone when listening, smooth spring-animated message cards, confirmation dialogs before sensitive intents (calls/SMS/deletions), and clear error banners when offline.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Local-First Storage with Room + Preferences DataStore**
  - *Chosen Approach*: Use **Jetpack Room** for structured relational data (Chat Sessions, Chat Messages, Custom Voice Commands, Personal Memory Items, Daily Attendance Logs, Salary Transactions) and **Jetpack DataStore** for user personalization settings (Assistant Name, Wake Phrase, Theme, Voice Speed/Pitch, AI Model, Biometric Toggles, Holiday & Wage Config).
  - *Why*: The prompt explicitly mandates Room for structured local data, DataStore for persistent settings, and keeping personal memory and attendance/salary data strictly local by default so renaming the assistant or restarting the app never loses user records.
  - *Alternatives Considered*: Cloud-only storage was rejected for attendance and private notes to honor the user's strict local-by-default privacy requirement.
- **Decision 2: Dual-Engine AI & Offline Command Architecture**
  - *Chosen Approach*: Route local phone commands (app launching via `PackageManager`, time/date, calculator math, attendance check, TTS toggles) through a deterministic on-device `LocalCommandProcessor` first, and route general knowledge, coding, and conversational Hindi/Hinglish queries through **Firebase AI Logic** (`gemini-3.8-flash` / `gemini-3.5-flash-lite`) with user-controlled context injection from the local Memory Vault.
  - *Why*: Ensures offline commands work reliably with zero latency or internet dependency while providing frontier Gemini intelligence when online, without pretending a large language model runs locally on the phone.
- **Decision 3: Authentic AndroidX BiometricPrompt & Foreground Service**
  - *Chosen Approach*: Use official `androidx.biometric:biometric` (`BiometricPrompt` with `BIOMETRIC_STRONG or DEVICE_CREDENTIAL`) hosted in a `FragmentActivity` (`MainActivity`) and a `ForegroundService` (`AssistantForegroundService` with `foregroundServiceType="microphone|specialUse"`) posting a lock-screen-compatible `NotificationCompat` action button.
  - *Why*: Meets Android 14+ security and background execution rules without fake fingerprint graphics or unsupported lock-screen bypass hacks.

---

### 4. Technical Architecture & Data Strategy *(Technical Reference)*

- **Architecture & Component Diagram**:
```
┌────────────────────────────────────────────────────────────────────────────┐
│                  MainActivity (FragmentActivity / Compose)                 │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌───────────────────┐  │
│  │ Onboarding & │ │ AI Chat &    │ │ Custom Cmds  │ │ PagarBook         │  │
│  │ Project Hub  │ │ Voice Studio │ │ & Wake Setup │ │ Attendance/Salary │  │
│  └──────┬───────┘ └──────┬───────┘ └──────┬───────┘ └─────────┬─────────┘  │
└─────────┼────────────────┼────────────────┼───────────────────┼────────────┘
          │                │                │                   │
          ▼                ▼                ▼                   ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                     MVVM ViewModels & Domain Engines                       │
│  ┌─────────────────────┐  ┌──────────────────────┐  ┌───────────────────┐  │
│  │ AssistantViewModel  │  │ Command & App Engine │  │ BiometricAuth     │  │
│  │ • Local Cmd Router  │  │ • PackageManager     │  │ Manager           │  │
│  │ • Firebase AI Logic │  │ • Intent Preview/Run │  │ • BiometricPrompt │  │
│  │ • TTS & SpeechRec   │  │ • WakePhraseEngine   │  └─────────┬─────────┘  │
│  └──────────┬──────────┘  └──────────┬───────────┘            │            │
└─────────────┼────────────────────────┼────────────────────────┼────────────┘
              │                        │                        │
              ▼                        ▼                        ▼
┌───────────────────────────────────────────┐  ┌─────────────────────────────┐
│      Local Persistence Layer (On-Device)  │  │ Background & System Layer   │
│  ┌─────────────────┐  ┌─────────────────┐ │  │ • AssistantForegroundService│
│  │ Room Database   │  │ DataStore Prefs │ │  │   (Lock-screen Notification │
│  │ • ChatSessions  │  │ • AssistantName │ │  │    & Stop-Listening Action) │
│  │ • ChatMessages  │  │ • WakePhrase    │ │  │ • CSV / JSON Backup &       │
│  │ • CustomCommands│  │ • Theme & Voice │ │  │   Project Docs Exporter     │
│  │ • MemoryItems   │  │ • AI Model      │ │  └─────────────────────────────┘
│  │ • AttendanceLog │  │ • Salary Config │ │
│  │ • SalaryLedger  │  │ • BiometricGate │ │
│  └─────────────────┘  └─────────────────┘ │
└───────────────────────────────────────────┘
```

- **Data Model & State**:
  - `AssistantSettings` (DataStore): `assistantName`, `hasCompletedOnboarding`, `projectSaveChoice`, `wakePhrase`, `isWakeEnabled`, `lockScreenNotificationVisibility`, `languageMode` (Hinglish/Hindi/English), `responseStyle`, `voiceSpeed`, `voicePitch`, `speakResponsesByDefault`, `aiModelName`, `themePreset`, `requireBiometricForMemory`, `requireBiometricForAttendance`, `requireBiometricForSettings`, `monthlyBaseSalary`, `hourlyOvertimeRate`, `standardWorkHoursPerDay`, `weeklyHolidayDayOfWeek` (default Wednesday = 3).
  - `ChatSessionEntity` & `ChatMessageEntity` (Room): Multi-session chat threads with timestamps, role (`USER`, `ASSISTANT`, `SYSTEM_CMD`), and optional proposed memory payload.
  - `CustomCommandEntity` (Room): `id`, `triggerPhrases` (comma-separated Hindi/Hinglish/English variants), `actionType` (`OPEN_APP`, `SYSTEM_INTENT`, `SPEAK_TIME`, `CHECK_ATTENDANCE`, `TOGGLE_TTS`, `CUSTOM_REPLY`), `targetPackageOrAction`, `requiresConfirmation`, `isEnabled`. Pre-populated with default commands (*"YouTube kholo"*, *"Camera kholo"*, *"Calculator kholo"*, *"Settings kholo"*, *"Time batao"*, *"Mera assistant kholo"*, *"Mujhe jawab bolkar do"*, *"Aaj ki attendance batao"*).
  - `MemoryItemEntity` (Room): `id`, `title`, `content`, `category` (`NOTE`, `PREFERENCE`, `FACT`), `createdAt`, `updatedAt`.
  - `AttendanceRecordEntity` & `SalaryAdjustmentEntity` (Room): Date-indexed `checkInTime`, `checkOutTime`, `workedMinutes`, `overtimeMinutes`, `status` (`PRESENT`, `HALF_DAY`, `ABSENT`, `WEEKLY_HOLIDAY`, `PAID_LEAVE`), `biometricVerified`, plus separate ledger entries for `ADVANCE`, `BONUS`, and `DEDUCTION`.

- **Interactive Component & State Mapping**:
  - **Project Saving & Onboarding Modal**: Lets the user select *Connect GitHub*, *Download Source Code*, or *Continue Without Connecting*, sets the initial assistant name, and persists choices in DataStore.
  - **Voice & Chat Controller**: Manages `SpeechRecognizer` lifecycle, runtime `RECORD_AUDIO` permission handling, `TextToSpeech` playback queue, local command interception, Memory Vault permission prompts, and `Firebase.ai.generativeModel` streaming with 60-second coroutine timeouts.
  - **Lock-Screen Foreground Service**: Starts/stops `AssistantForegroundService` with `POST_NOTIFICATIONS` permission verification, updates a persistent notification containing *"Talk to [Assistant Name]"* and *"Stop Listening"* actions, and launches `MainActivity` into voice-listening mode when tapped from the lock screen.
  - **Attendance & Salary Calculator**: Computes pro-rated daily salary, Wednesday weekly holidays, worked hours, overtime pay, advances, and net monthly payout in real time, with one-tap JSON/CSV export via Android's `ActivityResultContracts.CreateDocument` and Share sheet.
