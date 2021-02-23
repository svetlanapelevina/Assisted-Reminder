package com.example.assistedreminder

import android.annotation.SuppressLint
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import androidx.room.Room
import com.example.assistedreminder.db.AppDatabase
import com.example.assistedreminder.db.ReminderInfo
import java.util.*

class ReminderEdit : AppCompatActivity() {
    lateinit var timePicker: TimePickerHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminder_edit)

        findViewById<TextView>(R.id.reminderTopTitle).text = "Edit Reminder"
        findViewById<Button>(R.id.reminderBottomButton).text = "Save"

        retrieveReminderData()

        findViewById<Button>(R.id.reminderBottomButton).setOnClickListener {
            saveReminder()
        }

        timePicker = TimePickerHelper(this, true, true)

        findViewById<EditText>(R.id.reminderTime).setOnClickListener {
            showTimePickerDialog()
        }
    }

    // show TimePiker spinner for time input
    private fun showTimePickerDialog() {
        val cal = Calendar.getInstance()
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val m = cal.get(Calendar.MINUTE)
        timePicker.showDialog(h, m, object : TimePickerHelper.Callback {
            override fun onTimeSelected(hourOfDay: Int, minute: Int) {
                val hourStr = if (hourOfDay < 10) "0${hourOfDay}" else "$hourOfDay"
                val minuteStr = if (minute < 10) "0${minute}" else "$minute"

                findViewById<EditText>(R.id.reminderTime).setText("${hourStr} : ${minuteStr}")
            }
        })
    }

    // save reminder to DB
    private fun saveReminder() {
        val uid: String = getCurrentReminderUid()

        val message: String = findViewById<TextView>(R.id.reminderMessage).text.toString()
        val locationX: String =
            findViewById<TextView>(R.id.reminderLocationX).text.toString()
        val locationY: String =
            findViewById<TextView>(R.id.reminderLocationY).text.toString()
        val time: String = findViewById<TextView>(R.id.reminderTime).text.toString()

        if (message.isBlank()) {
            Toast.makeText(this, "Fill in message ", Toast.LENGTH_SHORT).show()
            return
        }

        if (time.isBlank() && (locationX.isBlank() || locationY.isBlank())) {
            Toast.makeText(this, "Fill in location or time ", Toast.LENGTH_SHORT).show()
            return
        }

        AsyncTask.execute {
            val db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java, getString(R.string.dbFileName)
            ).build()

            val remindersDao = db.remindersDao()
            val reminder: ReminderInfo = remindersDao.getReminderById(uid).get(0)

            if (message.isNotBlank()) {
                reminder.message = message
            }

            if (time.isNotBlank()) {
                reminder.reminder_time = time
            }

            reminder.location_x = if (locationX.isNotBlank()) locationX.toInt() else 0
            reminder.location_y = if (locationX.isNotBlank()) locationX.toInt() else 0
            remindersDao.update(reminder)
        }

        Toast.makeText(this, "Reminder saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    // get uid for current reminder
    private fun getCurrentReminderUid(): String {
        return (if (intent != null && intent.hasExtra("uid"))
            intent.getStringExtra("uid") else "")
            ?: ""
    }

    // retrieve reminder data from DB
    private fun retrieveReminderData() {
        val uid: String = getCurrentReminderUid()

        AsyncTask.execute {
            val db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java, getString(R.string.dbFileName)
            ).build()

            val remindersDao = db.remindersDao()
            val reminder: ReminderInfo = remindersDao.getReminderById(uid).get(0)

            findViewById<EditText>(R.id.reminderMessage).setText(reminder.message)
            if (reminder.location_x != 0 && reminder.location_y != 0) {
                findViewById<EditText>(R.id.reminderLocationX).setText(reminder.location_x.toString())
                findViewById<EditText>(R.id.reminderLocationY).setText(reminder.location_x.toString())
            }
            findViewById<EditText>(R.id.reminderTime).setText(reminder.reminder_time)
        }
    }
}