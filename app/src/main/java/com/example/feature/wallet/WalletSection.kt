package com.example.feature.wallet

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.models.CurrencyData
import com.example.domain.models.UserLeaderboard
import com.example.presentation.utils.getUserStatus
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletCard(
    balance: Double,
    convertedBalance: Double,
    userName: String,
    userUsername: String,
    userEmail: String,
    currentUserUid: String,
    userRank: Int?,
    selectedCurrency: CurrencyData,
    availableCurrencies: List<CurrencyData>,
    exchangeRates: Map<String, Double>,
    onCurrencySelected: (CurrencyData) -> Unit
) {
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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

                val lastFour = currentUserUid.takeLast(4).uppercase(Locale.ROOT).ifEmpty { "8824" }
                val cardNumber = "5412  8734  9012  $lastFour"
                Text(
                    text = cardNumber,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.5.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                Text(
                    text = "AVAILABLE BALANCE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f)
                )

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
                            fontFamily = FontFamily.Monospace
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
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

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

                    val clipboardManager = LocalClipboardManager.current
                    val context = LocalContext.current

                    Surface(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(currentUserUid))
                            Toast.makeText(context, "UID copied!", Toast.LENGTH_SHORT).show()
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
                        searchQuery = "" 
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
                        .width(280.dp)
                        .heightIn(max = 400.dp)
                ) {
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
                                onCurrencySelected(currency)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
