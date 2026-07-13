package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import com.example.WalletViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    bringIntoViewRequester: BringIntoViewRequester? = null,
    coroutineScope: CoroutineScope? = null,
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
