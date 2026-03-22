package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun MainMenu(navController: NavController) {
    val backgroundGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF4C1D95), Color(0xFF6B21A8), Color(0xFF9333EA))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.TrendingUp,
            contentDescription = "Trending",
            tint = Color.White,
            modifier = Modifier.size(80.dp).padding(bottom = 16.dp)
        )

        Text(
            text = "VAULT",
            color = Color.White,
            fontSize = 48.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Text(
            text = "Play big, win big!",
            color = Color(0xFFE9D5FF),
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 48.dp)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GradientButton(
                text = "EARN MONEY",
                icon = Icons.Filled.MonetizationOn,
                colors = listOf(Color(0xFF22C55E), Color(0xFF059669)),
                onClick = { navController.navigate("clicker") }
            )

            GradientButton(
                text = "PLAY OFFLINE",
                icon = Icons.Filled.TrendingUp,
                colors = listOf(Color(0xFFA855F7), Color(0xFFDB2777)),
                onClick = { navController.navigate("game") }
            )

            GradientButton(
                text = "HOST LOBBY",
                icon = Icons.Filled.Group,
                colors = listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)),
                onClick = { navController.navigate("host") }
            )

            GradientButton(
                text = "JOIN LOBBY",
                icon = Icons.Filled.Group,
                colors = listOf(Color(0xFF6366F1), Color(0xFF7E22CE)),
                onClick = { navController.navigate("join") }
            )
        }

        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = "v1.0.0",
            color = Color(0xFFD8B4FE),
            fontSize = 14.sp
        )
    }
}

@Composable
fun GradientButton(
    text: String,
    icon: ImageVector,
    colors: List<Color>,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(colors))
            .clickable { onClick() }
            .padding(vertical = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp).padding(end = 12.dp)
        )
        Text(
            text = text,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}