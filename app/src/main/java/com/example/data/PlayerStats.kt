package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_profile")
data class PlayerProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "Shubhankar",
    val stars: Int = 0,
    val correctAnswers: Int = 0,
    val level: Int = 1,
    val classLevel: Int = 3, // Class 1 to 8
    val board: String = "Odisha Board", // "Odisha Board" or "NCERT/CBSE"
    val stateName: String = "Odisha",
    val coins: Int = 120,
    val xp: Int = 150,
    val streak: Int = 3,
    val lastActiveDate: Long = System.currentTimeMillis(),
    val unlockedAdditionBadge: Boolean = false,
    val unlockedSubtractionBadge: Boolean = false,
    val unlockedMultiplicationBadge: Boolean = false,
    val unlockedDivisionBadge: Boolean = false,
    val unlockedPerfectScoreBadge: Boolean = false,
    val unlockedSuperStarBadge: Boolean = false
)

@Entity(tableName = "score_records")
data class ScoreRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val gameMode: String, // "COUNT", "ADD", "SUB", "MUL", "DIV", "QUIZ"
    val score: Int,
    val totalQuestions: Int,
    val starsEarned: Int,
    val timestamp: Long = System.currentTimeMillis()
)
