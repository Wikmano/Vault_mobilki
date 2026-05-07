package com.example.myapplication.model

import androidx.room.Entity
import androidx.room.PrimaryKey
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

@Entity(tableName = "game_save")
data class GameSave(
    @PrimaryKey val id: Int = 1,
    val coins: Int,
    val name: String = "Player",
    val avatarUrl: String = "android.resource://com.example.myapplication/drawable/avatar_1"
)
