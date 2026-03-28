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
    val coins: Int
)

@Dao
interface GameDao {
    @Query("SELECT coins FROM game_save WHERE id = 1")
    suspend fun getCoins(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCoins(save: GameSave)
}

@Database(entities = [GameSave::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
}