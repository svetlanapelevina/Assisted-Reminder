package com.example.assistedreminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.AsyncTask
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import androidx.room.Room
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.assistedreminder.db.AppDatabase
import com.example.assistedreminder.db.ReminderInfo
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class Reminders : AppCompatActivity() {
    private lateinit var remindersListView: ListView
    private var showAllReminders: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminders)

        remindersListView = findViewById(R.id.remindersList)
        refreshRemindersListView(showAllReminders)

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

        findViewById<ToggleButton>(R.id.showAllRemndersToggleButton).setOnCheckedChangeListener { _, isChecked ->
            showAllReminders = isChecked
            refreshRemindersListView(showAllReminders)
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
                        .fallbackToDestructiveMigration()
                        .build()
                    db.remindersDao().deleteReminder(selectedReminder.uid!!)
                }

                // cancel pending time based reminder
                cancelReminder(applicationContext, selectedReminder.uid!!)

                refreshRemindersListView(showAllReminders)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        refreshRemindersListView(showAllReminders)
    }

    // refresh list of reminders
    private fun refreshRemindersListView(showAll: Boolean) {
        val refreshTask = LoadRemindersInfoEntries(showAll)
        refreshTask.execute()
    }

    // get current username
    private fun getCurrentUsername(): String {
        return applicationContext.getSharedPreferences(
            getString(R.string.sharedPreference), Context.MODE_PRIVATE
        ).getString("currentUsername", "") ?: ""
    }

    // get reminders data from DB
    inner class LoadRemindersInfoEntries(showAll: Boolean) :
        AsyncTask<String?, String?, List<ReminderInfo>>() {
        private val showAllReminders: Boolean = showAll

        override fun doInBackground(vararg params: String?): List<ReminderInfo> {
            val db = Room
                .databaseBuilder(
                    applicationContext,
                    AppDatabase::class.java,
                    getString(R.string.dbFileName)
                )
                .fallbackToDestructiveMigration()
                .build()
            val remindersDao = db.remindersDao()
            var remindersInfo: List<ReminderInfo>? = null
            if (showAllReminders) {
                remindersInfo = remindersDao.getAllReminders(getCurrentUsername())
            } else {
                remindersInfo = remindersDao.getSeenReminders(getCurrentUsername())
            }
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

    // change reminder_active by change Switch in the reminders list
    fun changeReminderSeen(position: Int) {
        val selectedReminder = remindersListView.adapter.getItem(position) as ReminderInfo

        if (selectedReminder.reminder_seen) {
            Toast.makeText(this, "You can\'t activate past reminder ", Toast.LENGTH_SHORT).show()
            return
        }

        selectedReminder.reminder_active = !selectedReminder.reminder_active

        AsyncTask.execute {
            val db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java, getString(R.string.dbFileName)
            )
                .fallbackToDestructiveMigration()
                .build()

            val remindersDao = db.remindersDao()
            remindersDao.update(selectedReminder);
        }

        // add or delete notification job
        if (selectedReminder.reminder_active) {
            createNotificationJob(
                selectedReminder.reminder_date,
                selectedReminder.reminder_time,
                selectedReminder.uid!!,
                selectedReminder.message
            )
            Toast.makeText(this, "Reminder is on", Toast.LENGTH_SHORT).show()
        } else {
            cancelReminder(applicationContext, selectedReminder.uid!!)
            Toast.makeText(this, "Reminder is off", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        //val paymenthistoryList = mutableListOf<PaymentInfo>()

        fun showNofitication(context: Context, message: String) {
            val CHANNEL_ID = "REMINDER_APP_NOTIFICATION_CHANNEL"
            var notificationId = Random.nextInt(10, 1000) + 5
            // notificationId += Random(notificationId).nextInt(1, 500)

            var notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setGroup(CHANNEL_ID)

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Notification chancel needed since Android 8
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = context.getString(R.string.app_name)
                }
                notificationManager.createNotificationChannel(channel)
            }

            notificationManager.notify(notificationId, notificationBuilder.build())
        }

        fun setReminderWithWorkManager(
            context: Context,
            uid: Int,
            timeInMillis: Long,
            message: String
        ) {

            val reminderParameters = Data.Builder()
                .putString("message", message)
                .putInt("uid", uid)
                .build()

            // get minutes from now until reminder
            var minutesFromNow = 0L
            if (timeInMillis > System.currentTimeMillis())
                minutesFromNow = timeInMillis - System.currentTimeMillis()

            val reminderRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInputData(reminderParameters)
                .setInitialDelay(minutesFromNow, TimeUnit.MILLISECONDS)
                .addTag(uid.toString()) // add tag to cancel it
                .build()

            WorkManager.getInstance(context).enqueue(reminderRequest)
        }

        // cancel WorkManager job by tag
        fun cancelReminder(context: Context, uid: Int) {
            WorkManager.getInstance(context).cancelAllWorkByTag(uid.toString())
        }
    }

    fun createNotificationJob(date: String, time: String, uuid: Int, message: String) {
        if (date.isNotBlank()) {
            //convert date  string value to Date format using dd.mm.yyyy
            // here it is assumed that date is in dd.mm.yyyy
            val dateparts = date.split(".").toTypedArray()
            val timeparts = time.split(" : ").toTypedArray()
            val paymentCalender = GregorianCalendar(
                dateparts[2].toInt(),
                dateparts[1].toInt() - 1,
                dateparts[0].toInt(),
                timeparts[0].toInt(),
                timeparts[1].toInt()
            )

            //set reminder
            setReminderWithWorkManager(
                applicationContext,
                uuid,
                paymentCalender.timeInMillis,
                message
            )
        }
    }
}