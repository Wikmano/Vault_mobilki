package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun ClickerScreen(
    navController: NavController,
    monety: Int,
    onGetGold: () -> Unit
) {
    val backgroundGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF4C1D95), Color(0xFF6B21A8), Color(0xFF9333EA))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.MonetizationOn,
            contentDescription = "Moneta",
            tint = Color(0xFFFFD700),
            modifier = Modifier
                .size(100.dp)
                .padding(bottom = 16.dp)
        )

        Text(
            text = "Money: $monety",
            color = Color.White,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 48.dp)
        )

        Button(
            onClick = onGetGold,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(60.dp)
                .padding(bottom = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E))
        ) {
            Text(
                text = "Click me!!",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { navController.popBackStack() },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
        ) {
            Text(
                text = "Wróć do Menu",
                color = Color.White,
                fontSize = 16.sp
            )
        }
    }
}