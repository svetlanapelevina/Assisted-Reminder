package com.example.assistedreminder

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.room.Room
import com.example.assistedreminder.db.AppDatabase
import com.example.assistedreminder.db.ReminderInfo

class Reminders : AppCompatActivity() {
    private lateinit var remindersListView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminders)

        findViewById<Button>(R.id.profileButton).setOnClickListener {
            startActivity(
                Intent(applicationContext, Profile::class.java)
            )
        }

        findViewById<Button>(R.id.logoutButton).setOnClickListener {
            applicationContext.getSharedPreferences(
                getString(R.string.sharedPreference),
                Context.MODE_PRIVATE
            ).edit().putInt("LoginStatus", 0).apply()

            Toast.makeText(this, "Log out successful", Toast.LENGTH_SHORT).show()

            startActivity(
                Intent(applicationContext, MainActivity::class.java)
            )
        }

        addNewReminder()
    }

    fun addNewReminder() {
        remindersListView = findViewById<ListView>(R.id.remindersList)

        val reminders: ArrayList<Reminder> = arrayListOf(
            Reminder("Reminder 1", 156, 189, "13.12.2020"),
            Reminder("Reminder 2", 58, 1, "13.12.2020"),
            Reminder("Reminder 3", 151896, 2525, "13.12.2020")
            )

        remindersListView.adapter = ReminderAdapter(this, reminders)
    }

//    fun addNewReminder() {
//        AsyncTask.execute {
//            val db = Room.databaseBuilder(
//                applicationContext,
//                AppDatabase::class.java, "accountsData"
//            ).build()
//
//            val reminder = ReminderInfo(
//                null,
//                message = "Reminder message",
//                location_x = 185,
//                location_y = 169,
//                creation_time = "18:33"
//            )
//            val remindersDao = db.remindersDao()
//            remindersDao.insert(reminder);
//        }
//    }

//    inner class LoadPaymentInfoEntries : AsyncTask<String?, String?, List<ReminderInfo>>() {
//        override fun doInBackground(vararg params: String?): List<ReminderInfo> {
//            val db = Room
//                .databaseBuilder(
//                    applicationContext,
//                    AppDatabase::class.java,
//                    getString(R.string.dbFileName)
//                )
//                .build()
//            val remindersDao = db.remindersDao()
//            val remindersInfo: List<ReminderInfo> = remindersDao.getReminders()
//            db.close()
//            return remindersInfo
//        }
//
//        override fun onPostExecute(paymentInfos: List<ReminderInfo>?) {
//            super.onPostExecute(paymentInfos)
//        }
//    }
}