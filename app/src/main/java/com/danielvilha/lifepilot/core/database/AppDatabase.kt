package com.danielvilha.lifepilot.core.database

import androidx.room.Database
import androidx.room.RoomDatabase


@Database(
    entities = [TaskEntity::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
}