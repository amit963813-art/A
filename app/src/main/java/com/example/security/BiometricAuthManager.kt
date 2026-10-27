package com.example.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

sealed interface BiometricCapability {
    data object Ready : BiometricCapability
    data object NoHardware : BiometricCapability
    data object HardwareUnavailable : BiometricCapability
    data object NoneEnrolled : BiometricCapability
    data class Unsupported(val reason: String) : BiometricCapability
}

sealed interface BiometricResult {
    data object Success : BiometricResult
    data object Cancelled : BiometricResult
    data class Lockout(val message: String) : BiometricResult
    data class Failed(val message: String) : BiometricResult
    data class HardwareUnavailable(val message: String) : BiometricResult
}

/**
 * Genuine AndroidX BiometricPrompt wrapper.
 * Never simulates fingerprint scans and never accesses raw biometric images or templates.
 */
object BiometricAuthManager {

    private const val ALLOWED_AUTHENTICATORS = BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL

    fun checkCapability(context: Context): BiometricCapability {
        val manager = BiometricManager.from(context)
        return when (manager.canAuthenticate(ALLOWED_AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricCapability.Ready
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricCapability.NoHardware
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricCapability.HardwareUnavailable
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricCapability.NoneEnrolled
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
                BiometricCapability.Unsupported("Security update required for biometric sensor.")
            else -> BiometricCapability.Unsupported("Device authentication is not configured.")
        }
    }

    fun capabilityLabel(capability: BiometricCapability): String = when (capability) {
        is BiometricCapability.Ready -> "Biometric / Device Credential Ready"
        is BiometricCapability.NoHardware -> "No biometric sensor detected on this device/emulator"
        is BiometricCapability.HardwareUnavailable -> "Biometric sensor temporarily unavailable"
        is BiometricCapability.NoneEnrolled -> "No fingerprint, face, or screen lock enrolled in Android Settings"
        is BiometricCapability.Unsupported -> capability.reason
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onResult: (BiometricResult) -> Unit
    ) {
        val capability = checkCapability(activity)
        if (capability !is BiometricCapability.Ready) {
            onResult(BiometricResult.HardwareUnavailable(capabilityLabel(capability)))
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onResult(BiometricResult.Success)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // BiometricPrompt stays open for retry on single failed touch, but we notify UI feedback
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    BiometricPrompt.ERROR_CANCELED -> {
                        onResult(BiometricResult.Cancelled)
                    }
                    BiometricPrompt.ERROR_LOCKOUT,
                    BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                        onResult(BiometricResult.Lockout(errString.toString()))
                    }
                    BiometricPrompt.ERROR_HW_NOT_PRESENT,
                    BiometricPrompt.ERROR_HW_UNAVAILABLE,
                    BiometricPrompt.ERROR_NO_BIOMETRICS,
                    BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL -> {
                        onResult(BiometricResult.HardwareUnavailable(errString.toString()))
                    }
                    else -> {
                        onResult(BiometricResult.Failed(errString.toString()))
                    }
                }
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
            .build()

        prompt.authenticate(promptInfo)
    }
}
