package com.example.assistedreminder

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.room.Room
import com.example.assistedreminder.db.AppDatabase
import com.example.assistedreminder.db.ReminderInfo
import com.google.android.material.timepicker.TimeFormat
import java.sql.Time
import java.time.LocalDateTime
import java.util.*

class ReminderAddNew : AppCompatActivity() {
    lateinit var timePicker: TimePickerHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminder_edit)

        findViewById<TextView>(R.id.reminderTopTitle).text = "Add New Reminder"
        findViewById<Button>(R.id.reminderBottomButton).text = "add new reminder"

        findViewById<Button>(R.id.reminderBottomButton).setOnClickListener {
            addNewReminder()
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

    // insert reminder to DB
    fun addNewReminder() {
        val message: String = findViewById<TextView>(R.id.reminderMessage).text.toString();
        val locationX: String = findViewById<TextView>(R.id.reminderLocationX).text.toString();
        val locationY: String = findViewById<TextView>(R.id.reminderLocationY).text.toString();
        val time: String = findViewById<TextView>(R.id.reminderTime).text.toString();

        if (message.isBlank()) {
            Toast.makeText(this, "Fill in message ", Toast.LENGTH_SHORT).show()
            return
        }

        if (time.isBlank() && (locationX.isBlank() || locationY.isBlank())) {
            Toast.makeText(this, "Fill in location or time ", Toast.LENGTH_SHORT).show()
            return
        }

        val creatorUsername: String = applicationContext.getSharedPreferences(
            getString(R.string.sharedPreference), Context.MODE_PRIVATE
        ).getString("currentUsername", "") ?: ""

        val reminder = ReminderInfo(
            null,
            message = message,
            location_x = if (locationX.isNotBlank()) locationX.toInt() else 0,
            location_y =  if (locationY.isNotBlank()) locationY.toInt() else 0,
            creation_time = java.util.Calendar.getInstance().toString(),
            reminder_time = time,
            reminder_seen = true,
            creator_id = creatorUsername
        )

        AsyncTask.execute {
            val db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java, getString(R.string.dbFileName)
            ).build()

            val remindersDao = db.remindersDao()
            remindersDao.insert(reminder);
        }

        Toast.makeText(this, "Reminder added", Toast.LENGTH_SHORT).show()

        finish()
    }
}