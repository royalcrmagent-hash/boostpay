package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.network.NetworkClient
import com.example.core.security.DeviceIntegrityDetector
import com.example.core.security.PlayIntegrityHelper
import com.example.core.security.SecurePreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import com.google.firebase.firestore.DocumentReference
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SnapshotMetadata

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import java.util.Date
import com.example.domain.models.CurrencyData
import com.example.domain.models.Transaction
import com.example.domain.models.UserLeaderboard
import com.example.domain.models.ChatMessage

data class CurrencyResponse(
    val result: String = "",
    val rates: Map<String, Double>? = null,
    val conversion_rates: Map<String, Double>? = null
)

interface CurrencyService {
    @GET("v6/latest/USD")
    suspend fun getLatestRates(): CurrencyResponse

    @GET("v4/latest/USD")
    suspend fun getLatestRatesFallback(): CurrencyResponse
}

fun getFlagEmoji(currencyCode: String): String {
    val currencyToCountry = mapOf(
        "USD" to "US", "EUR" to "EU", "GBP" to "GB", "JPY" to "JP", "INR" to "IN",
        "BDT" to "BD", "CNY" to "CN", "RUB" to "RU", "KRW" to "KR", "TRY" to "TR",
        "PKR" to "PK", "AED" to "AE", "SAR" to "SA", "CAD" to "CA", "AUD" to "AU",
        "SGD" to "SG", "CHF" to "CH", "MYR" to "MY", "THB" to "TH", "IDR" to "ID",
        "PHP" to "PH", "VND" to "VN", "BRL" to "BR", "MXN" to "MX", "ZAR" to "ZA",
        "EGP" to "EG", "NGN" to "NG", "GHS" to "GH", "KES" to "KE", "LKR" to "LK",
        "NPR" to "NP", "AFN" to "AF", "IQD" to "IQ", "KWD" to "KW", "QAR" to "QA",
        "OMR" to "OM", "BHD" to "BH", "JOD" to "JO", "ILS" to "IL", "PLN" to "PL",
        "SEK" to "SE", "NOK" to "NO", "DKK" to "DK", "HUF" to "HU", "CZK" to "CZ",
        "RON" to "RO", "BGN" to "BG", "ARS" to "AR", "CLP" to "CL", "COP" to "CO",
        "PEN" to "PE", "UYU" to "UY", "NZD" to "NZ", "HKD" to "HK", "TWD" to "TW",
        "UAH" to "UA", "KZT" to "KZ", "DZD" to "DZ", "MAD" to "MA", "TND" to "TN",
        "EGP" to "EG", "QAR" to "QA", "OMR" to "OM", "KWD" to "KW", "BHD" to "BH",
        "MVR" to "MV", "MUR" to "MU", "SCR" to "SC", "MOP" to "MO", "KHR" to "KH",
        "LAK" to "LA", "MMK" to "MM", "MNT" to "MN", "BND" to "BN", "FJD" to "FJ",
        "PGK" to "PG", "SBD" to "SB", "VUV" to "VU", "TOP" to "TO", "WST" to "WS"
    )
    
    val countryCode = currencyToCountry[currencyCode] ?: if (currencyCode.length >= 2) currencyCode.take(2) else "US"
    
    return try {
        val firstLetter = Character.codePointAt(countryCode, 0) - 0x41 + 0x1F1E6
        val secondLetter = Character.codePointAt(countryCode, 1) - 0x41 + 0x1F1E6
        String(Character.toChars(firstLetter)) + String(Character.toChars(secondLetter))
    } catch (e: Exception) {
        "🏳️"
    }
}

class WalletViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val rtdb = com.google.firebase.database.FirebaseDatabase.getInstance()
    
    private val _currentDeviceId = MutableStateFlow("")

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages

    private val _liveAnnouncement = MutableStateFlow("")
    val liveAnnouncement: StateFlow<String> = _liveAnnouncement
    
    private val _balance = MutableStateFlow(0.0)
    val balance: StateFlow<Double> = _balance

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName

    private val _userUsername = MutableStateFlow("")
    val userUsername: StateFlow<String> = _userUsername

    private val _userPhone = MutableStateFlow("")
    val userPhone: StateFlow<String> = _userPhone

    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail

    private val _userAvatarUrl = MutableStateFlow("")
    val userAvatarUrl: StateFlow<String> = _userAvatarUrl

    private val _isEmailVerified = MutableStateFlow(auth.currentUser?.isEmailVerified ?: false)
    val isEmailVerified: StateFlow<Boolean> = _isEmailVerified

    private val _totalBoosted = MutableStateFlow(0.0)
    val totalBoosted: StateFlow<Double> = _totalBoosted

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions

    private val _currentUserUid = MutableStateFlow(auth.currentUser?.uid ?: "")
    val currentUserUid: StateFlow<String> = _currentUserUid

    private val _isBoosting = MutableStateFlow(false)
    val isBoosting: StateFlow<Boolean> = _isBoosting

    private val _boostTimeRemaining = MutableStateFlow(0)
    val boostTimeRemaining: StateFlow<Int> = _boostTimeRemaining

    private val _currentMaxBoostDurationMs = MutableStateFlow(60000L)
    val currentMaxBoostDurationMs: StateFlow<Long> = _currentMaxBoostDurationMs

    private val _boostEarnings = MutableStateFlow<Double?>(null)
    val boostEarnings: StateFlow<Double?> = _boostEarnings

    private val _totalSystemBoost = MutableStateFlow(0.0)
    val totalSystemBoost: StateFlow<Double> = _totalSystemBoost

    private val _leaderboard = MutableStateFlow<List<UserLeaderboard>>(emptyList())
    val leaderboard: StateFlow<List<UserLeaderboard>> = _leaderboard

    private val _isBoostDisabled = MutableStateFlow(false)
    val isBoostDisabled: StateFlow<Boolean> = _isBoostDisabled

    private val _totalPlatformBalance = MutableStateFlow(0.0)
    val totalPlatformBalance: StateFlow<Double> = _totalPlatformBalance

    private val _selectedCurrency = MutableStateFlow(CurrencyData("USD", "$", "US Dollar", getFlagEmoji("USD")))
    val selectedCurrency: StateFlow<CurrencyData> = _selectedCurrency

    private val _availableCurrencies = MutableStateFlow<List<CurrencyData>>(listOf(CurrencyData("USD", "$", "US Dollar", getFlagEmoji("USD"))))
    val availableCurrencies: StateFlow<List<CurrencyData>> = _availableCurrencies

    private val _exchangeRates = MutableStateFlow<Map<String, Double>>(emptyMap())
    val exchangeRates: StateFlow<Map<String, Double>> = _exchangeRates

    private val _showConfetti = MutableSharedFlow<Unit>()
    val showConfetti = _showConfetti.asSharedFlow()

    private var increaseJob: Job? = null
    private var pausedRemainingMs: Long = 0L
    private var lastBoostActiveTime: Long = 0L

    private var walletListener: ListenerRegistration? = null
    private var transactionsListener: ListenerRegistration? = null
    private var systemBoostListener: ListenerRegistration? = null
    private var settingsListener: ListenerRegistration? = null
    private var platformBalanceListener: ListenerRegistration? = null
    private var adminUsersListener: ListenerRegistration? = null
    private var adminTransactionsListener: ListenerRegistration? = null
    private var leaderboardListener: ListenerRegistration? = null

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin

    private val _isSuspended = MutableStateFlow(false)
    val isSuspended: StateFlow<Boolean> = _isSuspended

    private val _isOnHold = MutableStateFlow(false)
    val isOnHold: StateFlow<Boolean> = _isOnHold

    private val _globalBoostRatePercent = MutableStateFlow(100.0)
    val globalBoostRatePercent: StateFlow<Double> = _globalBoostRatePercent

    private val _allUsers = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val allUsers: StateFlow<List<Map<String, Any>>> = _allUsers

    private val _platformVolume = MutableStateFlow(0.0)
    val platformVolume: StateFlow<Double> = _platformVolume

    private val _notifications = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val notifications: SharedFlow<String> = _notifications

    fun setDeviceId(id: String) {
        _currentDeviceId.value = id
        val user = auth.currentUser
        if (user != null && id.isNotEmpty()) {
            val walletRef = db.collection("users").document(user.uid)
            walletRef.get().addOnSuccessListener { doc ->
                val remoteDeviceId = doc.getString("deviceId")
                if (remoteDeviceId != null && remoteDeviceId != id) {
                    walletRef.update(
                        mapOf(
                            "deviceId" to id,
                            "boostEndTime" to 0L
                        )
                    )
                } else {
                    walletRef.update("deviceId", id)
                }
            }
        }
    }

    private val _userRank = MutableStateFlow<Int?>(null)
    val userRank: StateFlow<Int?> = _userRank

    fun calculateUserRank() {
        val user = auth.currentUser ?: return
        val balance = _balance.value
        db.collection("users")
            .whereGreaterThan("balance", balance)
            .get()
            .addOnSuccessListener { snapshot ->
                _userRank.value = (snapshot?.size() ?: 0) + 1
            }
    }

    private fun checkDeviceIntegrity() {
        try {
            val isRooted = DeviceIntegrityDetector.isDeviceRooted()
            val isEmulator = DeviceIntegrityDetector.isRunningOnEmulator()
            val isHooked = DeviceIntegrityDetector.isHookFrameworkDetected()

            if (isRooted || isEmulator || isHooked) {
                val reasons = mutableListOf<String>()
                if (isRooted) reasons.add("Root Access")
                if (isEmulator) reasons.add("Emulator")
                if (isHooked) reasons.add("Hooks (Frida/Xposed)")

                val violationDesc = reasons.joinToString(", ")
                android.util.Log.e("WalletViewModel", "SECURITY ALERT - Device Integrity Violation: $violationDesc")
                
                _notifications.tryEmit("⚠️ Safety Alert: App is running in a potentially unsafe environment ($violationDesc).")

                // Log a security event to Firestore audit logs
                val user = auth.currentUser
                val auditRef = db.collection("audit_logs").document()
                val auditData = hashMapOf(
                    "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    "userId" to (user?.uid ?: "unauthenticated"),
                    "userEmail" to (user?.email ?: "none"),
                    "event" to "INTEGRITY_VIOLATION",
                    "details" to "Integrity checks triggered: $violationDesc",
                    "deviceModel" to android.os.Build.MODEL,
                    "deviceBrand" to android.os.Build.BRAND
                )
                auditRef.set(auditData)
            }
        } catch (e: Exception) {
            android.util.Log.e("WalletViewModel", "Failed to run device integrity checks", e)
        }
    }

    init {
        checkDeviceIntegrity()
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _currentUserUid.value = user?.uid ?: ""
            
            // Clear existing user-specific listeners
            walletListener?.remove()
            transactionsListener?.remove()

            if (user != null) {
                val walletRef = db.collection("users").document(user.uid)
                
                if (_currentDeviceId.value.isNotEmpty()) {
                    walletRef.get().addOnSuccessListener { doc ->
                        val remoteDeviceId = doc.getString("deviceId")
                        val updateTask = if (remoteDeviceId != null && remoteDeviceId != _currentDeviceId.value) {
                            walletRef.update(
                                mapOf(
                                    "deviceId" to _currentDeviceId.value,
                                    "boostEndTime" to 0L
                                )
                            )
                        } else {
                            walletRef.update("deviceId", _currentDeviceId.value)
                        }
                        
                        updateTask.addOnSuccessListener {
                            attachSnapshotListener(walletRef, user.email ?: "")
                        }
                    }.addOnFailureListener {
                        attachSnapshotListener(walletRef, user.email ?: "")
                    }
                } else {
                    attachSnapshotListener(walletRef, user.email ?: "")
                }
                    // Extracted to attachSnapshotListener

                val initialIsAdmin = user.email == "admin@gmail.com" || user.email == "royalcrmagent@gmail.com"
                _isAdmin.value = initialIsAdmin
                setupTransactionsListener(user.uid, initialIsAdmin)
                setupGlobalListeners()
            } else {
                clearGlobalListeners()
                _balance.value = 0.0
                _totalBoosted.value = 0.0
                _transactions.value = emptyList()
                _userName.value = ""
                _userUsername.value = ""
                _userPhone.value = ""
                _userEmail.value = ""
                _isAdmin.value = false
                _isSuspended.value = false
                _isOnHold.value = false
            }
        }

        // Global listeners (System-wide)
        // Extracted to setupGlobalListeners() and called upon authentication

        // 1. Listen for Real-Time Chat messages in Realtime Database
        rtdb.getReference("chat")
            .orderByChild("timestamp")
            .limitToLast(50)
            .addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val messages = mutableListOf<ChatMessage>()
                    for (child in snapshot.children) {
                        val msg = child.getValue(ChatMessage::class.java)
                        if (msg != null) {
                            messages.add(msg)
                        }
                    }
                    _chatMessages.value = messages.sortedBy { it.timestamp }
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    android.util.Log.e("WalletViewModel", "RTDB Chat listen failed: ${error.message}")
                }
            })

        // 2. Listen for Real-Time Live System Announcement in Realtime Database
        rtdb.getReference("announcement")
            .addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    _liveAnnouncement.value = snapshot.getValue(String::class.java) ?: ""
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    android.util.Log.e("WalletViewModel", "RTDB Announcement listen failed: ${error.message}")
                }
            })

        fetchExchangeRates()
    }

    fun fetchAdminData() {
        if (!_isAdmin.value) return
        
        // Listeners handle live updates, so we just log a refresh or optionally restart listeners.
        setupAdminListeners()
        fetchExchangeRates()
        _notifications.tryEmit("Admin dashboard data refreshed!")
    }

    fun refreshUserData() {
        fetchExchangeRates()
        if (_isAdmin.value) {
            fetchAdminData()
        } else {
            _notifications.tryEmit("Wallet data refreshed!")
        }
    }

    fun updateUserStatus(targetUid: String, isSuspended: Boolean) {
        if (!_isAdmin.value) return
        db.collection("users").document(targetUid).update("isSuspended", isSuspended)
            .addOnSuccessListener {
                fetchAdminData() // Refresh list
            }
    }

    fun updateGlobalBoostRate(percent: Double) {
        if (!_isAdmin.value) return
        db.collection("settings").document("global")
            .set(mapOf("globalBoostRatePercent" to percent), com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                _notifications.tryEmit("Global boost rate set to ${percent}%")
            }
    }

    fun updateUserDetails(
        targetUid: String,
        name: String,
        email: String,
        username: String,
        phoneNumber: String,
        balance: Double,
        isSuspended: Boolean,
        isOnHold: Boolean
    ) {
        if (!_isAdmin.value) return
        if (targetUid.isEmpty()) return

        db.collection("users").document(targetUid).get().addOnSuccessListener { snapshot ->
            val oldBalance = snapshot.getDouble("balance") ?: 0.0
            val balanceDiff = balance - oldBalance

            val updates = mapOf(
                "name" to name,
                "email" to email,
                "username" to username,
                "phoneNumber" to phoneNumber,
                "balance" to balance,
                "isSuspended" to isSuspended,
                "isOnHold" to isOnHold
            )

            db.collection("users").document(targetUid)
                .set(updates, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener {
                    fetchAdminData() // Refresh admin lists
                    if (java.lang.Math.abs(balanceDiff) > 0.0001) {
                        val transRef = db.collection("transactions").document()
                        val transactionData = mapOf(
                            "sender" to "ADMIN_SYSTEM",
                            "senderName" to "Administrator",
                            "receiver" to targetUid,
                            "receiverName" to name,
                            "amount" to balanceDiff,
                            "status" to "completed",
                            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                        )
                        transRef.set(transactionData)
                    }
                    _notifications.tryEmit("User details updated successfully")
                }
                .addOnFailureListener {
                    _notifications.tryEmit("Failed to update user: ${it.localizedMessage}")
                }
        }.addOnFailureListener {
            _notifications.tryEmit("Failed to fetch current user data: ${it.localizedMessage}")
        }
    }

    override fun onCleared() {
        super.onCleared()
        walletListener?.remove()
        transactionsListener?.remove()
        systemBoostListener?.remove()
        settingsListener?.remove()
        platformBalanceListener?.remove()
        leaderboardListener?.remove()
    }

    private fun fetchExchangeRates() {
        viewModelScope.launch {
            try {
                val moshi = Moshi.Builder()
                    .add(KotlinJsonAdapterFactory())
                    .build()
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://open.er-api.com/")
                    .client(NetworkClient.secureOkHttpClient)
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .build()
                val service = retrofit.create(CurrencyService::class.java)
                
                var response = try {
                    service.getLatestRates()
                } catch (e: Exception) {
                    android.util.Log.e("WalletViewModel", "Primary API failed, trying fallback", e)
                    // Fallback to older endpoint or slightly different URL if needed
                    val fallbackRetrofit = Retrofit.Builder()
                        .baseUrl("https://api.exchangerate-api.com/")
                        .client(NetworkClient.secureOkHttpClient)
                        .addConverterFactory(MoshiConverterFactory.create(moshi))
                        .build()
                    val fallbackService = fallbackRetrofit.create(CurrencyService::class.java)
                    fallbackService.getLatestRatesFallback()
                }

                val rates = response.rates ?: response.conversion_rates
                if (rates != null && rates.isNotEmpty()) {
                    _exchangeRates.value = rates
                    
                    // Better symbol mapping
                    val symbols = mapOf(
                        "USD" to "$", "EUR" to "€", "GBP" to "£", "JPY" to "¥", 
                        "INR" to "₹", "BDT" to "৳", "CNY" to "¥", "RUB" to "₽",
                        "KRW" to "₩", "TRY" to "₺", "PKR" to "₨", "AED" to "د.إ",
                        "SAR" to "﷼", "CAD" to "$", "AUD" to "$", "SGD" to "$",
                        "CHF" to "Fr", "MYR" to "RM", "THB" to "฿", "IDR" to "Rp",
                        "PHP" to "₱", "VND" to "₫", "BRL" to "R$", "MXN" to "$",
                        "ZAR" to "R", "EGP" to "E£", "NGN" to "₦", "GHS" to "GH₵",
                        "KES" to "KSh", "LKR" to "Rs", "NPR" to "₨", "AFN" to "؋",
                        "IQD" to "ع.د", "KWD" to "د.ك", "QAR" to "ر.ق", "OMR" to "ر.ع.",
                        "BHD" to "ب.د", "JOD" to "د.ا", "ILS" to "₪", "PLN" to "zł",
                        "SEK" to "kr", "NOK" to "kr", "DKK" to "kr", "HUF" to "Ft",
                        "CZK" to "Kč", "RON" to "lei", "BGN" to "лв", "ARS" to "$",
                        "CLP" to "$", "COP" to "$", "PEN" to "S/.", "UYU" to "\$U"
                    )
                    
                    val newList = rates.keys.map { code ->
                        try {
                            val javaCurrency = java.util.Currency.getInstance(code)
                            CurrencyData(
                                code = code,
                                symbol = symbols[code] ?: javaCurrency.getSymbol(java.util.Locale.ROOT),
                                name = javaCurrency.getDisplayName(java.util.Locale.getDefault()),
                                flag = getFlagEmoji(code)
                            )
                        } catch (e: Exception) {
                            CurrencyData(code, symbols[code] ?: code, code, getFlagEmoji(code))
                        }
                    }.sortedBy { it.code }
                    
                    if (newList.isNotEmpty()) {
                        _availableCurrencies.value = newList
                        
                        // Auto-detect local currency if not already changed from USD
                        try {
                            val localCurrency = java.util.Currency.getInstance(java.util.Locale.getDefault())
                            val localCode = localCurrency.currencyCode
                            if (newList.any { it.code == localCode } && _selectedCurrency.value.code == "USD") {
                                val detected = newList.first { it.code == localCode }
                                _selectedCurrency.value = detected
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("WalletViewModel", "Failed to detect local currency", e)
                        }
                    }
                } else {
                    android.util.Log.e("WalletViewModel", "API response success but rates are null or empty")
                }
            } catch (e: Exception) {
                android.util.Log.e("WalletViewModel", "Failed to fetch rates", e)
            }
        }
    }

    fun setCurrency(currency: CurrencyData) {
        _selectedCurrency.value = currency
    }

    fun toggleBoostMaintenance(disabled: Boolean) {
        db.collection("settings").document("global")
            .set(mapOf("boostDisabled" to disabled), com.google.firebase.firestore.SetOptions.merge())
    }

    fun stopBoost() {
        lastBoostActiveTime = System.currentTimeMillis()
        pausedRemainingMs = _boostTimeRemaining.value * 1000L
        auth.currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).update(
                mapOf(
                    "boostEndTime" to 0L,
                    "pausedRemainingMs" to pausedRemainingMs
                )
            )
        }
        increaseJob?.cancel()
        increaseJob = null
        _isBoosting.value = false
        // DO NOT reset _boostTimeRemaining to 0 so the UI can show paused time
    }

    fun resetState() {
        stopBoost()
        pausedRemainingMs = 0L
        _boostTimeRemaining.value = 0
        _balance.value = 0.0
        _totalBoosted.value = 0.0
        _transactions.value = emptyList()
        _userName.value = ""
        _userUsername.value = ""
        _userPhone.value = ""
        _userEmail.value = ""
        _isAdmin.value = false
        _isSuspended.value = false
        _isOnHold.value = false
        _boostEarnings.value = null
    }

    private var lastTransactionAttemptTime = 0L

    fun sendMoney(receiverUid: String, amount: Double, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val nowMs = System.currentTimeMillis()
        if (nowMs - lastTransactionAttemptTime < 3000) {
            onError("Rate limit exceeded: Please wait 3 seconds between transactions.")
            return
        }
        lastTransactionAttemptTime = nowMs

        if (_isBoosting.value) {
            onError("Cannot send money while boosting")
            return
        }
        if (_isSuspended.value) {
            onError("Your account is suspended")
            return
        }
        if (_isOnHold.value) {
            onError("Your account is on hold")
            return
        }
        val user = auth.currentUser ?: return
        if (receiverUid == user.uid) {
            onError("Cannot send money to yourself")
            return
        }
        if (_balance.value < amount) {
            onError("Insufficient balance")
            return
        }

        // Generate dynamic transaction idempotency key (deduplicates requests within a 5-second window)
        val timeGroup = System.currentTimeMillis() / 5000
        val idempotencyKey = "tx_${user.uid}_${receiverUid}_${String.format("%.4f", amount)}_${timeGroup}"

        db.runTransaction { transaction ->
            val senderRef = db.collection("users").document(user.uid)
            val receiverRef = db.collection("users").document(receiverUid)

            val senderSnapshot = transaction.get(senderRef)
            val receiverSnapshot = transaction.get(receiverRef)

            if (!receiverSnapshot.exists()) {
                throw Exception("Receiver not found")
            }

            // Transaction Idempotency Check: prevent double spending / replay attacks
            val transRef = db.collection("transactions").document(idempotencyKey)
            if (transaction.get(transRef).exists()) {
                throw Exception("Transaction already processed (Idempotency Check)")
            }

            val senderBalance = senderSnapshot.getDouble("balance") ?: 0.0
            val receiverBalance = receiverSnapshot.getDouble("balance") ?: 0.0
            val senderName = senderSnapshot.getString("name") ?: "Unknown"
            val receiverName = receiverSnapshot.getString("name") ?: "Unknown"

            // STRICT SERVER-SIDE BALANCE VALIDATION
            if (senderBalance < amount) {
                throw Exception("Insufficient balance")
            }

            val newSenderBalance = senderBalance - amount
            transaction.update(senderRef, "balance", newSenderBalance)
            transaction.update(receiverRef, "balance", receiverBalance + amount)

            val pausedMs = senderSnapshot.getLong("pausedRemainingMs") ?: 0L
            val boostEndTime = senderSnapshot.getLong("boostEndTime") ?: 0L
            val now = System.currentTimeMillis()

            // Scale sender boost
            if (pausedMs > 0L && senderBalance > 0.0) {
                val ratio = newSenderBalance / senderBalance
                val newPausedMs = (pausedMs * ratio).toLong().coerceAtLeast(0L)
                transaction.update(senderRef, "pausedRemainingMs", newPausedMs)
            } else if (boostEndTime > now && senderBalance > 0.0) {
                val ratio = newSenderBalance / senderBalance
                val remainingMs = boostEndTime - now
                val newRemainingMs = (remainingMs * ratio).toLong().coerceAtLeast(0L)
                val newBoostEndTime = now + newRemainingMs
                transaction.update(senderRef, "boostEndTime", newBoostEndTime)
            }

            // Scale receiver boost
            val receiverPausedMs = receiverSnapshot.getLong("pausedRemainingMs") ?: 0L
            val receiverBoostEndTime = receiverSnapshot.getLong("boostEndTime") ?: 0L
            if (receiverPausedMs > 0L && receiverBalance > 0.0) {
                val ratio = (receiverBalance + amount) / receiverBalance
                val newReceiverPausedMs = (receiverPausedMs * ratio).toLong()
                transaction.update(receiverRef, "pausedRemainingMs", newReceiverPausedMs)
            } else if (receiverBoostEndTime > now && receiverBalance > 0.0) {
                val ratio = (receiverBalance + amount) / receiverBalance
                val remainingMs = receiverBoostEndTime - now
                val newRemainingMs = (remainingMs * ratio).toLong()
                val newBoostEndTime = now + newRemainingMs
                transaction.update(receiverRef, "boostEndTime", newBoostEndTime)
            }

            val transactionData = mapOf(
                "sender" to user.uid,
                "senderName" to senderName,
                "receiver" to receiverUid,
                "receiverName" to receiverName,
                "amount" to amount,
                "status" to "completed",
                "idempotencyKey" to idempotencyKey,
                "timestamp" to FieldValue.serverTimestamp()
            )
            transaction.set(transRef, transactionData)

            // Collect emails
            val senderEmail = senderSnapshot.getString("email") ?: ""
            val receiverEmail = receiverSnapshot.getString("email") ?: ""
            
            mapOf(
                "senderEmail" to senderEmail,
                "senderName" to senderName,
                "receiverEmail" to receiverEmail,
                "receiverName" to receiverName,
                "amt" to amount,
                "senderNewBal" to (senderBalance - amount),
                "receiverNewBal" to (receiverBalance + amount)
            )
        }.addOnSuccessListener { result ->
            val data = result as? Map<*, *>
            if (data != null) {
                val senderEmail = data["senderEmail"] as? String
                val senderName = data["senderName"] as? String
                val receiverEmail = data["receiverEmail"] as? String
                val receiverName = data["receiverName"] as? String
                val amt = data["amt"] as? Double ?: 0.0
                val senderNewBal = data["senderNewBal"] as? Double ?: 0.0
                val receiverNewBal = data["receiverNewBal"] as? Double ?: 0.0

                // 1. Sent Money Email to Sender
                if (senderEmail != null && senderEmail.isNotEmpty()) {
                    triggerEmailNotification(
                        to = senderEmail,
                        subject = "Money Sent Successfully",
                        body = "Hello $senderName,\n\nYou have successfully sent $amt USD to $receiverName.\nYour remaining balance is $senderNewBal USD.\n\nThank you for choosing Wallet App!"
                    )
                }

                // 2. Received Money Email to Receiver
                if (receiverEmail != null && receiverEmail.isNotEmpty()) {
                    triggerEmailNotification(
                        to = receiverEmail,
                        subject = "Money Received Successfully",
                        body = "Hello $receiverName,\n\nYou have received $amt USD from $senderName.\nYour new balance is $receiverNewBal USD.\n\nThank you for choosing Wallet App!"
                    )
                }
            }
            onSuccess()
        }.addOnFailureListener { e ->
            android.util.Log.e("WalletViewModel", "sendMoney transaction failed", e)
            onError(e.message ?: "Transaction failed")
        }
    }

    fun updateDynamicBoostDuration() {
        val user = auth.currentUser ?: return
        
        // 1. Leaderboard Rank Check
        val lb = _leaderboard.value
        val rankIndex = lb.indexOfFirst { it.uid == user.uid }
        if (rankIndex == 0) {
            // #1 rank gets exactly 24 hours
            _currentMaxBoostDurationMs.value = 24 * 60 * 60 * 1000L
            return
        }
        
        // 2. Otherwise, dynamic balance scaling
        val currentBal = _balance.value
        val minTimeMs = 60 * 1000L // 1 minute
        val maxTimeMs = 24 * 60 * 60 * 1000L // 24 hours
        
        if (currentBal <= 0.0) {
            _currentMaxBoostDurationMs.value = minTimeMs
            return
        }
        
        // Reference maximum balance is the maximum balance on the leaderboard, or 100.0 as a floor
        val topUserBalance = lb.firstOrNull()?.balance ?: 100.0
        val referenceMax = topUserBalance.coerceAtLeast(100.0)
        
        // Scale: Fraction of balance relative to first place's total balance
        val fraction = (currentBal / referenceMax).coerceAtMost(1.0)
        val duration = minTimeMs + ((maxTimeMs - minTimeMs) * fraction).toLong()
        _currentMaxBoostDurationMs.value = duration.coerceIn(minTimeMs, maxTimeMs)
    }

    fun increaseBalance() {
        if (_isBoostDisabled.value) {
            _notifications.tryEmit("Boosting is temporarily disabled for maintenance.")
            return
        }
        if (_isSuspended.value) return
        if (_balance.value <= 0.0) {
            _notifications.tryEmit("You don't have sufficient balance to start boost")
            return
        }
        if (_isOnHold.value) {
            _notifications.tryEmit("Your account is on hold. Boosting is locked.")
            return
        }
        if (increaseJob?.isActive == true) return
        val user = auth.currentUser ?: return
        
        val duration = if (pausedRemainingMs > 0) {
            val d = pausedRemainingMs
            pausedRemainingMs = 0L
            d
        } else {
            // Calculate fresh duration on click
            updateDynamicBoostDuration()
            _currentMaxBoostDurationMs.value
        }
        
        val endTime = System.currentTimeMillis() + duration

        // Save boost end time to Firestore so it persists across restarts
        db.collection("users").document(user.uid).update(
            mapOf(
                "boostEndTime" to endTime,
                "pausedRemainingMs" to 0L
            )
        )
        
        resumeBoost(endTime)
    }

    private fun resumeBoost(endTime: Long) {
        increaseJob?.cancel()
        increaseJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            var localBalance = _balance.value
            var totalEarned = 0.0
            
            try {
                _isBoosting.value = true
                while (System.currentTimeMillis() < endTime) {
                    val remaining = ((endTime - System.currentTimeMillis()) / 1000).toInt()
                    _boostTimeRemaining.value = remaining.coerceAtLeast(0)
                    
                    // Calculate increment based on local tracked balance for accurate compounding
                    val multiplier = _globalBoostRatePercent.value / 100.0
                    val increment = localBalance * 0.0000002675 * multiplier
                    localBalance += increment
                    totalEarned += increment
                    
                    // Update Firestore using increment to avoid race conditions
                    incrementBalanceInFirestore(increment)
                    
                    delay(1000)
                }
            } finally {
                lastBoostActiveTime = System.currentTimeMillis()
                if (totalEarned > 0.0000001) { 
                    val durationSeconds = ((System.currentTimeMillis() - startTime) / 1000).toInt()
                    recordBoostTransaction(totalEarned, durationSeconds)
                    _boostEarnings.value = totalEarned
                    
                    if (System.currentTimeMillis() >= endTime) {
                        _showConfetti.tryEmit(Unit)
                        try {
                            com.example.MyFirebaseMessagingService.showNotification(
                                com.example.WalletApplication.instance,
                                "Boost Completed! 🚀",
                                "Your boost session completed successfully. You earned $${String.format("%.2f", totalEarned)}!"
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("WalletViewModel", "Failed to show boost notification", e)
                        }
                    }
                    
                    // Increment cumulative boosted amount for leaderboard
                    val user = auth.currentUser
                    if (user != null) {
                        val userRef = db.collection("users").document(user.uid)
                        userRef.update("totalBoosted", FieldValue.increment(totalEarned))
                        if (System.currentTimeMillis() >= endTime) {
                            userRef.update("boostEndTime", 0L)
                        }
                    }
                }
                
                if (System.currentTimeMillis() >= endTime) {
                    _notifications.tryEmit("Boost session expired")
                    _boostTimeRemaining.value = 0
                }
                _isBoosting.value = false
            }
        }
    }

    private fun recordBoostTransaction(amount: Double, durationSeconds: Int) {
        val user = auth.currentUser ?: return
        val transRef = db.collection("transactions").document()
        val transactionData = mapOf(
            "sender" to "BOOST_SYSTEM",
            "receiver" to user.uid,
            "amount" to amount,
            "durationSeconds" to durationSeconds,
            "timestamp" to FieldValue.serverTimestamp()
        )
        transRef.set(transactionData).addOnFailureListener {
            android.util.Log.e("WalletViewModel", "Failed to record boost transaction", it)
        }
    }

    fun addBalance(amount: Double) {
        incrementBalanceInFirestore(amount)
    }

    fun setBalance(newBalance: Double) {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid)
            .set(mapOf("balance" to newBalance), com.google.firebase.firestore.SetOptions.merge())
    }

    fun setBalanceForUser(uid: String, newBalance: Double) {
        if (uid.isEmpty()) return
        db.collection("users").document(uid)
            .set(mapOf("balance" to newBalance), com.google.firebase.firestore.SetOptions.merge())
    }

    fun clearBoostEarnings() {
        _boostEarnings.value = null
    }

    fun checkUsernameAvailable(username: String, onResult: (Boolean) -> Unit) {
        val q = username.trim().lowercase()
        if (q.isEmpty()) {
            onResult(true)
            return
        }
        db.collection("users")
            .whereEqualTo("username", q)
            .get()
            .addOnSuccessListener { snapshot ->
                onResult(snapshot == null || snapshot.isEmpty)
            }
            .addOnFailureListener { onResult(true) }
    }

    fun checkEmailAvailable(email: String, onResult: (Boolean) -> Unit) {
        val q = email.trim().lowercase()
        if (q.isEmpty()) {
            onResult(true)
            return
        }
        db.collection("users")
            .whereEqualTo("email", q)
            .get()
            .addOnSuccessListener { snapshot ->
                onResult(snapshot == null || snapshot.isEmpty)
            }
            .addOnFailureListener { onResult(true) }
    }

    fun checkPhoneAvailable(phone: String, onResult: (Boolean) -> Unit) {
        val q = phone.trim()
        if (q.isEmpty()) {
            onResult(true)
            return
        }
        db.collection("users")
            .whereEqualTo("phoneNumber", q)
            .get()
            .addOnSuccessListener { snapshot ->
                onResult(snapshot == null || snapshot.isEmpty)
            }
            .addOnFailureListener { onResult(true) }
    }

    fun checkEmailVerificationStatus() {
        val user = auth.currentUser
        user?.reload()?.addOnCompleteListener {
            _isEmailVerified.value = user.isEmailVerified
        }
    }

    fun sendVerificationEmail(onComplete: (Boolean, String?) -> Unit) {
        val user = auth.currentUser
        user?.sendEmailVerification()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onComplete(true, null)
            } else {
                onComplete(false, task.exception?.localizedMessage)
            }
        }
    }

    fun findUser(query: String, onResult: (String?, String?, String?) -> Unit) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            onResult(null, null, null)
            return
        }

        // Strip leading @ for username searches
        val q = if (trimmed.startsWith("@")) trimmed.substring(1) else trimmed
        if (q.isEmpty()) {
            onResult(null, null, null)
            return
        }

        val searchTerms = listOf(q, q.lowercase()).distinct()

        // 1. Try Email
        db.collection("users")
            .whereIn("email", searchTerms)
            .get()
            .addOnSuccessListener { emailSnapshot ->
                if (emailSnapshot != null && !emailSnapshot.isEmpty) {
                    val d = emailSnapshot.documents[0]
                    onResult(d.id, d.getString("name"), d.getString("email"))
                } else {
                    // 2. Try Username
                    db.collection("users")
                        .whereIn("username", searchTerms)
                        .get()
                        .addOnSuccessListener { userSnapshot ->
                            if (userSnapshot != null && !userSnapshot.isEmpty) {
                                val ud = userSnapshot.documents[0]
                                onResult(ud.id, ud.getString("name"), ud.getString("email"))
                            } else {
                                // 3. Try Phone Number
                                db.collection("users")
                                    .whereEqualTo("phoneNumber", q)
                                    .get()
                                    .addOnSuccessListener { phoneSnapshot ->
                                        if (phoneSnapshot != null && !phoneSnapshot.isEmpty) {
                                            val pd = phoneSnapshot.documents[0]
                                            onResult(pd.id, pd.getString("name"), pd.getString("email"))
                                        } else {
                                            onResult(null, null, null)
                                        }
                                    }.addOnFailureListener { e ->
                                        android.util.Log.e("WalletViewModel", "findUser phone query failed", e)
                                        if (e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true) {
                                            _notifications.tryEmit("Permission Denied: Please update Firestore Rules")
                                        }
                                        onResult(null, null, null)
                                    }
                            }
                        }.addOnFailureListener { e ->
                            android.util.Log.e("WalletViewModel", "findUser username query failed", e)
                            if (e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true) {
                                _notifications.tryEmit("Permission Denied: Please update Firestore Rules")
                            }
                            onResult(null, null, null)
                        }
                }
            }.addOnFailureListener { e ->
                android.util.Log.e("WalletViewModel", "findUser email query failed", e)
                if (e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true) {
                    _notifications.tryEmit("Permission Denied: Please update Firestore Rules")
                }
                onResult(null, null, null)
            }
    }

    fun updateUserProfile(uid: String, name: String, email: String, username: String = "", phoneNumber: String = "") {
        val profileData = mutableMapOf(
            "name" to name,
            "email" to email,
            "username" to username,
            "phoneNumber" to phoneNumber,
            "uid" to uid,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (email == "admin@gmail.com" || email == "royalcrmagent@gmail.com") {
            profileData["isAdmin"] = true
        }
        db.collection("users").document(uid)
            .set(profileData, com.google.firebase.firestore.SetOptions.merge())
            .addOnFailureListener {
                android.util.Log.e("WalletViewModel", "Failed to update profile: ${it.message}")
            }
    }

    fun updateUserProfileAndCredentials(
        name: String,
        email: String,
        username: String,
        phoneNumber: String,
        passwordToUpdate: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = auth.currentUser
        if (user == null) {
            onError("User not authenticated")
            return
        }

        val emailChanged = email.trim().lowercase() != (user.email ?: "").trim().lowercase()
        val passwordChanged = passwordToUpdate.isNotEmpty()

        val updateFirestoreAndFinish = {
            val profileData = mutableMapOf(
                "name" to name,
                "email" to email.trim().lowercase(),
                "username" to username.trim(),
                "phoneNumber" to phoneNumber.trim(),
                "uid" to user.uid,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (email.trim().lowercase() == "admin@gmail.com" || email.trim().lowercase() == "royalcrmagent@gmail.com") {
                profileData["isAdmin"] = true
            }
            db.collection("users").document(user.uid)
                .set(profileData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener {
                    // Force refresh internal view model state
                    refreshUserData()
                    onSuccess()
                }
                .addOnFailureListener {
                    android.util.Log.e("WalletViewModel", "Failed to update profile: ${it.message}")
                    onError("Failed to update profile in database: ${it.localizedMessage}")
                }
        }

        if (emailChanged || passwordChanged) {
            if (emailChanged && passwordChanged) {
                user.updateEmail(email.trim().lowercase()).addOnSuccessListener {
                    user.updatePassword(passwordToUpdate).addOnSuccessListener {
                        updateFirestoreAndFinish()
                    }.addOnFailureListener {
                        onError("Email updated, but password update failed: ${it.localizedMessage}")
                    }
                }.addOnFailureListener {
                    onError("Email update failed: ${it.localizedMessage}")
                }
            } else if (emailChanged) {
                user.updateEmail(email.trim().lowercase()).addOnSuccessListener {
                    updateFirestoreAndFinish()
                }.addOnFailureListener {
                    onError("Email update failed: ${it.localizedMessage}")
                }
            } else {
                user.updatePassword(passwordToUpdate).addOnSuccessListener {
                    updateFirestoreAndFinish()
                }.addOnFailureListener {
                    onError("Password update failed: ${it.localizedMessage}")
                }
            }
        } else {
            updateFirestoreAndFinish()
        }
    }

    fun updateUserAvatar(url: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid)
            .update("avatarUrl", url)
            .addOnSuccessListener {
                _userAvatarUrl.value = url
                onSuccess()
            }
            .addOnFailureListener {
                onError(it.localizedMessage ?: "Failed to update avatar")
            }
    }

    fun uploadAvatarImage(uri: android.net.Uri, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val user = auth.currentUser ?: run { onError("Not authenticated"); return }
        val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference
            .child("avatars/${user.uid}.jpg")
        
        storageRef.putFile(uri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    val url = downloadUri.toString()
                    updateUserAvatar(url, { onSuccess(url) }, onError)
                }.addOnFailureListener {
                    onError("Failed to get download URL: ${it.localizedMessage}")
                }
            }
            .addOnFailureListener {
                onError("Failed to upload image: ${it.localizedMessage}")
            }
    }

    private fun triggerEmailNotification(to: String, subject: String, body: String) {
        val mailData = mapOf(
            "to" to to,
            "message" to mapOf(
                "subject" to subject,
                "text" to body,
                "html" to "<div style='font-family: sans-serif; padding: 20px; border: 1px solid #eee; border-radius: 10px;'>" +
                        "<h2 style='color: #4CAF50;'>Transaction Successful!</h2>" +
                        "<p>${body.replace("\n", "<br/>")}</p>" +
                        "<hr/>" +
                        "<p style='font-size: 12px; color: #888;'>This is an automated message from your Wallet Application.</p>" +
                        "</div>"
            )
        )
        db.collection("mail").add(mailData)
            .addOnFailureListener {
                android.util.Log.e("WalletViewModel", "Failed to trigger email: ${it.message}")
            }
    }

    private fun incrementBalanceInFirestore(amount: Double) {
        val user = auth.currentUser ?: return
        val updates = mapOf(
            "balance" to FieldValue.increment(amount)
        )
        db.collection("users").document(user.uid)
            .set(updates, com.google.firebase.firestore.SetOptions.merge())
            .addOnFailureListener {
                android.util.Log.e("WalletViewModel", "Failed to increment balance: ${it.message}")
            }
    }

    private fun setupTransactionsListener(userId: String, isAdminUser: Boolean) {
        transactionsListener?.remove()
        
        val query = if (isAdminUser) {
            db.collection("transactions")
        } else {
            db.collection("transactions")
                .where(
                    Filter.or(
                        Filter.equalTo("sender", userId),
                        Filter.equalTo("receiver", userId)
                    )
                )
        }
        
        transactionsListener = query.addSnapshotListener { snapshot, e ->
            if (e != null) {
                android.util.Log.e("WalletViewModel", "Error fetching transactions", e)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.toObjects(Transaction::class.java)
                    .sortedByDescending { it.timestamp?.toDate()?.time ?: 0L }
                _transactions.value = list
            }
        }
    }

    fun sendChatMessage(messageText: String) {
        val user = auth.currentUser ?: return
        if (messageText.isBlank()) return

        val chatRef = rtdb.getReference("chat").push()
        val msgId = chatRef.key ?: ""
        val chatMsg = ChatMessage(
            id = msgId,
            uid = user.uid,
            name = _userName.value.ifBlank { "User" },
            message = messageText.trim(),
            timestamp = System.currentTimeMillis()
        )
        chatRef.setValue(chatMsg)
    }

    fun updateLiveAnnouncement(text: String) {
        if (!_isAdmin.value) return
        rtdb.getReference("announcement").setValue(text.trim())
    }

    private fun formatAmountWithDynamicPrecision(amount: Double): String {
        val absAmount = kotlin.math.abs(amount)
        return when {
            absAmount == 0.0 -> "0.00"
            absAmount < 0.01 -> String.format("%.6f", absAmount).trimEnd('0').trimEnd('.')
            absAmount < 1.0 -> String.format("%.4f", absAmount).trimEnd('0').trimEnd('.')
            else -> String.format("%.2f", absAmount)
        }
    }

    private fun attachSnapshotListener(walletRef: DocumentReference, userEmail: String) {
        val user = auth.currentUser ?: return
        
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                walletRef.update("fcmToken", token)
            }

        walletListener = walletRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                android.util.Log.e("WalletViewModel", "Listen failed.", e)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                _isEmailVerified.value = auth.currentUser?.isEmailVerified ?: false
                val newBalance = snapshot.getDouble("balance")
                val oldBalance = _balance.value
                
                if (newBalance == null) {
                    // Doc exists but no balance, initialize fields
                    val updates = mutableMapOf<String, Any>(
                        "balance" to 0.0,
                        "totalBoosted" to 0.0,
                        "boostEndTime" to 0L,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                    if (userEmail == "admin@gmail.com" || userEmail == "royalcrmagent@gmail.com") {
                        updates["isAdmin"] = true
                    }
                    walletRef.update(updates).addOnFailureListener {
                        android.util.Log.e("WalletViewModel", "Failed to init missing fields", it)
                    }
                }

                _balance.value = newBalance ?: 0.0
                _totalBoosted.value = snapshot.getDouble("totalBoosted") ?: 0.0
                updateDynamicBoostDuration()

                val currentName = snapshot.getString("name")
                val currentEmail = snapshot.getString("email")
                val currentUsername = snapshot.getString("username")
                val currentPhone = snapshot.getString("phoneNumber")
                val isAdminInFirestore = snapshot.getBoolean("isAdmin") ?: false
                
                val emailToUse = currentEmail ?: userEmail
                val isUserAdmin = emailToUse == "admin@gmail.com" || emailToUse == "royalcrmagent@gmail.com"
                
                val profileUpdates = mutableMapOf<String, Any>()
                
                if (currentName.isNullOrBlank()) {
                    val derivedName = if (emailToUse.startsWith("guest_")) {
                        val shortId = emailToUse.substringAfter("guest_").substringBefore("@").take(6)
                        "Guest ${shortId.uppercase()}"
                    } else {
                        emailToUse.substringBefore("@").ifBlank { "Guest User" }
                    }
                    profileUpdates["name"] = derivedName
                    _userName.value = derivedName
                } else {
                    _userName.value = currentName
                }
                
                if (currentEmail.isNullOrBlank() && emailToUse.isNotEmpty()) {
                    profileUpdates["email"] = emailToUse
                    _userEmail.value = emailToUse
                } else {
                    _userEmail.value = emailToUse
                }
                
                if (currentUsername.isNullOrBlank()) {
                    val derivedUsername = if (emailToUse.startsWith("guest_")) {
                        "guest_" + emailToUse.substringAfter("guest_").substringBefore("@").take(6)
                    } else {
                        emailToUse.substringBefore("@").ifBlank { "user_" + user.uid.take(6) }
                    }
                    profileUpdates["username"] = derivedUsername
                    _userUsername.value = derivedUsername
                } else {
                    _userUsername.value = currentUsername
                }
                
                if (currentPhone.isNullOrBlank()) {
                    val derivedPhone = "+8801" + (100000000..999999999).random().toString()
                    profileUpdates["phoneNumber"] = derivedPhone
                    _userPhone.value = derivedPhone
                } else {
                    _userPhone.value = currentPhone
                }
                
                if (isUserAdmin && !isAdminInFirestore) {
                    profileUpdates["isAdmin"] = true
                }
                
                if (profileUpdates.isNotEmpty()) {
                    walletRef.update(profileUpdates).addOnFailureListener {
                        android.util.Log.e("WalletViewModel", "Failed to update profile updates", it)
                    }
                }
                
                val finalAdminValue = isAdminInFirestore || isUserAdmin
                if (_isAdmin.value != finalAdminValue || transactionsListener == null) {
                    _isAdmin.value = finalAdminValue
                    setupTransactionsListener(user.uid, finalAdminValue)
                    if (finalAdminValue) {
                        setupAdminListeners()
                    }
                }
                _isSuspended.value = snapshot.getBoolean("isSuspended") ?: false
                _isOnHold.value = snapshot.getBoolean("isOnHold") ?: false
                _userAvatarUrl.value = snapshot.getString("avatarUrl") ?: ""
                
                // Persistent Boost Check
                val boostEnd = snapshot.getLong("boostEndTime") ?: 0L
                val pausedMs = snapshot.getLong("pausedRemainingMs") ?: 0L
                
                // Only update local pausedRemainingMs if we aren't currently boosting.
                if (_isBoosting.value == false) {
                    pausedRemainingMs = pausedMs
                    _boostTimeRemaining.value = (pausedMs / 1000).toInt()
                }

                if (boostEnd > System.currentTimeMillis() && _isBoosting.value == false && pausedMs == 0L && pausedRemainingMs == 0L) {
                    if (_isBoostDisabled.value == false) {
                        resumeBoost(boostEnd)
                    }
                } else if (_isBoosting.value == true && (
                    (boostEnd > 0 && boostEnd <= System.currentTimeMillis()) || 
                    (boostEnd == 0L && pausedMs == 0L)
                )) {
                    increaseJob?.cancel()
                    increaseJob = null
                    _isBoosting.value = false
                    _boostTimeRemaining.value = 0
                }

                // Single Device Session Check
                val remoteDeviceId = snapshot.getString("deviceId")
                if (remoteDeviceId != null && _currentDeviceId.value.isNotEmpty() && remoteDeviceId != _currentDeviceId.value) {
                    viewModelScope.launch {
                        _notifications.emit("You have been logged out because of a login on another device.")
                        resetState()
                        auth.signOut()
                    }
                }
                
                calculateUserRank()
            } else {
                // Create initial document for new users with a starting balance
                val emailToUse = userEmail
                val derivedName = if (emailToUse.startsWith("guest_")) {
                    val shortId = emailToUse.substringAfter("guest_").substringBefore("@").take(6)
                    "Guest ${shortId.uppercase()}"
                } else {
                    emailToUse.substringBefore("@").ifBlank { "Guest User" }
                }
                val derivedUsername = if (emailToUse.startsWith("guest_")) {
                    "guest_" + emailToUse.substringAfter("guest_").substringBefore("@").take(6)
                } else {
                    emailToUse.substringBefore("@").ifBlank { "user_" + user.uid.take(6) }
                }
                val derivedPhone = "+8801" + (100000000..999999999).random().toString()

                val initialData = mutableMapOf<String, Any>(
                    "uid" to user.uid,
                    "name" to derivedName,
                    "email" to emailToUse,
                    "username" to derivedUsername,
                    "phoneNumber" to derivedPhone,
                    "balance" to 0.0,
                    "totalBoosted" to 0.0,
                    "boostEndTime" to 0L,
                    "boostDisabled" to false,
                    "deviceId" to _currentDeviceId.value,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                if (emailToUse == "admin@gmail.com" || emailToUse == "royalcrmagent@gmail.com") {
                    initialData["isAdmin"] = true
                }
                walletRef.set(initialData).addOnFailureListener {
                    android.util.Log.e("WalletViewModel", "Failed to create user doc", it)
                }
            }
        }
    }

    private fun clearGlobalListeners() {
        systemBoostListener?.remove()
        settingsListener?.remove()
        platformBalanceListener?.remove()
        leaderboardListener?.remove()
        adminUsersListener?.remove()
        adminTransactionsListener?.remove()
    }

    private fun setupGlobalListeners() {
        clearGlobalListeners()

        // Listen for ALL boost transactions to calculate system total
        systemBoostListener = db.collection("transactions")
            .whereEqualTo("sender", "BOOST_SYSTEM")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                val total = snapshot?.documents?.sumOf { it.getDouble("amount") ?: 0.0 } ?: 0.0
                _totalSystemBoost.value = total
            }

        // Listen for Global Settings (Maintenance Mode)
        settingsListener = db.collection("settings").document("global")
            .addSnapshotListener { snapshot, _ ->
                val disabled = snapshot?.getBoolean("boostDisabled") ?: false
                _isBoostDisabled.value = disabled
                _globalBoostRatePercent.value = snapshot?.getDouble("globalBoostRatePercent") ?: 100.0
                
                if (disabled && _isBoosting.value) {
                    stopBoost()
                    _notifications.tryEmit("Boosting paused due to system maintenance.")
                }
            }

        // Listen for Total Platform Balance (Sum of all user balances)
        platformBalanceListener = db.collection("users")
            .addSnapshotListener { snapshot, e ->
                if (e == null && snapshot != null) {
                    val total = snapshot.documents.sumOf { it.getDouble("balance") ?: 0.0 }
                    _totalPlatformBalance.value = total
                }
            }

        // Listen for Leaderboard
        leaderboardListener = db.collection("users")
            .orderBy("balance", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents?.map { doc ->
                    UserLeaderboard(
                        uid = doc.id,
                        name = doc.getString("name") ?: "Unknown User",
                        balance = doc.getDouble("balance") ?: 0.0,
                        totalBoosted = doc.getDouble("totalBoosted") ?: 0.0
                    )
                } ?: emptyList()
                _leaderboard.value = list
                updateDynamicBoostDuration()
            }
            
        if (_isAdmin.value) {
            setupAdminListeners()
        }
    }

    private fun setupAdminListeners() {
        adminUsersListener = db.collection("users").addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
            if (snapshot != null) {
                val users = snapshot.documents.map { doc ->
                    val data = doc.data?.toMutableMap() ?: mutableMapOf()
                    data["uid"] = doc.id
                    
                    val name = data["name"] as? String
                    if (name.isNullOrBlank()) {
                        val email = data["email"] as? String ?: ""
                        if (email.startsWith("guest_")) {
                            val shortId = email.substringAfter("guest_").substringBefore("@")
                            data["name"] = "Guest ${shortId.take(6).uppercase()}"
                        } else {
                            data["name"] = "Guest User"
                        }
                    }
                    
                    if ((data["email"] as? String).isNullOrBlank()) {
                        data["email"] = "user_${doc.id.take(8)}@wallet.app"
                    }
                    data
                }
                _allUsers.value = users
            }
        }
        
        adminTransactionsListener = db.collection("transactions").addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener
            if (snapshot != null) {
                var volume = 0.0
                for (doc in snapshot.documents) {
                    volume += doc.getDouble("amount") ?: 0.0
                }
                _platformVolume.value = volume
            }
        }
    }
}
