package com.example.feature.home

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.WalletViewModel
import com.example.domain.models.CurrencyData
import com.example.core.security.showBiometricPrompt
import com.example.feature.wallet.WalletCard
import com.example.feature.deposit.ReceiveMoneyDialog
import com.example.feature.withdraw.SendMoneyDialog
import com.example.feature.profile.EditProfileDialog
import com.example.feature.history.TransactionItem
import com.example.feature.history.BoostSessionItem
import com.example.presentation.components.FloatingCurvedNavBar
import com.example.presentation.components.BoostBalanceButton
import com.example.presentation.components.ActiveBoostProgressCard
import com.example.presentation.components.ConfettiEffect
import com.example.presentation.components.LeaderboardDialog
import com.example.presentation.components.BoostSummaryDialog
import com.example.ui.screens.AdminDashboardScreen
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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

                    item {
                        WalletCard(
                            balance = balance,
                            convertedBalance = convertedBalance,
                            userName = userName,
                            userUsername = userUsername,
                            userEmail = userEmail,
                            currentUserUid = currentUserUid,
                            userRank = userRank,
                            selectedCurrency = selectedCurrency,
                            availableCurrencies = availableCurrencies,
                            exchangeRates = exchangeRates,
                            onCurrencySelected = { viewModel.setCurrency(it) }
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
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
                                        onError = { /* Do nothing */ }
                                    )
                                },
                                onStopClick = { viewModel.stopBoost() }
                            )
                        }
                    }

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
                                            tint = MaterialTheme.colorScheme.tertiary,
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
                                    val percentage = if (isRank1) {
                                        1.0f
                                    } else {
                                        (currentMaxBoostDurationMs.toFloat() / (24 * 60 * 60 * 1000L).toFloat()).coerceIn(0.001f, 1.0f)
                                    }

                                    LinearProgressIndicator(
                                        progress = { percentage },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = MaterialTheme.colorScheme.tertiary,
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

                        Text(
                            "Transactions",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

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

                        var typeDropdownExpanded by remember { mutableStateOf(false) }
                        var showDateRangeFilters by remember { mutableStateOf(false) }
                        val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

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
                                    OutlinedButton(
                                        onClick = {
                                            val cal = Calendar.getInstance()
                                            txStartDate?.let { cal.timeInMillis = it }
                                            DatePickerDialog(
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
                                            text = txStartDate?.let { dateFormatter.format(Date(it)) } ?: "Start Date",
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val cal = Calendar.getInstance()
                                            txEndDate?.let { cal.timeInMillis = it }
                                            DatePickerDialog(
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
                                            text = txEndDate?.let { dateFormatter.format(Date(it)) } ?: "End Date",
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

        // Top-aligned SnackbarHost right below top bar (elevation = 100 to draw above lists)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
                .zIndex(100f)
        )

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
