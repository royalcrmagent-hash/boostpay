package com.example

import android.app.Application
import com.google.firebase.FirebaseApp

class WalletApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        FirebaseApp.initializeApp(this)
    }

    companion object {
        lateinit var instance: WalletApplication
            private set
    }
}
