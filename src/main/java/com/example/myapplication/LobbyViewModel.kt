package com.example.myapplication

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import kotlin.random.Random

data class Player(
    val name: String, 
    val ip: String, 
    val money: Int = 100, 
    val isHost: Boolean = false,
    val avatarUrl: String = "android.resource://com.example.myapplication/drawable/avatar_1"
)

data class GameSettings(
    val loops: Int = 3,
    val blind: Int = 10,
    val volatility: Float = 60f,
    val buyin: Int = 100,
    val seed: Int = Random.nextInt()
)

data class ScoreEntry(val name: String, val balance: Int)

class LobbyViewModel : ViewModel() {
    val players = mutableStateListOf<Player>()
    val isConnected = mutableStateOf(false)
    val statusMessage = mutableStateOf("")
    val leaderboard = mutableStateListOf<ScoreEntry>()
    
    var myName by mutableStateOf("Player ${Random.nextInt(1000)}")
    var myAvatarUrl by mutableStateOf("android.resource://com.example.myapplication/drawable/avatar_1")

    val globalCoins = mutableIntStateOf(0)
    private var isDataLoaded = false
    private var gameDao: GameDao? = null

    fun initDatabase(dao: GameDao) {
        if (gameDao != null) return
        gameDao = dao
        viewModelScope.launch {
            val saved = dao.getGameSave()
            if (saved != null) {
                globalCoins.value = saved.coins
                myName = saved.name
                myAvatarUrl = saved.avatarUrl
            } else {
                globalCoins.value = 0
                // Keep default random name/avatar if not saved yet
            }
            isDataLoaded = true
        }
    }

    private fun saveGame() {
        if (isDataLoaded) {
            viewModelScope.launch {
                gameDao?.saveGame(
                    GameSave(
                        coins = globalCoins.value,
                        name = myName,
                        avatarUrl = myAvatarUrl
                    )
                )
            }
        }
    }

    fun updateProfile(name: String, avatar: String) {
        myName = name
        myAvatarUrl = avatar
        saveGame()
    }

    fun updateCoins(newValue: Int) {
        globalCoins.value = newValue
        saveGame()
    }

    fun addCoins(delta: Int) {
        globalCoins.value += delta
        saveGame()
    }

    private var _gameSettings = mutableStateOf(GameSettings())
    val gameSettings: GameSettings get() = _gameSettings.value

    fun updateSettings(settings: GameSettings) {
        _gameSettings.value = settings
        broadcastSettings()
        // Update host's money display in the list
        val hostIndex = players.indexOfFirst { it.isHost }
        if (hostIndex != -1) {
            players[hostIndex] = players[hostIndex].copy(money = settings.buyin)
        }
    }

    private val _gameStarted = MutableSharedFlow<GameSettings>()
    val gameStarted = _gameStarted.asSharedFlow()

    private val _showLeaderboard = MutableSharedFlow<List<ScoreEntry>>()
    val showLeaderboard = _showLeaderboard.asSharedFlow()

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private val clientWriters = mutableListOf<PrintWriter>()
    
    // For Host: map of playerName to score
    private val scoresReceived = mutableMapOf<String, Int>()

    fun startHosting() {
        players.clear()
        clientWriters.clear()
        players.add(Player(myName, "localhost", money = _gameSettings.value.buyin, isHost = true, avatarUrl = myAvatarUrl))
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(8888)
                statusMessage.value = "Server started on port 8888"
                
                while (true) {
                    val client = serverSocket?.accept() ?: break
                    handleClient(client)
                }
            } catch (e: Exception) {
                statusMessage.value = "Error: ${e.message}"
            }
        }
    }

    private fun handleClient(socket: Socket) {
        viewModelScope.launch(Dispatchers.IO) {
            var writer: PrintWriter? = null
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                writer = PrintWriter(socket.getOutputStream(), true)
                
                // Read name, avatarUrl and coins from client: "Name|AvatarUrl|Coins"
                val clientInfo = reader.readLine() ?: "Unknown|android.resource://com.example.myapplication/drawable/avatar_1|0"
                val parts = clientInfo.split("|")
                val playerName = parts.getOrElse(0) { "Unknown" }
                val playerAvatar = parts.getOrElse(1) { "android.resource://com.example.myapplication/drawable/avatar_1" }
                val playerCoins = parts.getOrElse(2) { "0" }.toIntOrNull() ?: 0

                // Check buy-in
                if (playerCoins < _gameSettings.value.buyin) {
                    writer.println("ERROR:INSUFFICIENT_FUNDS")
                    socket.close()
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    players.add(Player(playerName, socket.inetAddress.hostAddress ?: "unknown", money = _gameSettings.value.buyin, avatarUrl = playerAvatar))
                    synchronized(clientWriters) {
                        clientWriters.add(writer!!)
                    }
                }
                
                // MUST send WELCOME first so the client's initial readLine() succeeds
                writer.println("WELCOME")
                
                // Then send initial data
                sendSettingsToClient(writer!!)
                broadcastPlayerList()
                
                while (socket.isConnected) {
                    val message = reader.readLine() ?: break
                    if (message.startsWith("FINISH_GAME:")) {
                        val score = message.substringAfter("FINISH_GAME:").toIntOrNull() ?: 0
                        handleScoreReport(playerName, score)
                    }
                }
            } catch (e: Exception) {
                // Handle disconnection
            } finally {
                writer?.let { w ->
                    synchronized(clientWriters) {
                        clientWriters.remove(w)
                    }
                }
                withContext(Dispatchers.Main) {
                    players.removeAll { it.ip == socket.inetAddress.hostAddress }
                }
                broadcastPlayerList()
            }
        }
    }

    private fun broadcastPlayerList() {
        // Format: PLAYER_LIST:Name,Money,AvatarUrl|...
        val listMsg = "PLAYER_LIST:" + players.joinToString("|") { "${it.name},${it.money},${it.avatarUrl}" }
        synchronized(clientWriters) {
            clientWriters.forEach { it.println(listMsg) }
        }
    }

    private fun broadcastSettings() {
        val s = _gameSettings.value
        val msg = "SETTINGS:${s.loops},${s.blind},${s.volatility},${s.buyin}"
        synchronized(clientWriters) {
            clientWriters.forEach { it.println(msg) }
        }
    }

    private fun sendSettingsToClient(writer: PrintWriter) {
        val s = _gameSettings.value
        writer.println("SETTINGS:${s.loops},${s.blind},${s.volatility},${s.buyin}")
    }

    fun joinLobby(ip: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    statusMessage.value = "Connecting to $ip..."
                    isConnected.value = false
                }
                clientSocket = Socket(ip, 8888)
                val writer = PrintWriter(clientSocket!!.getOutputStream(), true)
                val reader = BufferedReader(InputStreamReader(clientSocket!!.getInputStream()))
                
                // Send "Name|AvatarUrl|Coins"
                writer.println("$myName|$myAvatarUrl|${globalCoins.value}")
                
                val response = reader.readLine()
                if (response == "WELCOME") {
                    withContext(Dispatchers.Main) {
                        isConnected.value = true
                        statusMessage.value = "Connected!"
                    }
                    
                    while (clientSocket?.isConnected == true) {
                        val message = reader.readLine() ?: break
                        when {
                            message.startsWith("PLAYER_LIST:") -> {
                                val list = message.substringAfter("PLAYER_LIST:").split("|")
                                withContext(Dispatchers.Main) {
                                    players.clear()
                                    list.forEach { 
                                        val parts = it.split(",")
                                        if (parts.size == 3) {
                                            players.add(Player(parts[0], "", money = parts[1].toIntOrNull() ?: 0, avatarUrl = parts[2]))
                                        }
                                    }
                                }
                            }
                            message.startsWith("SETTINGS:") -> {
                                val parts = message.substringAfter("SETTINGS:").split(",")
                                if (parts.size == 4) {
                                    withContext(Dispatchers.Main) {
                                        _gameSettings.value = _gameSettings.value.copy(
                                            loops = parts[0].toInt(),
                                            blind = parts[1].toInt(),
                                            volatility = parts[2].toFloat(),
                                            buyin = parts[3].toInt()
                                        )
                                    }
                                }
                            }
                            message.startsWith("START_GAME:") -> {
                                val parts = message.substringAfter("START_GAME:").split(",")
                                if (parts.size == 5) {
                                    val settings = GameSettings(
                                        loops = parts[0].toInt(),
                                        blind = parts[1].toInt(),
                                        volatility = parts[2].toFloat(),
                                        buyin = parts[3].toInt(),
                                        seed = parts[4].toInt()
                                    )
                                    withContext(Dispatchers.Main) {
                                        updateCoins(globalCoins.value - settings.buyin)
                                    }
                                    _gameStarted.emit(settings)
                                }
                            }
                            message.startsWith("LEADERBOARD:") -> {
                                val list = message.substringAfter("LEADERBOARD:").split("|")
                                val entries = list.mapNotNull { 
                                    val parts = it.split(",")
                                    if (parts.size == 2) ScoreEntry(parts[0], parts[1].toInt()) else null
                                }
                                withContext(Dispatchers.Main) {
                                    handleLeaderboardReceived(entries)
                                }
                                _showLeaderboard.emit(entries)
                            }
                        }
                    }
                } else if (response == "ERROR:INSUFFICIENT_FUNDS") {
                    withContext(Dispatchers.Main) {
                        statusMessage.value = "Error: Insufficient funds for buy-in"
                        clientSocket?.close()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        statusMessage.value = "Failed to connect: Unexpected server response"
                        clientSocket?.close()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    statusMessage.value = "Failed to connect: ${e.message}"
                }
            }
        }
    }

    fun startGame() {
        val settings = _gameSettings.value.copy(seed = Random.nextInt())
        scoresReceived.clear()
        // Subtract buyin from global coins
        updateCoins(globalCoins.value - settings.buyin)
        
        viewModelScope.launch(Dispatchers.IO) {
            val message = "START_GAME:${settings.loops},${settings.blind},${settings.volatility},${settings.buyin},${settings.seed}"
            synchronized(clientWriters) {
                for (writer in clientWriters) {
                    writer.println(message)
                }
            }
            _gameStarted.emit(settings)
        }
    }

    fun reportScore(balance: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            if (serverSocket != null) { // Host
                handleScoreReport(myName, balance)
            } else { // Guest
                val writer = PrintWriter(clientSocket?.getOutputStream(), true)
                writer.println("FINISH_GAME:$balance")
            }
        }
    }

    private fun handleScoreReport(name: String, score: Int) {
        synchronized(scoresReceived) {
            scoresReceived[name] = score
            // If all players reported (Host + all clients)
            if (scoresReceived.size == players.size) {
                val sortedScores = scoresReceived.map { ScoreEntry(it.key, it.value) }
                    .sortedByDescending { it.balance }
                
                val lbMsg = "LEADERBOARD:" + sortedScores.joinToString("|") { "${it.name},${it.balance}" }
                synchronized(clientWriters) {
                    clientWriters.forEach { it.println(lbMsg) }
                }
                viewModelScope.launch(Dispatchers.Main) {
                    leaderboard.clear()
                    leaderboard.addAll(sortedScores)
                    
                    // Award prize to winner
                    if (sortedScores.isNotEmpty()) {
                        val winner = sortedScores[0]
                        if (winner.name == myName && winner.balance > 0) {
                            addCoins(winner.balance)
                        }
                    }
                    
                    _showLeaderboard.emit(sortedScores)
                }
            }
        }
    }

    private fun handleLeaderboardReceived(scores: List<ScoreEntry>) {
        leaderboard.clear()
        leaderboard.addAll(scores)
        // Guest awards prize to themselves if they won
        if (scores.isNotEmpty()) {
            val winner = scores[0]
            if (winner.name == myName && winner.balance > 0) {
                addCoins(winner.balance)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        serverSocket?.close()
        clientSocket?.close()
    }
}
