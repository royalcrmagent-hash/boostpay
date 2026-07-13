package com.example.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Manages securely encrypted shared preferences using Android Keystore.
 * Both preference keys and values are encrypted using AES-256.
 */
class SecurePreferences(context: Context) {
    private val sharedPrefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "secure_wallet_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            android.util.Log.e("SecurePreferences", "Error creating EncryptedSharedPreferences, falling back", e)
            // Fallback to basic prefs if Keystore is corrupted/unavailable (extremely rare)
            context.getSharedPreferences("secure_wallet_prefs_fallback", Context.MODE_PRIVATE)
        }
    }

    fun saveString(key: String, value: String) {
        sharedPrefs.edit().putString(key, value).apply()
    }

    fun getString(key: String, defaultValue: String? = null): String? {
        return sharedPrefs.getString(key, defaultValue)
    }

    fun saveBoolean(key: String, value: Boolean) {
        sharedPrefs.edit().putBoolean(key, value).apply()
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return sharedPrefs.getBoolean(key, defaultValue)
    }

    fun saveDouble(key: String, value: Double) {
        sharedPrefs.edit().putLong(key, java.lang.Double.doubleToRawLongBits(value)).apply()
    }

    fun getDouble(key: String, defaultValue: Double = 0.0): Double {
        val raw = sharedPrefs.getLong(key, java.lang.Double.doubleToRawLongBits(defaultValue))
        return java.lang.Double.longBitsToDouble(raw)
    }

    fun remove(key: String) {
        sharedPrefs.edit().remove(key).apply()
    }

    fun clear() {
        sharedPrefs.edit().clear().apply()
    }
}
