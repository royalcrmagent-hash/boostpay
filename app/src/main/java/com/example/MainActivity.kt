package com.example

import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.example.feature.login.LoginScreen
import com.example.feature.home.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val systemInDark = isSystemInDarkTheme()
            val context = LocalContext.current
            val sharedPref = remember { context.getSharedPreferences("theme_prefs", android.content.Context.MODE_PRIVATE) }
            var isDarkMode by remember {
                mutableStateOf(sharedPref.getBoolean("is_dark_mode", systemInDark))
            }
            
            val updateManager = remember { com.example.core.update.InAppUpdateManager(context) }
            val updateState by updateManager.updateState.collectAsState()
            val scope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                // In production, you would point this to your actual server or a secure JSON file
                val updateApiUrl = "https://raw.githubusercontent.com/your-username/your-repo/main/version.json"
                updateManager.checkForUpdates(updateApiUrl)
            }

            MyApplicationTheme(darkTheme = isDarkMode) {
                WalletApp(
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = {
                        val newMode = !isDarkMode
                        isDarkMode = newMode
                        sharedPref.edit().putBoolean("is_dark_mode", newMode).apply()
                    }
                )
                
                com.example.presentation.components.InAppUpdateDialog(
                    updateState = updateState,
                    onDownload = { info -> 
                        scope.launch { updateManager.downloadAndInstallUpdate(info) }
                    },
                    onInstall = { file -> updateManager.installApk(file) },
                    onDismiss = { /* Ignored for mandatory */ }
                )
            }
        }
    }
}

@Composable
fun WalletApp(
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    viewModel: WalletViewModel = viewModel()
) {
    val context = LocalContext.current
    var user by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }
    
    LaunchedEffect(Unit) {
        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        viewModel.setDeviceId(deviceId)
        
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            user = auth.currentUser
        }
    }
    
    if (user == null) {
        LoginScreen(
            isDarkMode = isDarkMode,
            onToggleDarkMode = onToggleDarkMode
        ) { newUser, name, username, phone, email -> 
            viewModel.resetState()
            user = newUser
            name?.let { viewModel.updateUserProfile(newUser.uid, it, email ?: (newUser.email ?: ""), username ?: "", phone ?: "") }
        }
    } else {
        MainScreen(
            viewModel = viewModel,
            isDarkMode = isDarkMode,
            onToggleDarkMode = onToggleDarkMode,
            onLogout = { user = null }
        )
    }
}
