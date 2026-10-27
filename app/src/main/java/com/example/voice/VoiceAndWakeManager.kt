package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

/**
 * Modular interface for on-device wake-word engines.
 * Clearly separates continuous low-power DSP wake-word binaries from Android's standard SpeechRecognizer.
 */
interface WakePhraseEngine {
    val engineName: String
    val isContinuousDspHardwareEngine: Boolean
    val statusExplanation: String
    fun matchesWakePhrase(transcript: String, configuredWakePhrase: String, assistantName: String): Boolean
}

class StandardOnDeviceWakePhraseMatcher : WakePhraseEngine {
    override val engineName: String = "On-Device Transcript Wake Matcher + Modular DSP Hook"
    override val isContinuousDspHardwareEngine: Boolean = false
    override val statusExplanation: String =
        "Uses Android's built-in on-device/system SpeechRecognizer during active listening sessions without uploading continuous background audio to third-party servers. Note: Continuous 24/7 screen-off wake-word detection requires a device-specific DSP model binary (e.g., Porcupine/TFLite keyword spotter) and may be paused by Android 14+ battery optimizations. For reliable lock-screen access on any phone, enable the Lock-Screen Assistant Notification Button below."

    override fun matchesWakePhrase(
        transcript: String,
        configuredWakePhrase: String,
        assistantName: String
    ): Boolean {
        val cleanTranscript = transcript.lowercase(Locale.ROOT).trim()
        val cleanWake = configuredWakePhrase.lowercase(Locale.ROOT).trim()
        val cleanName = assistantName.lowercase(Locale.ROOT).trim()
        if (cleanTranscript.isBlank()) return false

        return cleanTranscript.contains(cleanWake) ||
            cleanTranscript.startsWith("hey $cleanName") ||
            cleanTranscript.startsWith("hello $cleanName") ||
            cleanTranscript.startsWith("namaste $cleanName") ||
            cleanTranscript.startsWith("sunno $cleanName") ||
            cleanTranscript == cleanName
    }
}

data class VoiceUiState(
    val isTtsReady: Boolean = false,
    val isSpeaking: Boolean = false,
    val isListening: Boolean = false,
    val isWakeSessionActive: Boolean = false,
    val partialTranscript: String = "",
    val lastRecognizedText: String = "",
    val statusMessage: String = "Microphone idle (Not recording)",
    val errorText: String? = null
)

class VoiceAndWakeManager(
    private val context: Context,
    private val onFinalSpeechResult: (transcript: String, wasWakeTriggered: Boolean) -> Unit
) {
    private val _state = MutableStateFlow(VoiceUiState())
    val state: StateFlow<VoiceUiState> = _state.asStateFlow()

    val wakePhraseEngine: WakePhraseEngine = StandardOnDeviceWakePhraseMatcher()

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null

    private var currentVoiceSpeed: Float = 1.0f
    private var currentVoicePitch: Float = 1.0f
    private var currentLanguageMode: String = "HINGLISH"
    private var currentWakePhrase: String = "Hey Aarya"
    private var currentAssistantName: String = "Aarya"

    init {
        initTextToSpeech()
    }

    private fun initTextToSpeech() {
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    applyTtsLocaleAndParams()
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _state.value = _state.value.copy(isSpeaking = true)
                        }

                        override fun onDone(utteranceId: String?) {
                            _state.value = _state.value.copy(isSpeaking = false)
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            _state.value = _state.value.copy(isSpeaking = false)
                        }
                    })
                    _state.value = _state.value.copy(isTtsReady = true)
                } else {
                    _state.value = _state.value.copy(
                        isTtsReady = false,
                        errorText = "Android TextToSpeech engine failed to initialize on this device."
                    )
                }
            }
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isTtsReady = false,
                errorText = "TextToSpeech unavailable: ${e.localizedMessage}"
            )
        }
    }

    fun updateVoicePreferences(
        speed: Float,
        pitch: Float,
        languageMode: String,
        wakePhrase: String,
        assistantName: String
    ) {
        currentVoiceSpeed = speed.coerceIn(0.5f, 2.0f)
        currentVoicePitch = pitch.coerceIn(0.5f, 2.0f)
        currentLanguageMode = languageMode
        currentWakePhrase = wakePhrase
        currentAssistantName = assistantName
        applyTtsLocaleAndParams()
    }

    private fun applyTtsLocaleAndParams() {
        val engine = tts ?: return
        val locale = when (currentLanguageMode) {
            "HINDI", "HINGLISH" -> Locale("hi", "IN")
            else -> Locale("en", "IN")
        }
        val result = engine.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            engine.setLanguage(Locale.US)
        }
        engine.setSpeechRate(currentVoiceSpeed)
        engine.setPitch(currentVoicePitch)
    }

    fun speak(text: String) {
        val cleanText = text
            .replace("**", "")
            .replace("`", "")
            .replace("#", "")
            .trim()
        if (cleanText.isBlank()) return
        val engine = tts
        if (engine == null || !_state.value.isTtsReady) {
            _state.value = _state.value.copy(
                errorText = "TextToSpeech is not ready on this device/emulator."
            )
            return
        }
        applyTtsLocaleAndParams()
        val utteranceId = UUID.randomUUID().toString()
        engine.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stopSpeaking() {
        tts?.stop()
        _state.value = _state.value.copy(isSpeaking = false)
    }

    fun startListening(isWakeCheckMode: Boolean = false) {
        stopSpeaking()
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _state.value = _state.value.copy(
                isListening = false,
                statusMessage = "Speech recognition service is not installed on this device/emulator.",
                errorText = "Speech recognition service is unavailable on this device. You can still type all Hindi/Hinglish commands."
            )
            return
        }

        try {
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            }
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _state.value = _state.value.copy(
                        isListening = true,
                        isWakeSessionActive = isWakeCheckMode,
                        partialTranscript = "",
                        errorText = null,
                        statusMessage = if (isWakeCheckMode) {
                            "Listening for wake phrase \"$currentWakePhrase\" or command..."
                        } else {
                            "Microphone active — Speak in Hindi, Hinglish, or English..."
                        }
                    )
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _state.value = _state.value.copy(
                        isListening = false,
                        statusMessage = "Processing spoken audio..."
                    )
                }

                override fun onError(error: Int) {
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
                        SpeechRecognizer.ERROR_CLIENT -> "Stopped listening."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                            "Network error during speech recognition."
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected. Tap the mic to try again."
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech heard."
                        else -> "Speech recognition ended."
                    }
                    _state.value = _state.value.copy(
                        isListening = false,
                        isWakeSessionActive = false,
                        statusMessage = "Microphone idle (Not recording)",
                        errorText = if (error == SpeechRecognizer.ERROR_CLIENT) null else msg
                    )
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val topText = matches?.firstOrNull()?.trim().orEmpty()
                    val wasWake = wakePhraseEngine.matchesWakePhrase(
                        topText,
                        currentWakePhrase,
                        currentAssistantName
                    )
                    // Strip wake phrase prefix if user said "Hey Raja time batao"
                    val strippedCommand = stripWakePrefix(topText, currentWakePhrase, currentAssistantName)
                    _state.value = _state.value.copy(
                        isListening = false,
                        isWakeSessionActive = false,
                        partialTranscript = "",
                        lastRecognizedText = topText,
                        statusMessage = "Microphone idle (Not recording)"
                    )
                    if (topText.isNotBlank()) {
                        onFinalSpeechResult(strippedCommand.ifBlank { topText }, wasWake)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    if (partial.isNotBlank()) {
                        _state.value = _state.value.copy(partialTranscript = partial)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val localeTag = when (currentLanguageMode) {
                "HINDI" -> "hi-IN"
                "HINGLISH" -> "hi-IN"
                else -> "en-IN"
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isListening = false,
                isWakeSessionActive = false,
                statusMessage = "Microphone idle (Not recording)",
                errorText = "Could not start SpeechRecognizer: ${e.localizedMessage}"
            )
        }
    }

    private fun stripWakePrefix(
        transcript: String,
        wakePhrase: String,
        assistantName: String
    ): String {
        val prefixes = listOf(
            wakePhrase,
            "hey $assistantName",
            "hello $assistantName",
            "namaste $assistantName",
            "sunno $assistantName"
        )
        var result = transcript.trim()
        for (p in prefixes) {
            if (result.lowercase(Locale.ROOT).startsWith(p.lowercase(Locale.ROOT))) {
                result = result.substring(p.length).trim().trimStart(',', '.', '!', '?')
                break
            }
        }
        return result
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
        } catch (_: Exception) {}
        _state.value = _state.value.copy(
            isListening = false,
            isWakeSessionActive = false,
            partialTranscript = "",
            statusMessage = "Microphone stopped (Not recording)"
        )
    }

    fun clearError() {
        _state.value = _state.value.copy(errorText = null)
    }

    fun release() {
        try {
            speechRecognizer?.destroy()
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
    }
}
