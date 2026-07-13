package com.example.ui.utils

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

fun showBiometricPrompt(context: Context, onSuccess: () -> Unit, onError: () -> Unit) {
    val executor = ContextCompat.getMainExecutor(context)
    val biometricPrompt = BiometricPrompt(context as FragmentActivity, executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError()
            }
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }
            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onError()
            }
        })
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Verify Identity")
        .setSubtitle("Confirm to start boosting")
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        .build()
    biometricPrompt.authenticate(promptInfo)
}
