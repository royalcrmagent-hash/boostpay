package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.CurrencyData
import com.example.Transaction
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

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
    val maxDurationSeconds = (maxDurationMs / 1000L).coerceAtLeast(1L).toFloat()
    val progressFraction = (timeRemainingSeconds.toFloat() / maxDurationSeconds).coerceIn(0f, 1f)
    
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
fun NavBarItem(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "nav_bg"
    )
    val contentColor by animateColorAsState(
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
