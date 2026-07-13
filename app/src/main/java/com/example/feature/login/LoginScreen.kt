package com.example.feature.login

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.WalletViewModel
import com.example.presentation.utils.PhoneNumberVisualTransformation
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

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
