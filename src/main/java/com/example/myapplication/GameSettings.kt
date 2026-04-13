package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSettingsScreen(
    navController: NavController,
    viewModel: LobbyViewModel,
    availableCoins: Int
) {
    var loops by remember { mutableStateOf("3") }
    var blind by remember { mutableStateOf("10") }
    var volatility by remember { mutableStateOf("60") }
    var buyin by remember { mutableStateOf("100") }

    val backgroundGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF4C1D95), Color(0xFF6B21A8), Color(0xFF9333EA))
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "GAME SETTINGS",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SettingsTextField(
                    label = "LOOPS",
                    value = loops,
                    onValueChange = { loops = it },
                    keyboardType = KeyboardType.Number
                )

                SettingsTextField(
                    label = "BLIND",
                    value = blind,
                    onValueChange = { blind = it },
                    keyboardType = KeyboardType.Number
                )

                SettingsTextField(
                    label = "VOLATILITY",
                    value = volatility,
                    onValueChange = { volatility = it },
                    keyboardType = KeyboardType.Decimal
                )

                SettingsTextField(
                    label = "BUY-IN (Available: $availableCoins)",
                    value = buyin,
                    onValueChange = { buyin = it },
                    keyboardType = KeyboardType.Number
                )

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        val bIn = buyin.toIntOrNull() ?: 0
                        val vol = volatility.toFloatOrNull() ?: 60f
                        if (bIn > 0 && bIn <= availableCoins) {
                            viewModel.updateSettings(
                                GameSettings(
                                    loops = loops.toIntOrNull() ?: 3,
                                    blind = blind.toIntOrNull() ?: 10,
                                    volatility = if (vol < 50f) 50f else vol,
                                    buyin = bIn
                                )
                            )
                            navController.navigate("host")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                    shape = RoundedCornerShape(12.dp),
                    enabled = (buyin.toIntOrNull() ?: 0).let { it > 0 && it <= availableCoins }
                ) {
                    Text(
                        text = "OK",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = Color(0xFFE9D5FF),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.2f)),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFA855F7),
                unfocusedBorderColor = Color.Transparent,
                cursorColor = Color.White
            ),
            singleLine = true
        )
    }
}
