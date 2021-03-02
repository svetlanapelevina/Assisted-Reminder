package com.example.assistedreminder.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = arrayOf(ReminderInfo::class), version = 3)
abstract class AppDatabase : RoomDatabase() {
    abstract fun remindersDao(): RemindersDao
}