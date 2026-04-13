package com.example.myapplication

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.math.round

@Composable
fun GameScreen(
    navController: NavController,
    coins: Int,
    onCoinsChange: (Int) -> Unit,
    onFinished: (Int) -> Unit = {},
    loops: Int = 3,
    blind: Int = 10,
    volatility: Float = 60f,
    seed: Int = 44444
) {
    val context = LocalContext.current
    var betAmount by remember { mutableIntStateOf(minOf(blind, coins)) }
    val selectedMultipliers = remember { mutableStateListOf<Int>() }
    val buttonBounds = remember { mutableStateMapOf<Int, Rect>() }
    var isStopped by remember { mutableStateOf(false) }
    var frozenMultiplier by remember { mutableStateOf<Float?>(null) }
    var collisionIndex by remember { mutableStateOf<Int?>(null) }
    val progress = remember { Animatable(0f) }
    var boxSize by remember { mutableStateOf(Size.Zero) }
    var boxCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    
    var currentLoop by remember { mutableIntStateOf(1) }

    DisposableEffect(Unit) {
        val activity = context as? Activity
        val window = activity?.window
        if (window != null) {
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        }
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        
        onDispose {
            activity?.requestedOrientation = originalOrientation
            if (window != null) {
                val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
                windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    val points = 500

    val chart = remember(seed, currentLoop, volatility) {
        val random = Random(seed + currentLoop * 777)
        val data = MutableList(points) { 0f }
        var currentValue = 0f
        for (i in 0 until points) {
            val change = (random.nextFloat() * 2f - 1f) * volatility
            currentValue += change
            currentValue = currentValue.coerceIn(-400f, 400f)
            data[i] = currentValue
        }
        data
    }

    val backgroundGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF4C1D95), Color(0xFF6B21A8), Color(0xFF9333EA))
    )

    var animationTriggered by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        animationTriggered = true
    }

    LaunchedEffect(animationTriggered, isStopped, currentLoop) {
        if (animationTriggered && !isStopped) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 10000, easing = LinearEasing)
            )
        } else if (isStopped) {
            progress.stop()
        }
    }

    LaunchedEffect(isStopped) {
        if (isStopped) {
            delay(3000)
            if (currentLoop < loops) {
                collisionIndex = null
                frozenMultiplier = null
                selectedMultipliers.clear()
                progress.snapTo(0f)
                isStopped = false
                currentLoop++
                // Reset bet amount for next loop if needed
                betAmount = minOf(blind, coins)
            } else {
                onFinished(coins)
            }
        }
    }

    LaunchedEffect(progress.value) {
        if (!isStopped && boxSize.width > 0) {
            val screenWidth = boxSize.width
            val screenHeight = boxSize.height
            val spacing = screenWidth / points
            val cp = progress.value * (points - 1)
            val i = cp.toInt().coerceIn(0, points - 2)
            val fraction = cp - i

            val startX = i * spacing
            val startY = screenHeight / 2 + chart[i]
            val targetEndX = (i + 1) * spacing
            val targetEndY = screenHeight / 2 + chart[i + 1]

            val endX = startX + (targetEndX - startX) * fraction
            val endY = startY + (targetEndY - startY) * fraction
            val tip = Offset(endX, endY)

            for ((index, rect) in buttonBounds) {
                if (rect.contains(tip)) {
                    collisionIndex = index
                    isStopped = true
                    progress.stop()
                    
                    if (selectedMultipliers.isNotEmpty() && selectedMultipliers[0] == collisionIndex) {
                        val actualBet = minOf(betAmount, coins + (if (selectedMultipliers.isNotEmpty()) betAmount else 0)) 
                        // Note: coins already had betAmount subtracted when button was clicked
                        onCoinsChange(coins + (betAmount * (frozenMultiplier ?: 0f)).toInt())
                    }
                    break
                }
            }
            
            if (progress.value >= 1f) {
                isStopped = true
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                boxSize = coords.size.toSize()
                boxCoordinates = coords
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
        ) {
            val screenWidth = size.width
            val screenHeight = size.height
            val spacing = screenWidth / points
            val currentPoint = progress.value * (points - 1)

            for (i in 0 until points - 1) {
                val fraction = currentPoint - i
                val startX = i * spacing
                val startY = screenHeight / 2 + chart[i]

                val targetEndX = (i + 1) * spacing
                val targetEndY = screenHeight / 2 + chart[i + 1]

                val endX = startX + (targetEndX - startX) * fraction
                val endY = startY + (targetEndY - startY) * fraction

                if (i < currentPoint.toInt()) {
                    drawLine(
                        color = Color.White,
                        start = Offset(x = startX, y = startY),
                        end = Offset(targetEndX, y = targetEndY),
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )
                } else if (i == currentPoint.toInt()) {
                    drawLine(
                        color = Color.White,
                        start = Offset(x = startX, y = startY),
                        end = Offset(endX, y = endY),
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.15f)
                .align(Alignment.CenterEnd)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            repeat(7) { index ->
                val isSelected = selectedMultipliers.contains(index)
                val currentMultiplier = frozenMultiplier ?: (round(5 * (1 - progress.value) * 10) / 10f)
                
                val buttonColor = when {
                    !isStopped -> {
                        if (isSelected) Color(0xFF4C1D95)
                        else Color.White.copy(alpha = 0.15f)
                    }
                    else -> {
                        if (isSelected) {
                            if (index == collisionIndex) Color(0xFF22C55E)
                            else Color(0xFFEF4444)
                        } else {
                            if (index == collisionIndex) Color(0xFF22C55E).copy(alpha = 0.5f)
                            else Color.White.copy(alpha = 0.05f)
                        }
                    }
                }

                Button(
                    onClick = {
                        if (!isStopped && selectedMultipliers.isEmpty() && coins > 0) {
                            val actualBet = minOf(betAmount, coins)
                            val mult = round(5 * (1 - progress.value) * 10) / 10f
                            frozenMultiplier = mult
                            selectedMultipliers.add(index)
                            // We use the actual bet (which can be less than betAmount if coins < blind)
                            onCoinsChange(coins - actualBet)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .onGloballyPositioned { coords ->
                            boxCoordinates?.let { parent ->
                                buttonBounds[index] = parent.localBoundingBoxOf(coords)
                            }
                        },
                    enabled = !isStopped && selectedMultipliers.isEmpty() && (coins > 0 || selectedMultipliers.isNotEmpty()),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonColor,
                        contentColor = Color.White,
                        disabledContainerColor = buttonColor,
                        disabledContentColor = Color.White
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (isSelected) "X$frozenMultiplier" else "${currentMultiplier}X",
                        fontSize = 12.sp,
                        maxLines = 1,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.MonetizationOn,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$coins",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = { if (betAmount > blind) betAmount -= blind },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                    enabled = !isStopped && selectedMultipliers.isEmpty() && betAmount > blind
                ) {
                    Text("-", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val actualBet = if (selectedMultipliers.isEmpty()) minOf(betAmount, coins) else betAmount
                    Text(
                        text = if (actualBet < blind && actualBet > 0 && selectedMultipliers.isEmpty()) "ALL-IN" else "BET",
                        color = Color(0xFFE9D5FF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$actualBet",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = { 
                        if (betAmount < blind) betAmount = blind 
                        else betAmount += blind 
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                    enabled = !isStopped && selectedMultipliers.isEmpty()
                ) {
                    Text("+", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Text(
            text = if (isStopped) "0.0" else "${(round((10 - 10 * progress.value) * 10) / 10f).coerceAtLeast(0f)}",
            color = Color.White,
            fontSize = 24.sp,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .padding(top = 24.dp)
                .align(Alignment.TopCenter)
        )
    }
}
