package com.example.core.security

import android.os.Build
import java.io.File

/**
 * Handles root detection, emulator detection, and runtime injection/hook checking (Frida, Xposed).
 */
object DeviceIntegrityDetector {

    /**
     * Scans for custom root binaries, test keys, and administrative superuser access.
     */
    fun isDeviceRooted(): Boolean {
        // Check Build Tags
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) {
            return true
        }

        // Check for typical SU binary locations
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        for (path in paths) {
            if (File(path).exists()) {
                return true
            }
        }

        // Execute 'su' test
        var process: Process? = null
        try {
            process = Runtime.getRuntime().exec(arrayOf("su"))
            return true // Executing 'su' succeeded
        } catch (t: Throwable) {
            // Succeeded if not rooted
        } finally {
            process?.destroy()
        }

        return false
    }

    /**
     * Determines whether the app is running within an emulated Android environment.
     */
    fun isRunningOnEmulator(): Boolean {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator")
    }

    /**
     * Inspects active stack traces to detect if known hook frameworks like Frida or Xposed are injected.
     */
    fun isHookFrameworkDetected(): Boolean {
        try {
            val ex = Exception("IntegrityCheck")
            val stackTrace = ex.stackTrace
            for (element in stackTrace) {
                val className = element.className.lowercase()
                if (className.contains("com.saurik.substrate") ||
                    className.contains("de.robv.android.xposed") ||
                    className.contains("frida")
                ) {
                    return true
                }
            }
        } catch (e: Exception) {
            // Ignored
        }
        return false
    }
}
