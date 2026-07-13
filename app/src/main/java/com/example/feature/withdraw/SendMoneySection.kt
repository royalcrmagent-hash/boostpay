package com.example.feature.withdraw

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.WalletViewModel
import com.example.domain.models.CurrencyData
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
