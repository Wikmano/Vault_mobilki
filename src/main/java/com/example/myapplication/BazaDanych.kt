package com.example.myapplication

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Entity(tableName = "game_save")
data class GameSave(
    @PrimaryKey val id: Int = 1,
    val coins: Int,
    val name: String = "Player",
    val avatarUrl: String = "android.resource://com.example.myapplication/drawable/avatar_1"
)

@Dao
interface GameDao {
    @Query("SELECT coins FROM game_save WHERE id = 1")
    suspend fun getCoins(): Int?

    @Query("SELECT * FROM game_save WHERE id = 1")
    suspend fun getGameSave(): GameSave?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveGame(save: GameSave)
}

@Database(entities = [GameSave::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
}