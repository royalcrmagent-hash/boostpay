package com.example

import android.os.Bundle
import android.provider.Settings
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.auth.FirebaseAuth
import coil.compose.AsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.layout.ContentScale

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.platform.testTag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.Canvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat

class PhoneNumberVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        // Mask: XXXXX XXXXXX
        val trimmed = if (text.text.length >= 11) text.text.substring(0, 11) else text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i == 4) out += " "
        }

        val phoneNumberOffsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 4) return offset
                if (offset <= 11) return offset + 1
                return 12
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 4) return offset
                if (offset <= 12) return offset - 1
                return 11
            }
        }

        return TransformedText(AnnotatedString(out), phoneNumberOffsetMapping)
    }
}

fun getUserStatus(rank: Int?, balance: Double): String {
    return when {
        rank == 1 -> "LEGEND"
        rank != null && rank <= 3 -> "MYTHIC"
        rank != null && rank <= 10 -> "ELITE"
        rank != null && rank <= 50 -> "PRO"
        balance >= 5000.0 -> "WHALE"
        balance >= 1000.0 -> "TYCOON"
        balance >= 500.0 -> "VIP"
        balance >= 100.0 -> "HUSTLER"
        else -> "ROOKIE"
    }
}

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
      
      MyApplicationTheme(darkTheme = isDarkMode) {
        WalletApp(
          isDarkMode = isDarkMode,
          onToggleDarkMode = {
            val newMode = !isDarkMode
            isDarkMode = newMode
            sharedPref.edit().putBoolean("is_dark_mode", newMode).apply()
          }
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

@Composable
fun LoginScreen(
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onLoginSuccess: (com.google.firebase.auth.FirebaseUser, String?, String?, String?, String?) -> Unit
) {
    val viewModel: WalletViewModel = viewModel()
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isSignUp by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    var isUsernameAvailable by remember { mutableStateOf<Boolean?>(null) }
    var isEmailAvailable by remember { mutableStateOf<Boolean?>(null) }
    var isPhoneAvailable by remember { mutableStateOf<Boolean?>(null) }
    
    val context = LocalContext.current

    // Availability Checks
    LaunchedEffect(username) {
        if (isSignUp && username.length >= 3) {
            delay(500)
            viewModel.checkUsernameAvailable(username) { available ->
                isUsernameAvailable = available
            }
        } else {
            isUsernameAvailable = null
        }
    }

    LaunchedEffect(email) {
        if (isSignUp && email.contains("@") && email.contains(".")) {
            delay(500)
            viewModel.checkEmailAvailable(email) { available ->
                isEmailAvailable = available
            }
        } else {
            isEmailAvailable = null
        }
    }

    LaunchedEffect(phoneNumber) {
        if (isSignUp && phoneNumber.length >= 10) {
            delay(500)
            viewModel.checkPhoneAvailable(phoneNumber) { available ->
                isPhoneAvailable = available
            }
        } else {
            isPhoneAvailable = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Theme toggle button at top end (with status bar padding support)
        IconButton(
            onClick = onToggleDarkMode,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
                .testTag("login_theme_toggle_button")
        ) {
            Icon(
                imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = "Toggle Theme",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
        Icon(
            Icons.Default.AccountBalanceWallet,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = if (isSignUp) "Create Account" else "Welcome Back",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (isSignUp) "Sign up to start earning" else "Login to your wallet",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.height(32.dp))

        if (isSignUp) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; error = null },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = username,
                onValueChange = { username = it; error = null },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                isError = isSignUp && username.isNotEmpty() && username.length < 3,
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                supportingText = {
                    if (isSignUp) {
                        if (username.isNotEmpty() && username.length < 3) {
                            Text("Username too short (min 3 chars)", color = MaterialTheme.colorScheme.error)
                        } else if (username.isNotEmpty()) {
                            Text("4 random digits will be added automatically", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it; error = null },
                label = { Text("Phone Number") },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (!focusState.isFocused && isSignUp && phoneNumber.length >= 10) {
                            viewModel.checkPhoneAvailable(phoneNumber) { available ->
                                isPhoneAvailable = available
                            }
                        }
                    },
                isError = isSignUp && ((phoneNumber.isNotEmpty() && phoneNumber.length < 10) || isPhoneAvailable == false),
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                visualTransformation = PhoneNumberVisualTransformation(),
                trailingIcon = {
                    if (isSignUp) {
                        if (isPhoneAvailable != null) {
                            Icon(
                                if (isPhoneAvailable!!) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (isPhoneAvailable!!) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                            )
                        } else if (phoneNumber.isNotEmpty() && phoneNumber.length < 10) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                supportingText = {
                    if (isSignUp) {
                        if (phoneNumber.isNotEmpty() && phoneNumber.length < 10) {
                            Text("Invalid phone number (min 10 digits)", color = MaterialTheme.colorScheme.error)
                        } else if (isPhoneAvailable != null) {
                            if (isPhoneAvailable == false) {
                                Text("already use these number", color = MaterialTheme.colorScheme.error)
                            } else {
                                Text("Phone number available", color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; error = null },
            label = { Text(if (isSignUp) "Email Address" else "Email, Username, or Phone") },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused && isSignUp && email.contains("@") && email.contains(".")) {
                        viewModel.checkEmailAvailable(email) { available ->
                            isEmailAvailable = available
                        }
                    }
                },
            isError = isSignUp && ((email.isNotEmpty() && (!email.contains("@") || !email.contains("."))) || isEmailAvailable == false),
            shape = RoundedCornerShape(16.dp),
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            trailingIcon = {
                if (isSignUp) {
                    if (isEmailAvailable != null) {
                        Icon(
                            if (isEmailAvailable!!) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (isEmailAvailable!!) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                        )
                    } else if (email.isNotEmpty() && (!email.contains("@") || !email.contains("."))) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    }
                }
            },
            supportingText = {
                if (isSignUp) {
                    if (email.isNotEmpty() && (!email.contains("@") || !email.contains("."))) {
                        Text("Invalid email format", color = MaterialTheme.colorScheme.error)
                    } else if (isEmailAvailable != null) {
                        if (isEmailAvailable == false) {
                            Text("already use these email", color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("Email available", color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; error = null },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                val description = if (passwordVisible) "Hide password" else "Show password"
                
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = description)
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        if (error != null) {
            Text(
                text = error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank() || (isSignUp && (name.isBlank() || username.isBlank() || phoneNumber.isBlank()))) {
                    error = "Please fill in all fields"
                    return@Button
                }
                
                if (isSignUp) {
                    if (isPhoneAvailable == false) {
                        error = "Phone number is already registered"
                        return@Button
                    }
                    if (isEmailAvailable == false) {
                        error = "Email address is already registered"
                        return@Button
                    }
                    if (isPhoneAvailable == null || isEmailAvailable == null) {
                        error = "Checking availability... please wait"
                        return@Button
                    }
                }

                isLoading = true
                val auth = FirebaseAuth.getInstance()
                
                if (isSignUp) {
                    isLoading = true
                    
                    // Generate a unique username by appending random digits
                    fun tryGenerateUniqueUsername(base: String, attempt: Int = 0) {
                        if (attempt > 5) {
                            isLoading = false
                            error = "Could not generate a unique username. Please try a different one."
                            return
                        }
                        
                        val randomSuffix = (1000..9999).random()
                        val finalUsername = "${base}${randomSuffix}".lowercase()
                        
                        viewModel.checkUsernameAvailable(finalUsername) { available ->
                            if (available) {
                                // Final username is unique, proceed with signup
                                val auth = FirebaseAuth.getInstance()
                                auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { result ->
                                    isLoading = false
                                    if (result.isSuccessful) {
                                        val firebaseUser = result.result?.user
                                        firebaseUser?.sendEmailVerification()
                                        onLoginSuccess(firebaseUser!!, name, finalUsername, phoneNumber, email)
                                    } else {
                                        error = result.exception?.localizedMessage ?: "Authentication failed"
                                    }
                                }
                            } else {
                                // Try again
                                tryGenerateUniqueUsername(base, attempt + 1)
                            }
                        }
                    }
                    
                    tryGenerateUniqueUsername(username)
                } else {
                    // Login: Resolve email from identifier (email, username, or phone)
                    viewModel.findUser(email) { uid, name, resolvedEmail ->
                        val emailToUse = resolvedEmail ?: email // Fallback to raw input if not found via query
                        auth.signInWithEmailAndPassword(emailToUse, password).addOnCompleteListener { result ->
                            isLoading = false
                            if (result.isSuccessful) {
                                val firebaseUser = result.result?.user
                                onLoginSuccess(firebaseUser!!, null, null, null, null)
                            } else {
                                error = result.exception?.localizedMessage ?: "Authentication failed"
                            }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(if (isSignUp) "Sign Up" else "Login")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { isSignUp = !isSignUp; error = null }) {
            Text(if (isSignUp) "Already have an account? Login" else "Don't have an account? Sign Up")
        }

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(
            onClick = {
                isLoading = true
                val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                val guestEmail = "guest_${deviceId}@wallet.app"
                val guestPassword = "guest_${deviceId}"
                val auth = FirebaseAuth.getInstance()
                
                auth.signInWithEmailAndPassword(guestEmail, guestPassword)
                    .addOnSuccessListener { result ->
                        isLoading = false
                        onLoginSuccess(result.user!!, null, null, null, null)
                    }
                    .addOnFailureListener {
                        // If login fails, try to sign up as guest
                        auth.createUserWithEmailAndPassword(guestEmail, guestPassword)
                            .addOnSuccessListener { result ->
                                isLoading = false
                                val shortId = deviceId.take(4).lowercase()
                                val guestName = "Guest ${shortId.uppercase()}"
                                val guestUsername = "guest_$shortId"
                                val guestPhone = "+8801${(100000000..999999999).random()}"
                                
                                onLoginSuccess(result.user!!, guestName, guestUsername, guestPhone, guestEmail)
                            }
                            .addOnFailureListener { ex ->
                                isLoading = false
                                error = "Guest login failed: ${ex.localizedMessage}"
                            }
                    }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = !isLoading
        ) {
            Icon(Icons.Default.Person, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Continue as Guest")
        }
    }
}
}

fun showBiometricPrompt(context: android.content.Context, onSuccess: () -> Unit, onError: () -> Unit) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: WalletViewModel,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val balance by viewModel.balance.collectAsState(initial = 0.0)
    val userName by viewModel.userName.collectAsState(initial = "Wallet User")
    val userUsername by viewModel.userUsername.collectAsState(initial = "")
    val userPhone by viewModel.userPhone.collectAsState(initial = "")
    val userEmail by viewModel.userEmail.collectAsState(initial = "")
    val transactions by viewModel.transactions.collectAsState(initial = emptyList())
    val currentUserUid by viewModel.currentUserUid.collectAsState(initial = "")
    
    val isBoosting by viewModel.isBoosting.collectAsState(initial = false)
    val boostTimeRemaining by viewModel.boostTimeRemaining.collectAsState(initial = 0)
    val currentMaxBoostDurationMs by viewModel.currentMaxBoostDurationMs.collectAsState(initial = 60000L)
    val boostEarnings by viewModel.boostEarnings.collectAsState(initial = null)
    val totalSystemBoost by viewModel.totalSystemBoost.collectAsState(initial = 0.0)
    val totalPlatformBalance by viewModel.totalPlatformBalance.collectAsState(initial = 0.0)
    val isBoostDisabled by viewModel.isBoostDisabled.collectAsState(initial = false)
    val leaderboard by viewModel.leaderboard.collectAsState(initial = emptyList())
    val selectedCurrency by viewModel.selectedCurrency.collectAsState(initial = CurrencyData("USD", "$"))
    val availableCurrencies by viewModel.availableCurrencies.collectAsState(initial = listOf(CurrencyData("USD", "$")))
    val exchangeRates by viewModel.exchangeRates.collectAsState(initial = emptyMap())
    val globalBoostRatePercent by viewModel.globalBoostRatePercent.collectAsState(initial = 100.0)

    val liveAnnouncement by viewModel.liveAnnouncement.collectAsState(initial = "")

    val isAdmin by viewModel.isAdmin.collectAsState(initial = false)
    val platformVolume by viewModel.platformVolume.collectAsState(initial = 0.0)
    val allUsers by viewModel.allUsers.collectAsState(initial = emptyList())
    
    val userRank by viewModel.userRank.collectAsState(initial = null)
    val isEmailVerified by viewModel.isEmailVerified.collectAsState(initial = true)
    
    val convertedBalance = if (selectedCurrency.code == "USD") {
        balance
    } else {
        balance * (exchangeRates[selectedCurrency.code] ?: 1.0)
    }
    
    var showSendDialog by remember { mutableStateOf(false) }
    var showReceiveDialog by remember { mutableStateOf(false) }
    var showAdminDialog by remember { mutableStateOf(false) }
    var showLeaderboardDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    var selectedTxTab by remember { mutableStateOf("All") }
    var txSearchQuery by remember { mutableStateOf("") }
    var selectedTxTypeFilter by remember { mutableStateOf("All Types") }
    var txStartDate by remember { mutableStateOf<Long?>(null) }
    var txEndDate by remember { mutableStateOf<Long?>(null) }

    val displayTransactions = remember(transactions, selectedTxTab, txSearchQuery, selectedTxTypeFilter, txStartDate, txEndDate, currentUserUid) {
        transactions.filter { tx ->
            val matchesTab = when (selectedTxTab) {
                "Boost" -> tx.sender == "BOOST_SYSTEM"
                "Sent" -> tx.sender == currentUserUid
                "Received" -> tx.receiver == currentUserUid
                else -> true
            }
            if (!matchesTab) return@filter false

            val matchesTypeDropdown = when (selectedTxTypeFilter) {
                "Boost" -> tx.sender == "BOOST_SYSTEM"
                "Admin Adjustment" -> tx.sender == "ADMIN_SYSTEM"
                "Transfer" -> tx.sender != "BOOST_SYSTEM" && tx.sender != "ADMIN_SYSTEM"
                else -> true
            }
            if (!matchesTypeDropdown) return@filter false

            val matchesDateRange = if (txStartDate != null || txEndDate != null) {
                val txTime = tx.timestamp?.toDate()?.time ?: 0L
                val matchesStart = txStartDate?.let { txTime >= it } ?: true
                val matchesEnd = txEndDate?.let { txTime <= (it + 86400000L - 1L) } ?: true
                matchesStart && matchesEnd
            } else {
                true
            }
            if (!matchesDateRange) return@filter false

            if (txSearchQuery.isNotEmpty()) {
                val query = txSearchQuery.trim()
                val senderMatches = tx.sender.contains(query, ignoreCase = true) || tx.senderName.contains(query, ignoreCase = true)
                val receiverMatches = tx.receiver.contains(query, ignoreCase = true) || tx.receiverName.contains(query, ignoreCase = true)
                val amountMatches = tx.amount.toString().contains(query)
                val typeMatches = when {
                    tx.sender == "BOOST_SYSTEM" -> "boost".contains(query, ignoreCase = true)
                    tx.sender == "ADMIN_SYSTEM" -> "admin adjustment".contains(query, ignoreCase = true)
                    tx.sender == currentUserUid -> "sent".contains(query, ignoreCase = true)
                    else -> "received".contains(query, ignoreCase = true)
                }
                senderMatches || receiverMatches || amountMatches || typeMatches
            } else {
                true
            }
        }
    }

    var confettiTrigger by remember { mutableStateOf(0) }
    var currentScreen by remember { mutableStateOf("Home") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.showConfetti.collect {
            confettiTrigger++
        }
    }

    LaunchedEffect(Unit) {
        viewModel.notifications.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(Unit) {
        while(true) {
            viewModel.checkEmailVerificationStatus()
            delay(30000)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { 
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(bottom = 80.dp)
                ) 
            },
            topBar = {
            TopAppBar(
                title = { Text("BoostPay", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {
                        viewModel.refreshUserData()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.primary)
                    }
                    if (isAdmin) {
                        TextButton(onClick = { showAdminDialog = true }) {
                            Text("ADMIN", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    IconButton(
                        onClick = onToggleDarkMode,
                        modifier = Modifier.testTag("appbar_theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { 
                        viewModel.resetState()
                        FirebaseAuth.getInstance().signOut()
                        onLogout()
                    }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            if (currentScreen == "Home") {
                if (!isEmailVerified && !userEmail.startsWith("guest_")) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Email Not Verified",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        "Please verify your email to secure your account.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                    )
                                }
                                Row {
                                    IconButton(onClick = {
                                        viewModel.sendVerificationEmail { success, errorMsg ->
                                            scope.launch {
                                                if (success) {
                                                    snackbarHostState.showSnackbar("Verification email sent!")
                                                } else {
                                                    snackbarHostState.showSnackbar(errorMsg ?: "Failed to send email")
                                                }
                                            }
                                        }
                                    }) {
                                        Icon(Icons.Default.Send, contentDescription = "Resend", tint = MaterialTheme.colorScheme.error)
                                    }
                                    IconButton(onClick = { viewModel.checkEmailVerificationStatus() }) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Status", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }

                if (liveAnnouncement.isNotEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        tonalElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = "Live Announcement",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = liveAnnouncement,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 2. Redesigned Balance Card with Dedicated Sleek Physical Look
            item {
                val userAvatarUrl by viewModel.userAvatarUrl.collectAsState()
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.35f),
                                Color.White.copy(alpha = 0.05f),
                                Color.White.copy(alpha = 0.2f)
                            )
                        )
                    ),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF5B4CFF), // BoostPrimary
                                        Color(0xFF2A1F9D), // Darker blue/purple
                                        Color(0xFF4536D6)  // Mid shade
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(1000f, 1000f)
                                )
                            )
                    ) {
                        // Glossy physical card visual design layers
                        Canvas(modifier = Modifier.matchParentSize()) {
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.04f),
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.02f)
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(size.width, size.height)
                                )
                            )
                            drawCircle(
                                color = Color(0xFF00C2A8).copy(alpha = 0.3f), // BoostSecondary glow
                                radius = size.minDimension * 0.45f,
                                center = Offset(size.width * 0.85f, size.height * 0.15f)
                            )
                            drawCircle(
                                color = Color(0xFFF5B700).copy(alpha = 0.2f), // BoostAccent halo
                                radius = size.minDimension * 0.65f,
                                center = Offset(size.width * 0.15f, size.height * 0.85f)
                            )
                        }

                        Column(
                            modifier = Modifier.padding(24.dp)
                        ) {
                            // Top Row: Premium Brand & Currency Dropdown
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stars,
                                        contentDescription = "BoostPay Star",
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = getUserStatus(userRank, balance),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            letterSpacing = 2.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = Color.White.copy(alpha = 0.95f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Chip and Contactless Rows
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Metallic smart gold chip representation
                                Surface(
                                    modifier = Modifier.size(width = 44.dp, height = 30.dp),
                                    color = MaterialTheme.colorScheme.tertiary,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val strokeWidth = 1.2.dp.toPx()
                                        val color = Color.Black.copy(alpha = 0.35f)
                                        drawLine(color, Offset(size.width * 0.33f, 0f), Offset(size.width * 0.33f, size.height), strokeWidth)
                                        drawLine(color, Offset(size.width * 0.66f, 0f), Offset(size.width * 0.66f, size.height), strokeWidth)
                                        drawLine(color, Offset(0f, size.height * 0.5f), Offset(size.width, size.height * 0.5f), strokeWidth)
                                        drawRoundRect(
                                            color = Color.Black.copy(alpha = 0.12f),
                                            topLeft = Offset(size.width * 0.33f, size.height * 0.3f),
                                            size = androidx.compose.ui.geometry.Size(size.width * 0.33f, size.height * 0.4f),
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.Default.Wifi,
                                    contentDescription = "Contactless payment",
                                    tint = Color.White.copy(alpha = 0.65f),
                                    modifier = Modifier
                                        .size(20.dp)
                                        .graphicsLayer { rotationZ = 90f }
                                )
                            }

                            // Card Number
                            val lastFour = currentUserUid.takeLast(4).uppercase(Locale.ROOT).ifEmpty { "8824" }
                            val cardNumber = "5412  8734  9012  $lastFour"
                            Text(
                                text = cardNumber,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    letterSpacing = 2.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.padding(vertical = 12.dp)
                            )

                            // Balance Header text
                            Text(
                                text = "AVAILABLE BALANCE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 1.5.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f)
                            )

                            // Main Balance Amount (Formatted with Integer & Decimals split)
                            val formattedBalanceStr = "%.6f".format(convertedBalance)
                            val dotIdx = formattedBalanceStr.indexOf('.')
                            val integerPart = if (dotIdx != -1) formattedBalanceStr.substring(0, dotIdx) else formattedBalanceStr
                            val decimalPart = if (dotIdx != -1) formattedBalanceStr.substring(dotIdx) else ""

                            Row(
                                verticalAlignment = Alignment.Bottom,
                                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                            ) {
                                Text(
                                    text = selectedCurrency.symbol,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    modifier = Modifier.padding(bottom = 5.dp, end = 2.dp)
                                )
                                Text(
                                    text = integerPart,
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = (-1).sp
                                    ),
                                    fontSize = 36.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = decimalPart,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Normal,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    ),
                                    fontSize = 18.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(bottom = 5.dp)
                                )
                            }

                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.12f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            // Cardholder name, Expiry Date
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "CARD HOLDER",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 8.sp,
                                            letterSpacing = 1.2.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color.White.copy(alpha = 0.45f)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    SelectionContainer {
                                        Text(
                                            text = userName.uppercase(Locale.ROOT),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "VALID THRU",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 8.sp,
                                            letterSpacing = 1.2.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color.White.copy(alpha = 0.45f)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "12/29",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        ),
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Bottom UID Pill with Copy Option
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (userUsername.isNotEmpty()) "@$userUsername" else userEmail,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.5f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                                val context = LocalContext.current

                                Surface(
                                    onClick = {
                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(currentUserUid))
                                        android.widget.Toast.makeText(context, "UID copied!", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    color = Color.White.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(100.dp),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy UID",
                                            tint = Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Text(
                                            text = "UID: ${currentUserUid.take(6)}...${currentUserUid.takeLast(4)}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = Color.White.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                            }
                        }

                        // Currency Selector floating beautiful element on the Card
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                        ) {
                            var expanded by remember { mutableStateOf(false) }
                            var searchQuery by remember { mutableStateOf("") }
                            
                            val filteredCurrencies = remember(searchQuery, availableCurrencies) {
                                if (searchQuery.isEmpty()) {
                                    availableCurrencies
                                } else {
                                    availableCurrencies.filter { 
                                        it.code.contains(searchQuery, ignoreCase = true) || 
                                        it.symbol.contains(searchQuery, ignoreCase = true) ||
                                        it.name.contains(searchQuery, ignoreCase = true)
                                    }
                                }
                            }

                            Surface(
                                onClick = { 
                                    expanded = true 
                                    searchQuery = "" // Reset search when opening
                                },
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (selectedCurrency.flag.isNotEmpty()) {
                                        Text(selectedCurrency.flag, modifier = Modifier.padding(end = 4.dp))
                                    }
                                    Text(
                                        selectedCurrency.code,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier
                                    .width(280.dp) // Increased width for names
                                    .heightIn(max = 400.dp)
                            ) {
                                // Search Field
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    placeholder = { Text("Search code or country...", style = MaterialTheme.typography.bodySmall) },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodySmall,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                
                                HorizontalDivider()

                                if (filteredCurrencies.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No results", style = MaterialTheme.typography.bodySmall) },
                                        onClick = { },
                                        enabled = false
                                    )
                                }

                                filteredCurrencies.forEach { currency ->
                                    val rate = exchangeRates[currency.code] ?: 1.0
                                    DropdownMenuItem(
                                        text = { 
                                            Column(modifier = Modifier.fillMaxWidth()) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        if (currency.flag.isNotEmpty()) {
                                                            Text(currency.flag, modifier = Modifier.padding(end = 8.dp), style = MaterialTheme.typography.titleMedium)
                                                        }
                                                        Text(currency.code, fontWeight = FontWeight.Bold)
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text("(${currency.symbol})", color = MaterialTheme.colorScheme.secondary)
                                                    }
                                                    Text(
                                                        "1 USD = ${"%.2f".format(rate)}", 
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.outline
                                                    )
                                                }
                                                if (currency.name.isNotEmpty()) {
                                                    Text(
                                                        currency.name,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            viewModel.setCurrency(currency)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Smart side-by-side high-fidelity primary actions
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Send Money Premium Action Button
                    Button(
                        onClick = { showSendDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Send Money",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Boost Balance Premium Action Button
                    BoostBalanceButton(
                        isBoosting = isBoosting,
                        isDisabled = isBoostDisabled,
                        timeRemaining = boostTimeRemaining,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        onClick = {
                            showBiometricPrompt(
                                context = context,
                                onSuccess = { viewModel.increaseBalance() },
                                onError = { /* Do nothing or show a toast */ }
                            )
                        },
                        onStopClick = { viewModel.stopBoost() }
                    )
                }
            }

            // High-Fidelity Active Boost Progress Bar Card
            item {
                AnimatedVisibility(
                    visible = boostTimeRemaining > 0,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))
                        ActiveBoostProgressCard(
                            isBoosting = isBoosting,
                            timeRemainingSeconds = boostTimeRemaining,
                            maxDurationMs = currentMaxBoostDurationMs,
                            currencySymbol = selectedCurrency.symbol,
                            boostRatePercent = globalBoostRatePercent
                        )
                    }
                }
            }

            // Beautiful Dynamic Boost Limit Info Card
            item {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.05f)))),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val isRank1 = leaderboard.firstOrNull()?.uid == currentUserUid
                                Icon(
                                    imageVector = if (isRank1) Icons.Default.EmojiEvents else Icons.Default.Bolt,
                                    tint = if (isRank1) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.tertiary,
                                    contentDescription = "Boost limit info",
                                    modifier = Modifier.size(24.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Dynamic Boosting Limit",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val formattedLimit = if (isRank1) {
                                        "24 hours (Leaderboard Rank #1 Privilege)"
                                    } else {
                                        val hrs = currentMaxBoostDurationMs / 3600000L
                                        val mins = (currentMaxBoostDurationMs % 3600000L) / 60000L
                                        val secs = (currentMaxBoostDurationMs % 60000L) / 1000L
                                        if (hrs > 0) {
                                            "${hrs}h ${mins}m limit"
                                        } else if (mins > 0) {
                                            "${mins}m limit"
                                        } else {
                                            "${secs}s limit"
                                        }
                                    }
                                    Text(
                                        text = "Current limit: $formattedLimit",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            val isRank1 = leaderboard.firstOrNull()?.uid == currentUserUid
                            val percentage = if (isRank1) 1.0f else {
                                (currentMaxBoostDurationMs.toFloat() / (24 * 60 * 60 * 1000L).toFloat()).coerceIn(0.001f, 1.0f)
                            }

                            // Progress bar representing potential
                            LinearProgressIndicator(
                                progress = { percentage },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (isRank1) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.tertiary,
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = if (isRank1) {
                                    "👑 You are Rank #1 on the leaderboard! Enjoying 24h of premium booster power!"
                                } else {
                                    "⏱️ Dynamic formula: lower balance runs for 1 minute, higher balance runs up to 24 hours. Rank #1 gets 24h boost!"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(28.dp))

                // Transaction History Header
                Text(
                    "Transactions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Modern Search Field with Custom Styling and test tag
                OutlinedTextField(
                    value = txSearchQuery,
                    onValueChange = { txSearchQuery = it },
                    placeholder = { Text("Search User ID, name, status, or amount...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon") },
                    trailingIcon = {
                        if (txSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { txSearchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_search_input"),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Type filter dropdown and Advanced Date filter toggle
                var typeDropdownExpanded by remember { mutableStateOf(false) }
                var showDateRangeFilters by remember { mutableStateOf(false) }
                val dateFormatter = remember { java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clickable { typeDropdownExpanded = true },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = selectedTxTypeFilter,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = typeDropdownExpanded,
                            onDismissRequest = { typeDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.6f)
                        ) {
                            listOf("All Types", "Transfer", "Boost", "Admin Adjustment").forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        selectedTxTypeFilter = type
                                        typeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedIconToggleButton(
                        checked = showDateRangeFilters,
                        onCheckedChange = { showDateRangeFilters = it },
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (showDateRangeFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Date range",
                                tint = if (showDateRangeFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Dates",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (showDateRangeFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                AnimatedVisibility(
                    visible = showDateRangeFilters,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Filter by Date Range", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Start Date Button
                            OutlinedButton(
                                onClick = {
                                    val cal = Calendar.getInstance()
                                    txStartDate?.let { cal.timeInMillis = it }
                                    android.app.DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            val selectedCal = Calendar.getInstance()
                                            selectedCal.set(year, month, dayOfMonth, 0, 0, 0)
                                            txStartDate = selectedCal.timeInMillis
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = txStartDate?.let { dateFormatter.format(java.util.Date(it)) } ?: "Start Date",
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // End Date Button
                            OutlinedButton(
                                onClick = {
                                    val cal = Calendar.getInstance()
                                    txEndDate?.let { cal.timeInMillis = it }
                                    android.app.DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            val selectedCal = Calendar.getInstance()
                                            selectedCal.set(year, month, dayOfMonth, 23, 59, 59)
                                            txEndDate = selectedCal.timeInMillis
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = txEndDate?.let { dateFormatter.format(java.util.Date(it)) } ?: "End Date",
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (txStartDate != null || txEndDate != null) {
                            TextButton(
                                onClick = {
                                    txStartDate = null
                                    txEndDate = null
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(Icons.Default.ClearAll, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear Dates", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Horizontal scrollable Filter Chips row for rich filter types
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedTxTab == "All",
                        onClick = { selectedTxTab = "All" },
                        label = { Text("All") },
                        leadingIcon = if (selectedTxTab == "All") {
                            { Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.testTag("tx_tab_all")
                    )
                    FilterChip(
                        selected = selectedTxTab == "Boost",
                        onClick = { selectedTxTab = "Boost" },
                        label = { Text("🚀 Boost Sessions") },
                        leadingIcon = if (selectedTxTab == "Boost") {
                            { Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.testTag("tx_tab_boost")
                    )
                    FilterChip(
                        selected = selectedTxTab == "Sent",
                        onClick = { selectedTxTab = "Sent" },
                        label = { Text("📤 Sent") },
                        leadingIcon = if (selectedTxTab == "Sent") {
                            { Icon(Icons.Default.ArrowOutward, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.testTag("tx_tab_sent")
                    )
                    FilterChip(
                        selected = selectedTxTab == "Received",
                        onClick = { selectedTxTab = "Received" },
                        label = { Text("📥 Received") },
                        leadingIcon = if (selectedTxTab == "Received") {
                            { Icon(Icons.Default.CallReceived, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.testTag("tx_tab_received")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            if (displayTransactions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = when (selectedTxTab) {
                                            "Boost" -> Icons.Default.Bolt
                                            "Sent" -> Icons.Default.ArrowOutward
                                            "Received" -> Icons.Default.CallReceived
                                            else -> Icons.Default.ReceiptLong
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp),
                                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = when (selectedTxTab) {
                                    "Boost" -> "No Boost Sessions"
                                    "Sent" -> "No Sent Transactions"
                                    "Received" -> "No Received Transactions"
                                    else -> if (txSearchQuery.isNotEmpty()) "No Matching Transactions" else "No Transactions Yet"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (txSearchQuery.isNotEmpty()) {
                                    "Try clearing or refining your search query."
                                } else {
                                    when (selectedTxTab) {
                                        "Boost" -> "Activate your high-performance boost to start generating earnings!"
                                        else -> "Send money to friends or click Boost to get started!"
                                    }
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(displayTransactions) { transaction ->
                    if (transaction.sender == "BOOST_SYSTEM") {
                        BoostSessionItem(transaction, selectedCurrency, exchangeRates)
                    } else {
                        TransactionItem(transaction, currentUserUid, selectedCurrency, exchangeRates)
                    }
                }
            }
        } else if (currentScreen == "Settings") {
            item {
                Text(
                    text = "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
                
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Account & Profile",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Manage your personal information and account details.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showEditProfileDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Edit Profile")
                            }
                        }
                    }
                }
            }
        }
    }

    FloatingCurvedNavBar(
            currentScreen = currentScreen,
            onScreenSelected = { screen ->
                if (screen == "Leaderboard") {
                    showLeaderboardDialog = true
                } else if (screen == "Profile") {
                    showEditProfileDialog = true
                } else {
                    currentScreen = screen
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )

        if (confettiTrigger > 0) {
            ConfettiEffect(key = confettiTrigger) {
                confettiTrigger = 0
            }
        }
    }

    if (showSendDialog) {
        SendMoneyDialog(
            viewModel = viewModel,
            currency = selectedCurrency,
            currentBalanceUsd = balance,
            exchangeRates = exchangeRates,
            onDismiss = { showSendDialog = false },
            onSend = { uid, amount, onSuccess, onError ->
                val usdAmount = if (selectedCurrency.code == "USD") {
                    amount
                } else {
                    val rate = exchangeRates[selectedCurrency.code] ?: 1.0
                    if (rate > 0) amount / rate else amount
                }
                viewModel.sendMoney(uid, usdAmount, 
                    onSuccess = onSuccess,
                    onError = onError
                )
            }
        )
    }

    if (showReceiveDialog) {
        ReceiveMoneyDialog(currentUserUid) { showReceiveDialog = false }
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            viewModel = viewModel,
            currentName = userName,
            currentUsername = userUsername,
            currentPhone = userPhone,
            currentEmail = userEmail,
            onDismiss = { showEditProfileDialog = false }
        )
    }
    
    if (showLeaderboardDialog) {
        LeaderboardDialog(leaderboard, currentUserUid, balance, userRank) { showLeaderboardDialog = false }
    }

    if (showAdminDialog && isAdmin) {
        LaunchedEffect(Unit) {
            viewModel.fetchAdminData()
        }
        AdminDashboardScreen(
            viewModel = viewModel,
            onBack = { showAdminDialog = false }
        )
    }

    boostEarnings?.let { earnings ->
        BoostSummaryDialog(
            amount = earnings,
            currency = selectedCurrency,
            rates = exchangeRates,
            onDismiss = { viewModel.clearBoostEarnings() }
        )
    }
}

@Composable
fun ConfettiEffect(key: Int, onFinished: () -> Unit) {
    val particles = remember(key) {
        List(100) {
            ConfettiParticle(
                x = Random.nextFloat(),
                y = -0.1f,
                color = Color(
                    Random.nextInt(256),
                    Random.nextInt(256),
                    Random.nextInt(256)
                ),
                speed = Random.nextFloat() * 1.2f + 0.8f,
                angle = Random.nextFloat() * 360f,
                rotationSpeed = Random.nextFloat() * 10f - 5f
            )
        }
    }

    val progress = remember { Animatable(0f) }

    LaunchedEffect(key) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 3000, easing = LinearOutSlowInEasing)
        )
        onFinished()
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        particles.forEach { particle ->
            val p = progress.value
            val currentY = (particle.y + p * particle.speed) * height
            val currentX = (particle.x + Math.sin(p * 5.0).toFloat() * 0.05f) * width
            
            rotate(particle.angle + p * 360 * particle.rotationSpeed, pivot = Offset(currentX, currentY)) {
                drawRect(
                    color = particle.color,
                    topLeft = Offset(currentX, currentY),
                    size = androidx.compose.ui.geometry.Size(20f, 10f)
                )
            }
        }
    }
}

data class ConfettiParticle(
    val x: Float,
    val y: Float,
    val color: Color,
    val speed: Float,
    val angle: Float,
    val rotationSpeed: Float
)

@Composable
fun ActionButton(icon: ImageVector, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(8.dp)
            .alpha(if (enabled) 1f else 0.5f)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun ActiveBoostProgressCard(
    isBoosting: Boolean,
    timeRemainingSeconds: Int,
    maxDurationMs: Long,
    currencySymbol: String,
    boostRatePercent: Double,
    modifier: Modifier = Modifier
) {
    // calculate progress fraction
    val maxDurationSeconds = (maxDurationMs / 1000L).coerceAtLeast(1L).toFloat()
    val progressFraction = (timeRemainingSeconds.toFloat() / maxDurationSeconds).coerceIn(0f, 1f)
    
    // Formatting remaining time
    val hrs = timeRemainingSeconds / 3600
    val mins = (timeRemainingSeconds % 3600) / 60
    val secs = timeRemainingSeconds % 60
    val formattedTime = if (hrs > 0) {
        String.format("%02d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format("%02d:%02d", mins, secs)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.05f)))),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "boost_pulse")
                    val pulseAlpha by if (isBoosting) {
                        infiniteTransition.animateFloat(
                            initialValue = 0.4f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulse_alpha"
                        )
                    } else {
                        remember { mutableStateOf(1f) }
                    }
                    
                    Icon(
                        imageVector = if (isBoosting) Icons.Default.Bolt else Icons.Default.Pause,
                        tint = if (isBoosting) MaterialTheme.colorScheme.tertiary else Color.Gray,
                        contentDescription = "Session status",
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer { alpha = pulseAlpha }
                    )
                    
                    Text(
                        text = if (isBoosting) "Active Boost Session" else "Boost Session Paused",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = if (isBoosting) MaterialTheme.colorScheme.tertiary else Color.Gray,
                    modifier = Modifier.testTag("boost_time_remaining_text")
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Progress Bar with pulse animation if active
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .testTag("boost_progress_bar"),
                color = if (isBoosting) MaterialTheme.colorScheme.tertiary else Color.Gray,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isBoosting) "Earning with +${String.format("%.1f", boostRatePercent)}% boost" else "Resume to continue boosting",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${(progressFraction * 100).toInt()}% left",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun BoostBalanceButton(
    isBoosting: Boolean, 
    isDisabled: Boolean, 
    timeRemaining: Int, 
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onStopClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Button(
        onClick = { if (isBoosting) onStopClick() else onClick() },
        enabled = !isDisabled,
        modifier = modifier
            .graphicsLayer {
                if (isBoosting) {
                    scaleX = scale
                    scaleY = scale
                    alpha = pulseAlpha
                }
            },
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = when {
                isBoosting -> MaterialTheme.colorScheme.secondaryContainer
                isDisabled -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.tertiary
            },
            contentColor = when {
                isBoosting -> MaterialTheme.colorScheme.onSecondaryContainer
                isDisabled -> MaterialTheme.colorScheme.onErrorContainer
                else -> MaterialTheme.colorScheme.onTertiary
            }
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                when {
                    isBoosting -> Icons.Default.StopCircle
                    isDisabled -> Icons.Default.Engineering
                    else -> Icons.Default.Bolt
                },
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = when {
                    isBoosting -> {
                        val hrs = timeRemaining / 3600
                        val mins = (timeRemaining % 3600) / 60
                        val secs = timeRemaining % 60
                        val formattedTime = if (hrs > 0) {
                            String.format("%02d:%02d:%02d", hrs, mins, secs)
                        } else {
                            String.format("%02d:%02d", mins, secs)
                        }
                        "Stop ($formattedTime)"
                    }
                    isDisabled -> "Maintenance"
                    else -> "Boost"
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun TransactionItem(
    transaction: Transaction, 
    currentUserUid: String,
    currency: CurrencyData,
    rates: Map<String, Double>
) {
    val isBoost = transaction.sender == "BOOST_SYSTEM"
    val isAdminAdjustment = transaction.sender == "ADMIN_SYSTEM"
    val isSent = transaction.sender == currentUserUid
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val timestamp = transaction.timestamp?.toDate() ?: Date()
    val date = sdf.format(timestamp)

    val convertedAmount = if (currency.code == "USD") {
        transaction.amount
    } else {
        transaction.amount * (rates[currency.code] ?: 1.0)
    }

    val icon = when {
        isBoost -> Icons.Default.Bolt
        isAdminAdjustment -> Icons.Default.Tune
        isSent -> Icons.Default.ArrowOutward
        else -> Icons.Default.CallReceived
    }

    val iconColor = when {
        isBoost -> MaterialTheme.colorScheme.tertiary
        isAdminAdjustment -> MaterialTheme.colorScheme.primary
        isSent -> Color.Red
        else -> MaterialTheme.colorScheme.secondary
    }

    val backgroundColor = when {
        isBoost -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        isAdminAdjustment -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        isSent -> Color(0xFFFFEBEE)
        else -> Color(0xFFE8F5E9)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = backgroundColor,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = iconColor
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        isBoost -> "Boost Earning${if (transaction.durationSeconds != null) " (${transaction.durationSeconds}s)" else ""}"
                        isAdminAdjustment -> "Admin Adjustment"
                        isSent -> "To: ${if (transaction.receiverName.isNotEmpty() && transaction.receiverName != "Unknown") transaction.receiverName else transaction.receiver.take(8) + "..."}"
                        else -> "From: ${if (transaction.senderName.isNotEmpty() && transaction.senderName != "Unknown") transaction.senderName else transaction.sender.take(8) + "..."}"
                    },
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
            Column(horizontalAlignment = Alignment.End) {
                val absAmount = java.lang.Math.abs(convertedAmount)
                val signStr = when {
                    isSent -> "-"
                    isAdminAdjustment -> if (convertedAmount < 0.0) "-" else "+"
                    else -> "+"
                }
                val amountColor = when {
                    isSent -> Color.Red
                    isAdminAdjustment -> if (convertedAmount < 0.0) Color.Red else MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.secondary
                }
                Text(
                    text = "$signStr${currency.symbol}${"%.6f".format(absAmount)}",
                    fontWeight = FontWeight.ExtraBold,
                    color = amountColor
                )
                Surface(
                    color = when (transaction.status.lowercase()) {
                        "completed" -> Color(0xFFE8F5E9)
                        "processing" -> Color(0xFFFFF3E0)
                        "failed" -> Color(0xFFFFEBEE)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = transaction.status.replaceFirstChar { it.uppercase() },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = when (transaction.status.lowercase()) {
                            "completed" -> MaterialTheme.colorScheme.secondary
                            "processing" -> Color(0xFFEF6C00)
                            "failed" -> Color.Red
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun BoostSessionItem(
    transaction: Transaction,
    currency: CurrencyData,
    rates: Map<String, Double>
) {
    val sdf = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
    val timestamp = transaction.timestamp?.toDate() ?: Date()
    val fullDate = sdf.format(timestamp)

    val convertedAmount = if (currency.code == "USD") {
        transaction.amount
    } else {
        transaction.amount * (rates[currency.code] ?: 1.0)
    }

    val secs = transaction.durationSeconds ?: 0
    val durationText = when {
        secs >= 3600 -> {
            val hrs = secs / 3600
            val mins = (secs % 3600) / 60
            val remainingSecs = secs % 60
            "${hrs}h ${mins}m ${remainingSecs}s"
        }
        secs >= 60 -> {
            val mins = secs / 60
            val remainingSecs = secs % 60
            "${mins}m ${remainingSecs}s"
        }
        else -> "${secs}s"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .testTag("boost_session_item_${transaction.timestamp?.seconds ?: 0}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Neon glowing cyber circle
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Boost Session",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "🚀 Compounding Boost Session",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Duration: $durationText",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Text(
                        text = fullDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "+${currency.symbol}${"%.6f".format(convertedAmount)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.secondary // Shiny deep green
                )

                Surface(
                    color = Color(0xFFE8F5E9), // Light material green
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary)
                        )
                        Text(
                            text = "COMPLETED",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendMoneyDialog(
    viewModel: WalletViewModel,
    currency: CurrencyData,
    currentBalanceUsd: Double,
    exchangeRates: Map<String, Double>,
    onDismiss: () -> Unit, 
    onSend: (String, Double, () -> Unit, (String) -> Unit) -> Unit
) {
    var recipientInput by remember { mutableStateOf("") }
    var recipientName by remember { mutableStateOf<String?>(null) }
    var resolvedUid by remember { mutableStateOf<String?>(null) }
    var amount by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isSending by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }

    val rate = if (currency.code == "USD") 1.0 else (exchangeRates[currency.code] ?: 1.0)
    val convertedBalance = currentBalanceUsd * rate

    // Auto-resolve recipient when input changes with debounce
    LaunchedEffect(recipientInput) {
        if (recipientInput.length >= 3) {
            isSearching = true
            delay(500) // 500ms debounce
            viewModel.findUser(recipientInput.trim()) { uid, name, email ->
                resolvedUid = uid
                recipientName = name
                isSearching = false
                if (uid == null) {
                    error = "User not found"
                } else if (uid == viewModel.currentUserUid.value) {
                    error = "Cannot send to yourself"
                    resolvedUid = null
                    recipientName = null
                } else {
                    error = null
                }
            }
        } else {
            resolvedUid = null
            recipientName = null
            error = null
            isSearching = false
        }
    }

    AlertDialog(
        onDismissRequest = if (!isSending && !isSuccess) onDismiss else ({}),
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Send Money")
            }
        },
        text = {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer(alpha = if (isSuccess) 0f else 1f)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Your Balance", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                                Text(
                                    "${currency.symbol}${String.format("%.2f", convertedBalance)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            TextButton(
                                onClick = { amount = String.format("%.2f", convertedBalance) },
                                enabled = convertedBalance > 0 && !isSending && !isSuccess
                            ) {
                                Text("MAX")
                            }
                        }
                    }

                    OutlinedTextField(
                        value = recipientInput,
                        onValueChange = { recipientInput = it },
                        label = { Text("Email, Username or Phone") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { 
                            if (isSearching) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Person, contentDescription = null)
                            }
                        },
                        enabled = !isSending && !isSuccess,
                        singleLine = true,
                        placeholder = { Text("e.g. @username, 017x, or email") }
                    )
                    
                    if (recipientName != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle, 
                                    contentDescription = null, 
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Recipient Found",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = recipientName!!,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { 
                            if (it.isEmpty() || it.toDoubleOrNull() != null) {
                                amount = it 
                            }
                        },
                        label = { Text("Amount (${currency.code})") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Text(currency.symbol, modifier = Modifier.padding(start = 12.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = !isSending && !isSuccess,
                        singleLine = true
                    )
                    
                    if (error != null) {
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                        )
                    }
                }

                if (isSuccess) {
                    SuccessCelebration()
                }
            }
        },
        confirmButton = {
            if (!isSuccess) {
                Button(
                    onClick = { 
                        val amt = amount.toDoubleOrNull()
                        val target = resolvedUid
                        if (amt == null || amt <= 0) {
                            error = "Please enter a valid amount"
                        } else if (amt > convertedBalance + 0.001) { 
                            error = "Insufficient balance"
                        } else if (target == null || target.isBlank()) {
                            error = "Recipient not found"
                        } else {
                            error = null
                            isSending = true
                            onSend(target, amt, {
                                isSending = false
                                isSuccess = true
                            }, {
                                isSending = false
                                error = it
                            })
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSending && !isSearching && resolvedUid != null
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sending...")
                    } else {
                        Text("Send")
                    }
                }
            } else {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Done")
                }
            }
        },
        dismissButton = {
            if (!isSending && !isSuccess) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
fun SuccessCelebration() {
    val infiniteTransition = rememberInfiniteTransition(label = "success_celebration")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color(0xFF00C2A8).copy(alpha = 0.2f),
                    radius = size.minDimension / 2
                )
            }
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Transfer Successful!",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center
        )
        Text(
            "Your money has been sent.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun BoostSummaryDialog(
    amount: Double, 
    currency: CurrencyData,
    rates: Map<String, Double>,
    onDismiss: () -> Unit
) {
    val convertedAmount = if (currency.code == "USD") {
        amount
    } else {
        amount * (rates[currency.code] ?: 1.0)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Bolt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(48.dp)
            )
        },
        title = { Text("Boost Complete!", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "You earned a total of",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "${currency.symbol}${"%.6f".format(convertedAmount)}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Keep it up!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Great!")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelDialog(
    totalBoost: Double,
    platformBalance: Double,
    platformVolume: Double,
    allUsers: List<Map<String, Any>>,
    isMaintenance: Boolean,
    leaderboard: List<UserLeaderboard>,
    onDismiss: () -> Unit, 
    onAdd1000: () -> Unit, 
    onSetBalance: (String, Double) -> Unit,
    onToggleMaintenance: (Boolean) -> Unit,
    onUpdateUserStatus: (String, Boolean) -> Unit
) {
    var customBalance by remember { mutableStateOf("") }
    var targetUid by remember { mutableStateOf("") }
    var userSearchQuery by remember { mutableStateOf("") }
    var minBalance by remember { mutableStateOf("") }
    var maxBalance by remember { mutableStateOf("") }
    var sortByNewest by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("None") }
    var filterExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Admin Panel", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("System Boost", style = MaterialTheme.typography.labelSmall)
                            Text("$${"%.2f".format(totalBoost)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Platform Bal.", style = MaterialTheme.typography.labelSmall)
                            Text("$${"%.2f".format(platformBalance)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Total Platform Volume", style = MaterialTheme.typography.labelSmall)
                        Text("$${"%.2f".format(platformVolume)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                // Maintenance Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Boost Maintenance", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text("Disable boost for everyone", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = isMaintenance, onCheckedChange = onToggleMaintenance)
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                
                // User Management
                Text("User Management", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Filter Dropdown
                        ExposedDropdownMenuBox(
                            expanded = filterExpanded,
                            onExpandedChange = { filterExpanded = !filterExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedFilter,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Filter Type") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = filterExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = filterExpanded,
                                onDismissRequest = { filterExpanded = false }
                            ) {
                                listOf("None", "Balance", "Date").forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            selectedFilter = option
                                            filterExpanded = false
                                            // Reset other filters when switching
                                            minBalance = ""
                                            maxBalance = ""
                                            sortByNewest = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        OutlinedTextField(
                            value = userSearchQuery,
                            onValueChange = { userSearchQuery = it },
                            label = { Text("Search by name, email, or UID") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            singleLine = true
                        )
                        
                        if (selectedFilter == "Balance") {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = minBalance,
                                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) minBalance = it },
                                    label = { Text("Min Bal") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = maxBalance,
                                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) maxBalance = it },
                                    label = { Text("Max Bal") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                            }
                        }
                        
                        if (selectedFilter == "Date") {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("Sort by Newest", style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.weight(1f))
                                Switch(checked = sortByNewest, onCheckedChange = { sortByNewest = it })
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                val filteredUsers = allUsers.filter { user ->
                    val name = user["name"] as? String ?: ""
                    val email = user["email"] as? String ?: ""
                    val uid = user["uid"] as? String ?: ""
                    val balance = (user["balance"] as? Number)?.toDouble() ?: 0.0
                    
                    val matchesSearch = name.contains(userSearchQuery, ignoreCase = true) ||
                                       email.contains(userSearchQuery, ignoreCase = true) ||
                                       uid.contains(userSearchQuery, ignoreCase = true)
                    
                    val matchesBalance = if (selectedFilter == "Balance") {
                        val minBal = minBalance.toDoubleOrNull() ?: 0.0
                        val maxBal = maxBalance.toDoubleOrNull() ?: Double.MAX_VALUE
                        balance in minBal..maxBal
                    } else true
                    
                    matchesSearch && matchesBalance
                }.let { list ->
                    if (selectedFilter == "Date" && sortByNewest) {
                        list.sortedByDescending { (it["createdAt"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0L }
                    } else {
                        list
                    }
                }

                Column(modifier = Modifier.heightIn(max = 300.dp)) {
                    filteredUsers.take(10).forEach { user ->
                        val uid = user["uid"] as? String ?: ""
                        val name = user["name"] as? String ?: "No Name"
                        val email = user["email"] as? String ?: ""
                        val isSuspended = user["isSuspended"] as? Boolean ?: false
                        
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSuspended) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f) 
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(name, fontWeight = FontWeight.Bold)
                                    Text(email, style = MaterialTheme.typography.labelSmall)
                                }
                                TextButton(
                                    onClick = { onUpdateUserStatus(uid, !isSuspended) },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = if (isSuspended) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text(if (isSuspended) "Activate" else "Suspend")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Leaderboard
                Text("Top Balances (Admin View)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                leaderboard.forEachIndexed { index, user ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${index + 1}. ${user.name} (${user.uid.take(6)}...)")
                        Text("$${"%.6f".format(user.balance)}", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onAdd1000(); onDismiss() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Add $1,000 to YOUR account")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Manual Fund Adjustment", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = targetUid,
                    onValueChange = { targetUid = it },
                    label = { Text("User UID (Optional, default: You)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = customBalance,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) customBalance = it },
                    label = { Text("Set Custom Balance ($)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { 
                        val bal = customBalance.toDoubleOrNull()
                        if (bal != null) {
                            onSetBalance(targetUid, bal)
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    enabled = customBalance.isNotEmpty()
                ) {
                    Text("Apply Balance Change")
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Close")
            }
        }
    )
}

@Composable
fun LeaderboardDialog(
    leaderboard: List<UserLeaderboard>, 
    currentUserUid: String, 
    currentUserBalance: Double, 
    currentUserRank: Int?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Top Balance Leaderboard")
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                if (leaderboard.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No data available")
                    }
                } else {
                    LazyColumn {
                        items(leaderboard.size) { index ->
                            val user = leaderboard[index]
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (user.uid == currentUserUid) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f)
                                    else if (index < 3) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "#${index + 1}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(40.dp),
                                        color = when(index) {
                                            0 -> MaterialTheme.colorScheme.tertiary // Gold
                                            1 -> Color(0xFFC0C0C0) // Silver
                                            2 -> Color(0xFFCD7F32) // Bronze
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (user.uid == currentUserUid) "${user.name} (You)" else user.name,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = getUserStatus(index + 1, user.balance),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.tertiary,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "ID: ${user.uid.take(8)}...",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                    Text(
                                        "$${"%.4f".format(user.balance)}",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        
                        // Show current user at bottom if not in top 20
                        if (leaderboard.none { it.uid == currentUserUid }) {
                             item {
                                 Spacer(modifier = Modifier.height(16.dp))
                                 Text("Your Rank: ${currentUserRank ?: "Calculating..."}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                 Card(
                                     modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                     colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f))
                                 ) {
                                     Row(
                                         modifier = Modifier.padding(16.dp),
                                         verticalAlignment = Alignment.CenterVertically
                                     ) {
                                         Text(
                                             text = "#${currentUserRank ?: "..."}",
                                             style = MaterialTheme.typography.titleMedium,
                                             fontWeight = FontWeight.Bold,
                                             modifier = Modifier.width(40.dp)
                                         )
                                         Column(modifier = Modifier.weight(1f)) {
                                             Text("You", fontWeight = FontWeight.Bold)
                                             Text(
                                                 text = getUserStatus(currentUserRank, currentUserBalance),
                                                 style = MaterialTheme.typography.labelSmall,
                                                 color = MaterialTheme.colorScheme.tertiary,
                                                 fontWeight = FontWeight.ExtraBold
                                             )
                                         }
                                         Text(
                                             "$${"%.4f".format(currentUserBalance)}",
                                             fontWeight = FontWeight.ExtraBold,
                                             color = MaterialTheme.colorScheme.primary
                                         )
                                     }
                                 }
                             }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Close")
            }
        }
    )
}

@Composable
fun ReceiveMoneyDialog(uid: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Receive Money", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.QrCode, null, modifier = Modifier.size(150.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Share your UID to receive money", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        uid,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Done")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(
    viewModel: WalletViewModel,
    currentName: String,
    currentUsername: String,
    currentPhone: String,
    currentEmail: String,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var username by remember { mutableStateOf(currentUsername) }
    var phoneNumber by remember { mutableStateOf(currentPhone) }
    var email by remember { mutableStateOf(currentEmail) }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val currentAvatarUrl by viewModel.userAvatarUrl.collectAsState()
    var selectedAvatarUrl by remember { mutableStateOf(currentAvatarUrl) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            isUploading = true
            uploadError = null
            viewModel.uploadAvatarImage(
                uri = uri,
                onSuccess = { downloadUrl ->
                    selectedAvatarUrl = downloadUrl
                    isUploading = false
                },
                onError = { err ->
                    uploadError = err
                    isUploading = false
                }
            )
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header with Premium Brand Theme and Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.AccountBox,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = "Account Profile",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Configure your identity credentials",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = { if (!isLoading) onDismiss() },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

                // Avatar Editor Center
                Box(
                    modifier = Modifier.size(96.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        } else if (selectedAvatarUrl.isNotEmpty()) {
                            AsyncImage(
                                model = selectedAvatarUrl,
                                contentDescription = "Avatar Picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Default Icon",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    // Elegant gold edit button badge directly overlaying the avatar
                    IconButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        enabled = !isUploading && !isLoading,
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                            .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            .testTag("upload_avatar_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Upload Avatar",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (uploadError != null) {
                    Text(
                        uploadError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                // Preset Avatar List Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Or Select Premium Preset",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    val avatarPresets = listOf(
                        "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80",
                        "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=150&q=80",
                        "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?auto=format&fit=crop&w=150&q=80",
                        "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?auto=format&fit=crop&w=150&q=80",
                        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=150&q=80",
                        "https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=150&q=80"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        avatarPresets.forEach { presetUrl ->
                            val isSelected = selectedAvatarUrl == presetUrl
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        selectedAvatarUrl = presetUrl
                                        viewModel.updateUserAvatar(presetUrl)
                                    }
                            ) {
                                AsyncImage(
                                    model = presetUrl,
                                    contentDescription = "Preset Picture",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Input Forms
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; error = null },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        singleLine = true,
                        enabled = !isLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                            focusedLabelColor = MaterialTheme.colorScheme.tertiary
                        )
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it; error = null },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        singleLine = true,
                        enabled = !isLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                            focusedLabelColor = MaterialTheme.colorScheme.tertiary
                        )
                    )

                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it; error = null },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        enabled = !isLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                            focusedLabelColor = MaterialTheme.colorScheme.tertiary
                        )
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; error = null },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        enabled = !isLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                            focusedLabelColor = MaterialTheme.colorScheme.tertiary
                        )
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; error = null },
                        label = { Text("New Password (optional)") },
                        placeholder = { Text("Leave blank to keep current", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        enabled = !isLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                            focusedLabelColor = MaterialTheme.colorScheme.tertiary
                        )
                    )
                }

                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                    )
                }

                // Action Buttons Row
                Button(
                    onClick = {
                        if (name.isBlank() || email.isBlank() || username.isBlank() || phoneNumber.isBlank()) {
                            error = "Please fill in all required fields"
                            return@Button
                        }
                        isLoading = true
                        viewModel.updateUserProfileAndCredentials(
                            name = name.trim(),
                            email = email.trim(),
                            username = username.trim(),
                            phoneNumber = phoneNumber.trim(),
                            passwordToUpdate = password.trim(),
                            onSuccess = {
                                isLoading = false
                                onDismiss()
                            },
                            onError = { errMsg ->
                                isLoading = false
                                error = errMsg
                            }
                        )
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = Color.Black
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black, strokeWidth = 2.dp)
                    } else {
                        Text("Save Profile Credentials", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AdminDashboardScreen(
    viewModel: WalletViewModel,
    onBack: () -> Unit
) {
    val totalSystemBoost by viewModel.totalSystemBoost.collectAsState()
    val totalPlatformBalance by viewModel.totalPlatformBalance.collectAsState()
    val platformVolume by viewModel.platformVolume.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val isBoostDisabled by viewModel.isBoostDisabled.collectAsState()
    val globalBoostRatePercent by viewModel.globalBoostRatePercent.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var userToEdit by remember { mutableStateOf<Map<String, Any>?>(null) }
    var globalRateInput by remember { mutableStateOf(globalBoostRatePercent.toString()) }

    var minBalanceInput by remember { mutableStateOf("") }
    var maxBalanceInput by remember { mutableStateOf("") }
    var sortByOption by remember { mutableStateOf("Newest Users") }
    var selectedFilterPreset by remember { mutableStateOf("All Users") }
    var filterDropdownExpanded by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    // Sync input field when database value changes
    LaunchedEffect(globalBoostRatePercent) {
        globalRateInput = globalBoostRatePercent.toString()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Control Panel", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.fetchAdminData()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(modifier = Modifier.padding(innerPadding)) {
            val isNarrow = maxWidth < 480.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Stats Panel
                Text("System & Platform Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard("Platform Balance", "$${"%,.2f".format(totalPlatformBalance)}", Icons.Default.AccountBalanceWallet, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.weight(1f))
                    StatCard("Platform Volume", "$${"%,.2f".format(platformVolume)}", Icons.Default.TrendingUp, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard("System Boost", "$${"%,.2f".format(totalSystemBoost)}", Icons.Default.Bolt, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.weight(1f))
                    StatCard("Total Users", "${allUsers.size}", Icons.Default.People, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, bringIntoViewRequester, coroutineScope, modifier = Modifier.weight(1f))
                }

                HorizontalDivider()

                // Global System Configuration
                Text("Global Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Maintenance mode
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Boost Maintenance Mode", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                Text("Disable the compounding boost feature for all users.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                            }
                            Switch(
                                checked = isBoostDisabled,
                                onCheckedChange = { viewModel.toggleBoostMaintenance(it) }
                            )
                        }

                        HorizontalDivider()

                        // Global User % (Multiplier) setting
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Global User Boost/Earning Rate (%)", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text("Change the compounding multiplier (default: 100%). E.g. 200% doubles earnings, 50% halves it.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                            
                            val rateRowModifier = if (isNarrow) Modifier.fillMaxWidth() else Modifier.fillMaxWidth()
                            val rateRowContent: @Composable RowScope.() -> Unit = {
                                OutlinedTextField(
                                    value = globalRateInput,
                                    onValueChange = { globalRateInput = it },
                                    label = { Text("Earning rate percent") },
                                    modifier = Modifier.weight(1f),
                                    leadingIcon = { Icon(Icons.Default.TrendingUp, contentDescription = null) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Button(
                                    onClick = {
                                        val pct = globalRateInput.toDoubleOrNull()
                                        if (pct != null && pct >= 0.0) {
                                            viewModel.updateGlobalBoostRate(pct)
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Save %")
                                }
                            }

                            if (isNarrow) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = globalRateInput,
                                        onValueChange = { globalRateInput = it },
                                        label = { Text("Earning rate percent") },
                                        modifier = Modifier.fillMaxWidth(),
                                        leadingIcon = { Icon(Icons.Default.TrendingUp, contentDescription = null) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Button(
                                        onClick = {
                                            val pct = globalRateInput.toDoubleOrNull()
                                            if (pct != null && pct >= 0.0) {
                                                viewModel.updateGlobalBoostRate(pct)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Save %")
                                    }
                                }
                            } else {
                                Row(
                                    modifier = rateRowModifier,
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    content = rateRowContent
                                )
                            }
                        }

                        HorizontalDivider()

                        // Live Announcement section (Realtime Database)
                        val liveAnnouncement by viewModel.liveAnnouncement.collectAsState(initial = "")
                        var announcementInput by remember { mutableStateOf("") }
                        
                        // Sync locally with DB when first loaded
                        LaunchedEffect(liveAnnouncement) {
                            if (announcementInput.isEmpty()) {
                                announcementInput = liveAnnouncement
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Live System Broadcast Announcement (Realtime DB)", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text("This alert is synced in real-time to the top of all active users' screens.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                            
                            if (isNarrow) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = announcementInput,
                                        onValueChange = { announcementInput = it },
                                        label = { Text("Broadcast Message") },
                                        placeholder = { Text("Type an alert or clear to hide...") },
                                        modifier = Modifier.fillMaxWidth(),
                                        leadingIcon = { Icon(Icons.Default.Campaign, contentDescription = null) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Button(
                                        onClick = {
                                            viewModel.updateLiveAnnouncement(announcementInput)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Broadcast")
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = announcementInput,
                                        onValueChange = { announcementInput = it },
                                        label = { Text("Broadcast Message") },
                                        placeholder = { Text("Type an alert or clear to hide...") },
                                        modifier = Modifier.weight(1f),
                                        leadingIcon = { Icon(Icons.Default.Campaign, contentDescription = null) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Button(
                                        onClick = {
                                            viewModel.updateLiveAnnouncement(announcementInput)
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Broadcast")
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                // User Management Search and List
                Text(
                    text = "User Directories & Management",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.bringIntoViewRequester(bringIntoViewRequester)
                )

                // 1. Filter Preset Dropdown (Above Search)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { filterDropdownExpanded = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.FilterList, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("Filter Group: ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(selectedFilterPreset, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }

                    DropdownMenu(
                        expanded = filterDropdownExpanded,
                        onDismissRequest = { filterDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        val presets = listOf("All Users", "With Active Balance", "Admins Only", "Suspended Users Only", "On Hold Users Only")
                        presets.forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset) },
                                onClick = {
                                    selectedFilterPreset = preset
                                    filterDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                
                // 2. Search Text Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search Users (Name/Email/Username/Phone)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, null)
                            }
                        }
                    },
                    singleLine = true
                )

                // 3. Min/Max Balance and Creation Date sorting controls (Below Search)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isNarrow) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = minBalanceInput,
                                    onValueChange = { minBalanceInput = it },
                                    label = { Text("Min Balance", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                OutlinedTextField(
                                    value = maxBalanceInput,
                                    onValueChange = { maxBalanceInput = it },
                                    label = { Text("Max Balance", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = minBalanceInput,
                                    onValueChange = { minBalanceInput = it },
                                    label = { Text("Min Balance", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                OutlinedTextField(
                                    value = maxBalanceInput,
                                    onValueChange = { maxBalanceInput = it },
                                    label = { Text("Max Balance", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Sort Users",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("Newest Users", "High Balance", "Low Balance").forEach { option ->
                                val isSelected = sortByOption == option
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { sortByOption = option },
                                    label = { Text(option) },
                                    leadingIcon = if (isSelected) {
                                        {
                                            Icon(
                                                imageVector = when (option) {
                                                    "High Balance" -> Icons.Default.TrendingUp
                                                    "Low Balance" -> Icons.Default.TrendingDown
                                                    else -> Icons.Default.Schedule
                                                },
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null
                                )
                            }
                        }
                    }
                }

                // 4. Combined Filtering and Sorting Logic
                val filteredUsers = allUsers.filter { user ->
                    val name = user["name"] as? String ?: ""
                    val email = user["email"] as? String ?: ""
                    val username = user["username"] as? String ?: ""
                    val phone = user["phoneNumber"] as? String ?: ""
                    val uid = user["uid"] as? String ?: ""
                    
                    val matchesSearch = name.contains(searchQuery, ignoreCase = true) ||
                            email.contains(searchQuery, ignoreCase = true) ||
                            username.contains(searchQuery, ignoreCase = true) ||
                            phone.contains(searchQuery, ignoreCase = true) ||
                            uid.contains(searchQuery, ignoreCase = true)

                    val matchesPreset = when (selectedFilterPreset) {
                        "With Active Balance" -> {
                            val bal = (user["balance"] as? Number)?.toDouble() ?: 0.0
                            bal > 0.0
                        }
                        "Admins Only" -> {
                            user["isAdmin"] as? Boolean ?: false
                        }
                        "Suspended Users Only" -> {
                            user["isSuspended"] as? Boolean ?: false
                        }
                        "On Hold Users Only" -> {
                            user["isOnHold"] as? Boolean ?: false
                        }
                        else -> true
                    }

                    val balanceVal = (user["balance"] as? Number)?.toDouble() ?: 0.0
                    val matchesMin = minBalanceInput.toDoubleOrNull()?.let { balanceVal >= it } ?: true
                    val matchesMax = maxBalanceInput.toDoubleOrNull()?.let { balanceVal <= it } ?: true

                    matchesSearch && matchesPreset && matchesMin && matchesMax
                }

                val sortedUsers = when (sortByOption) {
                    "High Balance" -> {
                        filteredUsers.sortedByDescending { (it["balance"] as? Number)?.toDouble() ?: 0.0 }
                    }
                    "Low Balance" -> {
                        filteredUsers.sortedBy { (it["balance"] as? Number)?.toDouble() ?: 0.0 }
                    }
                    "Newest Users" -> {
                        filteredUsers.sortedWith { u1, u2 ->
                            val t1 = (u1["createdAt"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0L
                            val t2 = (u2["createdAt"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0L
                            t2.compareTo(t1) // descending (newest first)
                        }
                    }
                    else -> filteredUsers
                }

                if (sortedUsers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No users found matching query.", color = MaterialTheme.colorScheme.secondary)
                    }
                } else {
                    sortedUsers.forEach { user ->
                        val uid = user["uid"] as? String ?: ""
                        val name = user["name"] as? String ?: "No Name"
                        val email = user["email"] as? String ?: ""
                        val username = user["username"] as? String ?: ""
                        val phone = user["phoneNumber"] as? String ?: ""
                        val balanceVal = (user["balance"] as? Number)?.toDouble() ?: 0.0
                        val isSuspended = user["isSuspended"] as? Boolean ?: false
                        val isOnHold = user["isOnHold"] as? Boolean ?: false
                        val isUserAdmin = user["isAdmin"] as? Boolean ?: false

                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSuspended) {
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                                } else if (isOnHold) {
                                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                }
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                val userCardContent: @Composable () -> Unit = {
                                    Column(modifier = if (isNarrow) Modifier.fillMaxWidth() else Modifier.weight(1f)) {
                                        Text(name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        if (username.isNotEmpty()) {
                                            Text("@$username", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Email: $email", style = MaterialTheme.typography.bodySmall)
                                        if (phone.isNotEmpty()) {
                                            Text("Phone: $phone", style = MaterialTheme.typography.bodySmall)
                                        }
                                        Text("UID: ${uid.take(10)}...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                    }
                                    
                                    if (isNarrow) Spacer(modifier = Modifier.height(12.dp))

                                    Column(
                                        modifier = if (isNarrow) Modifier.fillMaxWidth() else Modifier,
                                        horizontalAlignment = if (isNarrow) Alignment.Start else Alignment.End
                                    ) {
                                        Text(
                                            text = "$${"%,.2f".format(balanceVal)}",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (isUserAdmin) {
                                                SuggestionChip(
                                                    onClick = {},
                                                    label = { Text("Admin", style = MaterialTheme.typography.labelSmall) },
                                                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                                )
                                            }
                                            if (isSuspended) {
                                                SuggestionChip(
                                                    onClick = {},
                                                    label = { Text("Banned", style = MaterialTheme.typography.labelSmall) },
                                                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                                                )
                                            }
                                            if (isOnHold) {
                                                SuggestionChip(
                                                    onClick = {},
                                                    label = { Text("Hold", style = MaterialTheme.typography.labelSmall) },
                                                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                                                )
                                            }
                                            if (!isSuspended && !isOnHold) {
                                                SuggestionChip(
                                                    onClick = {},
                                                    label = { Text("Active", style = MaterialTheme.typography.labelSmall) }
                                                )
                                            }
                                        }
                                    }
                                }

                                if (isNarrow) {
                                    Column { userCardContent() }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        userCardContent()
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    OutlinedButton(
                                        onClick = { userToEdit = user },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Edit User details")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Sheet or Dialog for editing user details
    userToEdit?.let { user ->
        var editName by remember { mutableStateOf(user["name"] as? String ?: "") }
        var editEmail by remember { mutableStateOf(user["email"] as? String ?: "") }
        var editUsername by remember { mutableStateOf(user["username"] as? String ?: "") }
        var editPhone by remember { mutableStateOf(user["phoneNumber"] as? String ?: "") }
        var editBalance by remember { mutableStateOf(((user["balance"] as? Number)?.toDouble() ?: 0.0).toString()) }
        var editSuspended by remember { mutableStateOf(user["isSuspended"] as? Boolean ?: false) }
        var editHold by remember { mutableStateOf(user["isOnHold"] as? Boolean ?: false) }
        val editUid = user["uid"] as? String ?: ""

        AlertDialog(
            onDismissRequest = { userToEdit = null },
            title = { Text("Edit User Account", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("UID: $editUid", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = editUsername,
                        onValueChange = { editUsername = it },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = editBalance,
                        onValueChange = { editBalance = it },
                        label = { Text("Wallet Balance ($)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) }
                    )

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Ban / Suspend User", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text("Blocks login and all system interactions", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = editSuspended,
                            onCheckedChange = { editSuspended = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Hold Wallet Status", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text("Prevents spending/transfers/boosting", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = editHold,
                            onCheckedChange = { editHold = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalBal = editBalance.toDoubleOrNull() ?: 0.0
                        viewModel.updateUserDetails(
                            targetUid = editUid,
                            name = editName,
                            email = editEmail,
                            username = editUsername,
                            phoneNumber = editPhone,
                            balance = finalBal,
                            isSuspended = editSuspended,
                            isOnHold = editHold
                        )
                        userToEdit = null
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color,
    bringIntoViewRequester: BringIntoViewRequester? = null,
    coroutineScope: kotlinx.coroutines.CoroutineScope? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .then(
                if (bringIntoViewRequester != null && coroutineScope != null) {
                    Modifier.clickable {
                        coroutineScope.launch {
                            bringIntoViewRequester.bringIntoView()
                        }
                    }
                } else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, tint = contentColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.8f))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun FloatingCurvedNavBar(
    modifier: Modifier = Modifier,
    currentScreen: String,
    onScreenSelected: (String) -> Unit
) {
    Surface(
        modifier = modifier
            .padding(horizontal = 48.dp)
            .height(64.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shadowElevation = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavBarItem(
                icon = Icons.Default.Home,
                label = "Home",
                isSelected = currentScreen == "Home",
                onClick = { onScreenSelected("Home") }
            )
            NavBarItem(
                icon = Icons.Default.EmojiEvents,
                label = "Leaderboard",
                isSelected = false,
                onClick = { onScreenSelected("Leaderboard") }
            )
            NavBarItem(
                icon = Icons.Default.Settings,
                label = "Settings",
                isSelected = currentScreen == "Settings",
                onClick = { onScreenSelected("Settings") }
            )
            NavBarItem(
                icon = Icons.Default.Person,
                label = "Profile",
                isSelected = currentScreen == "Profile",
                onClick = { onScreenSelected("Profile") }
            )
        }
    }
}

@Composable
fun NavBarItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "nav_bg"
    )
    val contentColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "nav_content"
    )
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            if (isSelected) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = contentColor
                )
            }
        }
    }
}
