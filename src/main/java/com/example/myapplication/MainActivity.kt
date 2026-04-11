package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val lobbyViewModel: LobbyViewModel = viewModel()

    val database = remember {
        androidx.room.Room.databaseBuilder(
            context,
            AppDatabase::class.java, "game-database"
        ).build()
    }
    val gameDao = database.gameDao()

    var coins by remember { mutableStateOf(0) }
    var isDataLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val savedCoins = gameDao.getCoins() ?: 0
        coins = savedCoins
        isDataLoaded = true
    }

    LaunchedEffect(coins) {
        if (isDataLoaded) {
            gameDao.saveCoins(GameSave(coins = coins))
        }
    }

    // Listen for game start from the ViewModel
    LaunchedEffect(Unit) {
        lobbyViewModel.gameStarted.collect { seed ->
            // In a real app, you would pass the seed to GameScreen
            navController.navigate("game")
        }
    }

    NavHost(navController = navController, startDestination = "menu") {
        composable("menu") {
            MainMenu(navController = navController, monety = coins)
        }
        composable("clicker") {
            ClickerScreen(
                navController = navController,
                coins = coins,
                onGetGold = { coins += 1 }
            )
        }
        composable("game") {
            GameScreen(
                navController = navController,
                coins = coins,
                onCoinsChange = { coins = it }
            )
        }
        composable("host") {
            HostLobbyScreen(
                viewModel = lobbyViewModel,
                onBack = { navController.popBackStack() },
                onStartGame = { lobbyViewModel.startGame() }
            )
        }
        composable("join") {
            JoinLobbyScreen(
                viewModel = lobbyViewModel,
                onBack = { navController.popBackStack() },
                onJoinSuccess = {
                    navController.navigate("guest_lobby")
                }
            )
        }
        composable("guest_lobby") {
            HostLobbyScreen(
                viewModel = lobbyViewModel,
                onBack = { navController.popBackStack() },
                onStartGame = { /* Guests can't start the game */ }
            )
        }
    }
}
