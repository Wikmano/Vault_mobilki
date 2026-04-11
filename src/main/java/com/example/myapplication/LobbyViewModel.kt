package com.example.myapplication

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
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

data class Player(val name: String, val ip: String, val money: String = "$1000.00", val isHost: Boolean = false)

class LobbyViewModel : ViewModel() {
    val players = mutableStateListOf<Player>()
    val isConnected = mutableStateOf(false)
    val statusMessage = mutableStateOf("")

    private val _gameStarted = MutableSharedFlow<Int>()
    val gameStarted = _gameStarted.asSharedFlow()

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private val clientWriters = mutableListOf<PrintWriter>()

    fun startHosting() {
        players.clear()
        clientWriters.clear()
        players.add(Player("You (Host)", "localhost", isHost = true))
        
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
                
                synchronized(clientWriters) {
                    clientWriters.add(writer)
                }
                
                val playerName = reader.readLine() ?: "Unknown Player"
                withContext(Dispatchers.Main) {
                    players.add(Player(playerName, socket.inetAddress.hostAddress ?: "unknown"))
                }
                
                writer.println("WELCOME")
                
                // Keep connection alive and listen for messages if needed
                while (socket.isConnected) {
                    val message = reader.readLine() ?: break
                    // Handle incoming messages from clients if any
                }
            } catch (e: Exception) {
                // Handle disconnection
            } finally {
                writer?.let {
                    synchronized(clientWriters) {
                        clientWriters.remove(it)
                    }
                }
                withContext(Dispatchers.Main) {
                    players.removeAll { it.ip == socket.inetAddress.hostAddress }
                }
            }
        }
    }

    fun joinLobby(ip: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                statusMessage.value = "Connecting to $ip..."
                clientSocket = Socket(ip, 8888)
                val writer = PrintWriter(clientSocket!!.getOutputStream(), true)
                val reader = BufferedReader(InputStreamReader(clientSocket!!.getInputStream()))
                
                writer.println("Guest ${System.currentTimeMillis() % 1000}")
                
                val response = reader.readLine()
                if (response == "WELCOME") {
                    withContext(Dispatchers.Main) {
                        isConnected.value = true
                        statusMessage.value = "Connected!"
                    }
                    
                    // Listen for game start command
                    while (clientSocket?.isConnected == true) {
                        val message = reader.readLine() ?: break
                        if (message.startsWith("START_GAME:")) {
                            val seed = message.substringAfter("START_GAME:").toIntOrNull() ?: 0
                            _gameStarted.emit(seed)
                        }
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
        val seed = Random.nextInt()
        viewModelScope.launch(Dispatchers.IO) {
            synchronized(clientWriters) {
                for (writer in clientWriters) {
                    writer.println("START_GAME:$seed")
                }
            }
            _gameStarted.emit(seed)
        }
    }

    override fun onCleared() {
        super.onCleared()
        serverSocket?.close()
        clientSocket?.close()
    }
}
