package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.myapplication.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, viewModel: LobbyViewModel) {
    var username by remember { mutableStateOf(viewModel.myName) }
    var selectedAvatar by remember { mutableStateOf(viewModel.myAvatarUrl) }
    
    val context = LocalContext.current
    val avatars = remember {
        List(20) { i -> "android.resource://${context.packageName}/drawable/avatar_${i + 1}" }
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF4C1D95), Color(0xFF1E1B4B))
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("PROFILE", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Current Avatar Preview
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .border(4.dp, AccentPurple, CircleShape)
            ) {
                AsyncImage(
                    model = selectedAvatar,
                    contentDescription = "Selected Avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Username Field
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username", color = Color.White.copy(alpha = 0.7f)) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(color = Color.White),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentPurple,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    cursorColor = Color.White
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "CHOOSE AVATAR",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Avatar Grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(60.dp),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(avatars) { url ->
                    val isSelected = selectedAvatar == url
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color.Green else Color.White.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                            .clickable { selectedAvatar = url },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Check, null, tint = Color.Green)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.updateProfile(username, selectedAvatar)
                    navController.popBackStack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("SAVE PROFILE", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
