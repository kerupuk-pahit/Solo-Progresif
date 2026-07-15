package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM user_stats WHERE id = 1 LIMIT 1")
    fun getUserStatsFlow(): Flow<UserStats?>

    @Query("SELECT * FROM user_stats WHERE id = 1 LIMIT 1")
    suspend fun getUserStats(): UserStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserStats(userStats: UserStats)

    @Update
    suspend fun updateUserStats(userStats: UserStats)
}

@Dao
interface QuestDao {
    @Query("SELECT * FROM quests ORDER BY id DESC")
    fun getAllQuestsFlow(): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quests WHERE id = :id LIMIT 1")
    suspend fun getQuestById(id: Int): QuestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuest(quest: QuestEntity): Long

    @Update
    suspend fun updateQuest(quest: QuestEntity)

    @Delete
    suspend fun deleteQuest(quest: QuestEntity)

    @Query("DELETE FROM quests WHERE id = :id")
    suspend fun deleteQuestById(id: Int)

    @Query("DELETE FROM quests WHERE isCompleted = 0 AND isMandatory = 0")
    suspend fun clearNonCompletedCustomQuests()
}

@Dao
interface HabitLogDao {
    @Query("SELECT * FROM habit_logs ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs ORDER BY timestamp DESC")
    suspend fun getAllLogs(): List<HabitLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: HabitLogEntity)

    @Query("DELETE FROM habit_logs")
    suspend fun clearLogs()
}

@Dao
interface ActiveLockDao {
    @Query("SELECT * FROM active_locks")
    fun getAllLocksFlow(): Flow<List<ActiveLockEntity>>

    @Query("SELECT * FROM active_locks")
    suspend fun getAllLocks(): List<ActiveLockEntity>

    @Query("SELECT * FROM active_locks WHERE appName = :appName LIMIT 1")
    suspend fun getLockForApp(appName: String): ActiveLockEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLock(lock: ActiveLockEntity)

    @Query("DELETE FROM active_locks WHERE appName = :appName")
    suspend fun deleteLock(appName: String)

    @Query("DELETE FROM active_locks")
    suspend fun clearAllLocks()
}
