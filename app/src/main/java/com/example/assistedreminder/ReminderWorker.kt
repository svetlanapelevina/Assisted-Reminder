package com.example.assistedreminder

import android.annotation.SuppressLint
import android.content.Context
import android.os.AsyncTask
import androidx.room.Room
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.assistedreminder.db.AppDatabase
import com.example.assistedreminder.db.ReminderInfo

class ReminderWorker(appContext: Context, workerParameters: WorkerParameters) :
    Worker(appContext, workerParameters) {

    override fun doWork(): Result {
        val text = inputData.getString("message") // this comes from the reminder parameters
        val uid = inputData.getInt("uid", 0)

        changeReminderSeen(uid)

        Reminders.showNofitication(applicationContext, text!!)
        return Result.success()
    }

    private fun changeReminderSeen(uid: Int) {
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "remindersInfo"
        )
            .fallbackToDestructiveMigration()
            .build()

        val remindersDao = db.remindersDao()

        var reminder: ReminderInfo = remindersDao.getReminderById(uid.toString())[0]
        reminder.reminder_seen = true
        reminder.reminder_active = false

        remindersDao.update(reminder)
    }
}