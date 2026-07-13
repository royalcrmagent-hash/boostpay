package com.example.presentation.components

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
