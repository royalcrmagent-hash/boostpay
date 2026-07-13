package com.example.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.models.CurrencyData
import com.example.domain.models.UserLeaderboard
import com.example.presentation.utils.getUserStatus

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
