package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    var monety by remember { mutableStateOf(0) }

    NavHost(navController = navController, startDestination = "menu") {
        composable("menu") {
            MainMenu(navController = navController, monety = monety)
        }
        composable("clicker") {
            ClickerScreen(
                navController = navController,
                monety = monety,
                onGetGold = { monety += 1 }
            )
        }
        composable("game") {
            GameScreen(navController = navController)
        }
        composable("host") { Text("Ekran HOST LOBBY") }
        composable("join") { Text("Ekran JOIN LOBBY") }
    }
}
