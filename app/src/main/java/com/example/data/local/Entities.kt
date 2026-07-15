package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey val id: Int = 1,
    val level: Int = 1,
    val exp: Int = 0,
    val gold: Int = 100,
    val emergencyPasses: Int = 1,
    val streak: Int = 0,
    val rank: String = "E-Rank Hunter",
    val title: String = "The Chosen One",
    val aura: String = "None",
    val skin: String = "Default Cyber",
    val lastLoginDate: String = "", // YYYY-MM-DD
    val balanceScore: Int = 50, // 0 - 100
    val shopPenaltyPercent: Int = 0, // e.g. 0% initially, rises by +30%, +35% etc.
    val consecutiveSuccessfulMandatoryDays: Int = 0
)

@Entity(tableName = "quests")
data class QuestEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String, // Belajar, Olahraga, Membaca, Meditasi, Kebersihan, Kesehatan, Custom
    val difficulty: String, // Easy, Medium, Hard, Legendary
    val durationMinutes: Int, // 0 if no timer, otherwise minutes
    val expReward: Int,
    val goldReward: Int,
    val isCompleted: Boolean = false,
    val isMandatory: Boolean = false,
    val deadlineTime: String? = null, // "HH:mm"
    val dateCreated: Long = System.currentTimeMillis()
)

@Entity(tableName = "habit_logs")
data class HabitLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String, // Belajar, Olahraga, Membaca, Meditasi, Kebersihan, Kesehatan
    val durationMinutes: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "active_locks")
data class ActiveLockEntity(
    @PrimaryKey val appName: String, // e.g. "Mobile Legends", "Free Fire", etc.
    val expiresAt: Long // timestamp when lock resumes
)
