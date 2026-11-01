package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StatsDao {
    @Query("SELECT * FROM player_profile WHERE id = 1 LIMIT 1")
    fun getProfile(): Flow<PlayerProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: PlayerProfile)

    @Update
    suspend fun updateProfile(profile: PlayerProfile)

    @Query("SELECT * FROM score_records ORDER BY timestamp DESC")
    fun getScoreRecords(): Flow<List<ScoreRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScoreRecord(record: ScoreRecord)

    @Query("DELETE FROM score_records")
    suspend fun clearHistory()
}
