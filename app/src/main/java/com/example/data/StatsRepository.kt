package com.example.data

import kotlinx.coroutines.flow.Flow

class StatsRepository(private val statsDao: StatsDao) {
    val profile: Flow<PlayerProfile?> = statsDao.getProfile()
    val scoreRecords: Flow<List<ScoreRecord>> = statsDao.getScoreRecords()

    suspend fun saveProfile(profile: PlayerProfile) {
        statsDao.insertProfile(profile)
    }

    suspend fun updateProfile(profile: PlayerProfile) {
        statsDao.updateProfile(profile)
    }

    suspend fun insertScoreRecord(record: ScoreRecord) {
        statsDao.insertScoreRecord(record)
    }

    suspend fun clearHistory() {
        statsDao.clearHistory()
    }
}
