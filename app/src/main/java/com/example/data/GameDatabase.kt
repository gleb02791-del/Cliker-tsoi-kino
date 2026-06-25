package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "game_state")
data class GameStateEntity(
    @PrimaryKey val id: Int = 1,
    val points: Double = 0.0,
    val totalClicks: Int = 0,
    val totalPointsEarned: Double = 0.0,
    val guitarLevel: Int = 0,
    val venueLevels: String = "0,0,0,0", // Kamchatka, Rock Club, Olimpiyskiy, Luzhniki
    val songLevels: String = "0,0,0,0,0", // 5 songs: Zvezda, Pack, Blood, Peremen, Kukushka
    val peopleLevels: String = "0,0,0,0,0", // 5 people: Kasparyan, Guryanov, Tikhomirov, Marianna, Joanna
    val unlockedAchievements: String = "", // comma-separated achievement keys
    val lastSavedTime: Long = System.currentTimeMillis()
)

@Dao
interface GameStateDao {
    @Query("SELECT * FROM game_state WHERE id = 1 LIMIT 1")
    fun getGameStateFlow(): Flow<GameStateEntity?>

    @Query("SELECT * FROM game_state WHERE id = 1 LIMIT 1")
    suspend fun getGameState(): GameStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(gameState: GameStateEntity)
}

@Database(entities = [GameStateEntity::class], version = 1, exportSchema = false)
abstract class GameDatabase : RoomDatabase() {
    abstract fun gameStateDao(): GameStateDao

    companion object {
        @Volatile
        private var INSTANCE: GameDatabase? = null

        fun getDatabase(context: Context): GameDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GameDatabase::class.java,
                    "tsoi_clicker_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
