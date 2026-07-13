package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.CurrencyData
import com.example.UserLeaderboard
import com.example.WalletViewModel
import com.example.ui.utils.getUserStatus
import kotlinx.coroutines.delay

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
