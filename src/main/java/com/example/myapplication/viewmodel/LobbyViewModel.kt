package com.example.myapplication.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.GameDao
import com.example.myapplication.model.*
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
            }
            isDataLoaded = true
        }
    }

    private fun saveGame() {
        if (isDataLoaded) {
            viewModelScope.launch {
                gameDao?.saveGame(GameSave(coins = globalCoins.value, name = myName, avatarUrl = myAvatarUrl))
            }
        }
    }

    fun updateProfile(name: String, avatar: String) {
        myName = name
        myAvatarUrl = avatar
        saveGame()
    }

    fun addCoins(delta: Int) {
        globalCoins.value += delta
        saveGame()
    }

    fun updateCoins(newValue: Int) {
        globalCoins.value = newValue
        saveGame()
    }

    private var _gameSettings = mutableStateOf(GameSettings())
    val gameSettings: GameSettings get() = _gameSettings.value

    fun updateSettings(settings: GameSettings) {
        _gameSettings.value = settings
        broadcastSettings()
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
    private val connectedClientSockets = mutableListOf<Socket>()
    private val scoresReceived = mutableMapOf<String, Int>()

    fun leaveLobby() {
        resetUIState()
        viewModelScope.launch(Dispatchers.IO) {
            cleanupSocketsInternal()
        }
    }

    private fun resetUIState() {
        players.clear()
        isConnected.value = false
        statusMessage.value = ""
        leaderboard.clear()
        scoresReceived.clear()
    }

    private suspend fun cleanupSocketsInternal() {
        try {
            serverSocket?.close()
            serverSocket = null
            clientSocket?.close()
            clientSocket = null
            
            synchronized(connectedClientSockets) {
                connectedClientSockets.forEach { try { it.close() } catch(e: Exception) {} }
                connectedClientSockets.clear()
            }
            
            synchronized(clientWriters) {
                clientWriters.forEach { try { it.close() } catch(e: Exception) {} }
                clientWriters.clear()
            }
        } catch (e: Exception) {}
    }

    fun startHosting() {
        resetUIState()
        viewModelScope.launch(Dispatchers.IO) {
            cleanupSocketsInternal()
            delay(300)
            
            withContext(Dispatchers.Main) {
                players.add(Player(myName, "localhost", money = _gameSettings.value.buyin, isHost = true, avatarUrl = myAvatarUrl))
            }

            try {
                val socket = ServerSocket()
                socket.reuseAddress = true
                socket.bind(java.net.InetSocketAddress("0.0.0.0", 8888))
                serverSocket = socket
                
                withContext(Dispatchers.Main) {
                    statusMessage.value = "Server started on port 8888"
                }

                while (true) {
                    val client = serverSocket?.accept() ?: break
                    handleClient(client)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    statusMessage.value = "Host Error: ${e.message}"
                }
            }
        }
    }

    private fun handleClient(socket: Socket) {
        viewModelScope.launch(Dispatchers.IO) {
            var writer: PrintWriter? = null
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                writer = PrintWriter(socket.getOutputStream(), true)
                
                val clientInfo = reader.readLine() ?: return@launch
                val parts = clientInfo.split("|")
                val playerName = parts.getOrElse(0) { "Unknown" }
                val playerAvatar = parts.getOrElse(1) { myAvatarUrl }
                val playerCoins = parts.getOrElse(2) { "0" }.toIntOrNull() ?: 0

                if (playerCoins < _gameSettings.value.buyin) {
                    writer.println("ERROR:INSUFFICIENT_FUNDS")
                    writer.flush()
                    socket.close()
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    players.add(Player(playerName, socket.inetAddress.hostAddress ?: "unknown", money = _gameSettings.value.buyin, avatarUrl = playerAvatar))
                    synchronized(clientWriters) { clientWriters.add(writer!!) }
                    synchronized(connectedClientSockets) { connectedClientSockets.add(socket) }
                }
                
                writer.println("WELCOME")
                writer.flush()
                
                sendSettingsToClient(writer!!)
                broadcastPlayerList()
                
                while (!socket.isClosed) {
                    val message = reader.readLine() ?: break
                    if (message.startsWith("FINISH_GAME:")) {
                        val score = message.substringAfter("FINISH_GAME:").toIntOrNull() ?: 0
                        handleScoreReport(playerName, score)
                    }
                }
            } catch (e: Exception) {
            } finally {
                synchronized(clientWriters) { writer?.let { clientWriters.remove(it) } }
                synchronized(connectedClientSockets) { connectedClientSockets.remove(socket) }
                try { socket.close() } catch(e: Exception) {}
                withContext(Dispatchers.Main) {
                    players.removeAll { it.ip == socket.inetAddress.hostAddress }
                }
                broadcastPlayerList()
            }
        }
    }

    fun joinLobby(ip: String) {
        resetUIState()
        viewModelScope.launch(Dispatchers.IO) {
            cleanupSocketsInternal()
            delay(300)
            try {
                withContext(Dispatchers.Main) {
                    statusMessage.value = "Connecting to $ip..."
                }
                
                val socket = Socket()
                socket.connect(java.net.InetSocketAddress(ip, 8888), 5000)
                clientSocket = socket
                
                val writer = PrintWriter(socket.getOutputStream(), true)
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                
                writer.println("$myName|$myAvatarUrl|${globalCoins.value}")
                writer.flush()
                
                val response = reader.readLine()
                if (response == "WELCOME") {
                    withContext(Dispatchers.Main) {
                        isConnected.value = true
                        statusMessage.value = "Connected!"
                    }
                    
                    while (!socket.isClosed) {
                        val message = reader.readLine() ?: break
                        handleServerMessage(message)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        statusMessage.value = response ?: "Connection failed"
                    }
                    socket.close()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    statusMessage.value = "Join Error: ${e.message}"
                }
            }
        }
    }

    private suspend fun handleServerMessage(message: String) {
        withContext(Dispatchers.Main) {
            when {
                message.startsWith("PLAYER_LIST:") -> {
                    val list = message.substringAfter("PLAYER_LIST:").split("|")
                    players.clear()
                    list.forEach { 
                        val parts = it.split(",")
                        if (parts.size == 3) {
                            players.add(Player(parts[0], "", money = parts[1].toIntOrNull() ?: 0, avatarUrl = parts[2]))
                        }
                    }
                }
                message.startsWith("SETTINGS:") -> {
                    val parts = message.substringAfter("SETTINGS:").split(",")
                    if (parts.size == 4) {
                        _gameSettings.value = _gameSettings.value.copy(
                            loops = parts[0].toInt(), blind = parts[1].toInt(),
                            volatility = parts[2].toFloat(), buyin = parts[3].toInt()
                        )
                    }
                }
                message.startsWith("START_GAME:") -> {
                    val parts = message.substringAfter("START_GAME:").split(",")
                    if (parts.size == 5) {
                        val settings = GameSettings(
                            loops = parts[0].toInt(), blind = parts[1].toInt(),
                            volatility = parts[2].toFloat(), buyin = parts[3].toInt(), seed = parts[4].toInt()
                        )
                        updateCoins(globalCoins.value - settings.buyin)
                        _gameStarted.emit(settings)
                    }
                }
                message.startsWith("LEADERBOARD:") -> {
                    val list = message.substringAfter("LEADERBOARD:").split("|")
                    val entries = list.mapNotNull { 
                        val parts = it.split(",")
                        if (parts.size == 2) ScoreEntry(parts[0], parts[1].toInt()) else null
                    }
                    handleLeaderboardReceived(entries)
                    _showLeaderboard.emit(entries)
                }
            }
        }
    }

    fun startGame() {
        val settings = _gameSettings.value.copy(seed = Random.nextInt())
        updateCoins(globalCoins.value - settings.buyin)
        viewModelScope.launch(Dispatchers.IO) {
            val message = "START_GAME:${settings.loops},${settings.blind},${settings.volatility},${settings.buyin},${settings.seed}"
            broadcastMessage(message)
            _gameStarted.emit(settings)
        }
    }

    fun reportScore(balance: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            if (serverSocket != null) {
                handleScoreReport(myName, balance)
            } else {
                val writer = PrintWriter(clientSocket?.getOutputStream(), true)
                writer.println("FINISH_GAME:$balance")
                writer.flush()
            }
        }
    }

    private fun handleScoreReport(name: String, score: Int) {
        synchronized(scoresReceived) {
            scoresReceived[name] = score
            if (scoresReceived.size == players.size) {
                val sortedScores = scoresReceived.map { ScoreEntry(it.key, it.value) }.sortedByDescending { it.balance }
                broadcastMessage("LEADERBOARD:" + sortedScores.joinToString("|") { "${it.name},${it.balance}" })
                viewModelScope.launch(Dispatchers.Main) {
                    leaderboard.clear()
                    leaderboard.addAll(sortedScores)
                    if (sortedScores.isNotEmpty() && sortedScores[0].name == myName && sortedScores[0].balance > 0) {
                        addCoins(sortedScores[0].balance)
                    }
                    _showLeaderboard.emit(sortedScores)
                }
            }
        }
    }

    private fun handleLeaderboardReceived(scores: List<ScoreEntry>) {
        leaderboard.clear()
        leaderboard.addAll(scores)
        if (scores.isNotEmpty() && scores[0].name == myName && scores[0].balance > 0) {
            addCoins(scores[0].balance)
        }
    }

    private fun broadcastMessage(msg: String) {
        synchronized(clientWriters) {
            clientWriters.forEach { it.println(msg); it.flush() }
        }
    }

    private fun broadcastPlayerList() {
        broadcastMessage("PLAYER_LIST:" + players.joinToString("|") { "${it.name},${it.money},${it.avatarUrl}" })
    }

    private fun broadcastSettings() {
        val s = _gameSettings.value
        broadcastMessage("SETTINGS:${s.loops},${s.blind},${s.volatility},${s.buyin}")
    }

    private fun sendSettingsToClient(writer: PrintWriter) {
        val s = _gameSettings.value
        writer.println("SETTINGS:${s.loops},${s.blind},${s.volatility},${s.buyin}")
        writer.flush()
    }

    override fun onCleared() {
        super.onCleared()
        leaveLobby()
    }
}
