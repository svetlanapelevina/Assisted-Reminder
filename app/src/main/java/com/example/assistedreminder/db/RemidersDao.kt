package com.example.assistedreminder.db

import androidx.room.*

@Dao
interface RemindersDao {
    @Transaction
    @Insert
    fun insert(paymentInfo: ReminderInfo): Long

    @Update
    fun update(paymentInfo: ReminderInfo)

    @Query("SELECT * FROM remindersInfo WHERE creator_id = :id")
    fun getReminders(id: String): List<ReminderInfo>

    @Query("SELECT * FROM remindersInfo WHERE uid = :id")
    fun getReminderById(id: String): List<ReminderInfo>

    @Query("DELETE FROM remindersInfo WHERE uid = :id")
    fun deleteReminder(id: Int)
}