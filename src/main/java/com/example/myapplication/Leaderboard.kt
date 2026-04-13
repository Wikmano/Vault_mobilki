package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.*

@Composable
fun LeaderboardScreen(
    scores: List<ScoreEntry>,
    onBackToMenu: () -> Unit
) {
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF4C1D95), Color(0xFF1E1B4B))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        Icon(
            imageVector = Icons.Default.EmojiEvents,
            contentDescription = "Winner",
            tint = GoldAccent,
            modifier = Modifier.size(80.dp)
        )
        
        Text(
            text = "LEADERBOARD",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(scores) { index, entry ->
                LeaderboardItem(index + 1, entry)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onBackToMenu,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "BACK TO MENU",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
fun LeaderboardItem(rank: Int, entry: ScoreEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (rank == 1) GoldAccent else Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = rank.toString(),
                color = if (rank == 1) TextDark else Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = entry.name,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "$${entry.balance}",
            color = if (rank == 1) Color.Green else Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
