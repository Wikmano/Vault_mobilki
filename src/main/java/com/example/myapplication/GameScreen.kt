package com.example.myapplication

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlin.random.Random
import kotlin.math.round

@Composable
fun GameScreen(navController: NavController) {
    val seed = 123456
    val points = 200
    val volatility = 200f

    val chart = remember(seed) {
        val random = Random(seed)
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

    val progress by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 10000, easing = LinearEasing),
        label = ""
    )

    LaunchedEffect(Unit) {
        animationTriggered = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
        ) {
            val screenWidth = size.width
            val screenHeight = size.height
            val spacing = screenWidth / points
            val currentPoint = progress * (points - 1)

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
                .fillMaxWidth(0.2f)
                .align(Alignment.CenterEnd)
                .background(Color(0xFF2D0B5A))
                .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            repeat(7) { index ->
                Button(
                    onClick = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.15f),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "${round(5 * (1 - progress) * 10) / 10f}X",
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
        }

        Text(
            text = "${round((10 - 10 * progress) * 10) / 10f}s",
            color = Color.White,
            fontSize = 24.sp,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .padding(top = 24.dp)
                .align(Alignment.TopCenter)
        )
    }
}