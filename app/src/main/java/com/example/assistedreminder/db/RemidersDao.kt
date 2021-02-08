package com.example.assistedreminder.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface RemindersDao {
    @Transaction
    @Insert
    fun insert(paymentInfo: ReminderInfo): Long

    @Query("SELECT * FROM remindersInfo")
    fun getReminders(): List<ReminderInfo>
}