package com.example.myapplication.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import com.example.myapplication.model.GameSave

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
