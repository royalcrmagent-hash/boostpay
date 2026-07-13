package com.example.core.datastore

import android.content.Context
import android.content.SharedPreferences

class ThemePrefs(context: Context) {
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    fun isDarkMode(systemInDark: Boolean): Boolean {
        return sharedPrefs.getBoolean("is_dark_mode", systemInDark)
    }

    fun setDarkMode(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("is_dark_mode", enabled).apply()
    }
}
