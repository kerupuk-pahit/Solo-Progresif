package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserStats::class,
        QuestEntity::class,
        HabitLogEntity::class,
        ActiveLockEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun questDao(): QuestDao
    abstract fun habitLogDao(): HabitLogDao
    abstract fun activeLockDao(): ActiveLockDao
}
