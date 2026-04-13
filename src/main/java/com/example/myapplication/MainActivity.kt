package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
        ).fallbackToDestructiveMigration()
            .build()
    }
    
    // Initialize DB in ViewModel once
    LaunchedEffect(Unit) {
        lobbyViewModel.initDatabase(database.gameDao())
    }

    val coins by lobbyViewModel.globalCoins

    // Listen for game start
    LaunchedEffect(Unit) {
        lobbyViewModel.gameStarted.collect { settings ->
            navController.navigate("game/${settings.loops}/${settings.blind}/${settings.volatility}/${settings.buyin}/${settings.seed}")
        }
    }

    // Listen for leaderboard
    LaunchedEffect(Unit) {
        lobbyViewModel.showLeaderboard.collect { scores ->
            navController.navigate("leaderboard")
        }
    }

    NavHost(navController = navController, startDestination = "menu") {
        composable("menu") {
            MainMenu(navController = navController, monety = coins)
        }
        composable("profile") {
            ProfileScreen(navController = navController, viewModel = lobbyViewModel)
        }
        composable("settings") {
            GameSettingsScreen(
                navController = navController,
                viewModel = lobbyViewModel,
                availableCoins = coins
            )
        }
        composable("clicker") {
            ClickerScreen(
                navController = navController,
                coins = coins,
                onGetGold = { lobbyViewModel.addCoins(1) }
            )
        }
        composable(
            "game/{loops}/{blind}/{volatility}/{buyin}/{seed}",
            arguments = listOf(
                navArgument("loops") { type = NavType.IntType },
                navArgument("blind") { type = NavType.IntType },
                navArgument("volatility") { type = NavType.FloatType },
                navArgument("buyin") { type = NavType.IntType },
                navArgument("seed") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val loops = backStackEntry.arguments?.getInt("loops") ?: 3
            val blind = backStackEntry.arguments?.getInt("blind") ?: 10
            val volatility = backStackEntry.arguments?.getFloat("volatility") ?: 60f
            val buyin = backStackEntry.arguments?.getInt("buyin") ?: 100
            val seed = backStackEntry.arguments?.getInt("seed") ?: 0
            
            var inGameBalance by remember { mutableIntStateOf(buyin) }

            GameScreen(
                navController = navController,
                coins = inGameBalance,
                onCoinsChange = { 
                    inGameBalance = it
                },
                onFinished = { finalBalance ->
                    lobbyViewModel.reportScore(finalBalance)
                },
                loops = loops,
                blind = blind,
                volatility = volatility,
                seed = seed
            )
        }
        composable("leaderboard") {
            LeaderboardScreen(
                scores = lobbyViewModel.leaderboard,
                onBackToMenu = { 
                    navController.navigate("menu") {
                        popUpTo("menu") { inclusive = true }
                    }
                }
            )
        }
        composable("host") {
            HostLobbyScreen(
                viewModel = lobbyViewModel,
                onBack = { navController.popBackStack() },
                onStartGame = { lobbyViewModel.startGame() },
                isHost = true
            )
        }
        composable("join") {
            JoinLobbyScreen(
                viewModel = lobbyViewModel,
                onBack = { navController.popBackStack() },
                onJoinSuccess = { ip ->
                    navController.navigate("guest_lobby/$ip")
                }
            )
        }
        composable(
            "guest_lobby/{serverIp}",
            arguments = listOf(navArgument("serverIp") { type = NavType.StringType })
        ) { backStackEntry ->
            val serverIp = backStackEntry.arguments?.getString("serverIp") ?: "Unknown"
            HostLobbyScreen(
                viewModel = lobbyViewModel,
                onBack = { navController.popBackStack() },
                onStartGame = { /* Guests can't start */ },
                isHost = false,
                serverIp = serverIp
            )
        }
    }
}
