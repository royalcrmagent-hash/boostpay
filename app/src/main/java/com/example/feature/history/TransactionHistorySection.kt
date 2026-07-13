package com.example.feature.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.models.CurrencyData
import com.example.domain.models.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                val absAmount = Math.abs(convertedAmount)
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
                    color = MaterialTheme.colorScheme.secondary
                )

                Surface(
                    color = Color(0xFFE8F5E9),
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
