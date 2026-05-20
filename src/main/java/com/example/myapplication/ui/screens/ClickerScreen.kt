package com.example.myapplication.ui.screens

import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.myapplication.R

@Composable
fun ClickerScreen(
    navController: NavController,
    coins: Int,
    onGetGold: () -> Unit
) {
    val context = LocalContext.current
    val backgroundGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF4C1D95), Color(0xFF6B21A8), Color(0xFF9333EA))
    )

    // Specify the type explicitly to resolve the "Cannot infer type" error.
    val marioClickPlayer = remember<MediaPlayer?> { MediaPlayer.create(context, R.raw.mario_click) }

    DisposableEffect(Unit) {
        onDispose {
            marioClickPlayer?.release()
        }
    }

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
                .clickable {
                    marioClickPlayer?.seekTo(0)
                    marioClickPlayer?.start()
                    onGetGold()
                }
        )

        Text(
            text = "Money: $coins",
            color = Color.White,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(
            onClick = {
                marioClickPlayer?.seekTo(0)
                marioClickPlayer?.start()
                onGetGold()
            },
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

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        val uri = Uri.parse("android.resource://${ctx.packageName}/${R.raw.subway_surfers}")
                        setVideoURI(uri)
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            mp.setVolume(0f, 0f) // Mute video so it doesn't interrupt game audio
                            start()
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
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
