package com.example.assistedreminder

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.room.Room
import com.example.assistedreminder.db.AppDatabase
import com.example.assistedreminder.db.ReminderInfo

class Reminders : AppCompatActivity() {
    private lateinit var remindersListView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminders)

        remindersListView = findViewById(R.id.remindersList)
        refreshRemindersListView()

        remindersListView.setOnItemClickListener { parent, view, position, id ->
            val selectedReminder = remindersListView.adapter.getItem(position) as ReminderInfo
            startActivity(
                Intent(applicationContext, ReminderEdit::class.java)
                    .putExtra("uid", selectedReminder.uid.toString())
            )
        }

        remindersListView.setOnItemLongClickListener { parent, view, position, id ->
            deleteReminderByClick(position)
            return@setOnItemLongClickListener (true)
        }

        findViewById<Button>(R.id.profileButton).setOnClickListener {
            startActivity(
                Intent(applicationContext, Profile::class.java)
            )
        }

        findViewById<Button>(R.id.logoutButton).setOnClickListener {
            logOutUser()
        }

        findViewById<Button>(R.id.addNewReminderButton).setOnClickListener {
            startActivity(
                Intent(applicationContext, ReminderAddNew::class.java)
            )
        }
    }

    // User logout
    private fun logOutUser() {
        val spName = getString(R.string.sharedPreference);
        val spMode = Context.MODE_PRIVATE

        applicationContext.getSharedPreferences(spName, spMode)
            .edit()
            .putInt("LoginStatus", 0)
            .apply()

        Toast.makeText(this, "Log out successful", Toast.LENGTH_SHORT).show()

        startActivity(
            Intent(applicationContext, MainActivity::class.java)
        )
    }

    // delete reminder by long click on reminder
    private fun deleteReminderByClick(position: Int) {
        val selectedReminder = remindersListView.adapter.getItem(position) as ReminderInfo
        val message =
            "Do you want to delete a reminder \" ${selectedReminder.message} \"?"

        // Show AlertDialog to delete the reminder
        val builder = AlertDialog.Builder(this@Reminders)
        builder.setTitle("Delete reminder?")
            .setMessage(message)
            .setPositiveButton("Delete") { _, _ ->
                //delete from database
                AsyncTask.execute {
                    val db = Room
                        .databaseBuilder(
                            applicationContext,
                            AppDatabase::class.java,
                            getString(R.string.dbFileName)
                        )
                        .build()
                    db.remindersDao().deleteReminder(selectedReminder.uid!!)
                }

                refreshRemindersListView()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        refreshRemindersListView()
    }

    // refresh list of reminders
    private fun refreshRemindersListView() {
        val refreshTask = LoadRemindersInfoEntries()
        refreshTask.execute()
    }

    // get current username
    private fun getCurrentUsername(): String {
        val username = applicationContext.getSharedPreferences(
            getString(R.string.sharedPreference), Context.MODE_PRIVATE
        ).getString("currentUsername", "") ?: ""

        return username
    }

    // get reminders data from DB
    inner class LoadRemindersInfoEntries : AsyncTask<String?, String?, List<ReminderInfo>>() {
        override fun doInBackground(vararg params: String?): List<ReminderInfo> {
            val db = Room
                .databaseBuilder(
                    applicationContext,
                    AppDatabase::class.java,
                    getString(R.string.dbFileName)
                )
                .build()
            val remindersDao = db.remindersDao()
            val remindersInfo: List<ReminderInfo> = remindersDao.getReminders(getCurrentUsername())
            db.close()
            return remindersInfo
        }

        override fun onPostExecute(remindersInfo: List<ReminderInfo>?) {
            super.onPostExecute(remindersInfo)
            if (remindersInfo != null) {
                if (remindersInfo.isNotEmpty()) {
                    val adaptor = ReminderAdapter(applicationContext, remindersInfo, this@Reminders)
                    remindersListView.adapter = adaptor
                } else {
                    remindersListView.adapter = null
                    Toast.makeText(applicationContext, "No reminders now", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    // change reminder_seen by change Switch in the reminders list
    fun changeReminderSeen(position: Int) {
        val selectedReminder = remindersListView.adapter.getItem(position) as ReminderInfo

        selectedReminder.reminder_seen = !selectedReminder.reminder_seen

        AsyncTask.execute {
            val db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java, getString(R.string.dbFileName)
            ).build()

            val remindersDao = db.remindersDao()
            remindersDao.update(selectedReminder);
        }
    }
}