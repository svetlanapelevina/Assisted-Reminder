package com.example.assistedreminder

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
    lateinit var datePicker: DatePickerHelper

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
        datePicker = DatePickerHelper(this, true)

        findViewById<EditText>(R.id.reminderTime).setOnClickListener {
            showTimePickerDialog()
        }

        findViewById<EditText>(R.id.reminderDate).setOnClickListener {
            showDatePickerDialog()
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

    // show DatePicker spinner for date input
    private fun showDatePickerDialog() {
        val cal = Calendar.getInstance()
        val d = cal.get(Calendar.DAY_OF_MONTH)
        val m = cal.get(Calendar.MONTH)
        val y = cal.get(Calendar.YEAR)
        datePicker.showDialog(d, m, y, object : DatePickerHelper.Callback {
            override fun onDateSelected(dayofMonth: Int, month: Int, year: Int) {
                val dayStr = if (dayofMonth < 10) "0${dayofMonth}" else "${dayofMonth}"
                val mon = month + 1
                val monthStr = if (mon < 10) "0${mon}" else "${mon}"

                findViewById<EditText>(R.id.reminderDate).setText("${dayStr}.${monthStr}.${year}")
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
        val date: String = findViewById<TextView>(R.id.reminderDate).text.toString();

        if (message.isBlank()) {
            Toast.makeText(this, "Fill in message ", Toast.LENGTH_SHORT).show()
            return
        }

        if ((time.isBlank() || date.isBlank()) && (locationX.isBlank() || locationY.isBlank())) {
            Toast.makeText(this, "Fill in location or time ", Toast.LENGTH_SHORT).show()
            return
        }

        AsyncTask.execute {
            val db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java, getString(R.string.dbFileName)
            )
                .fallbackToDestructiveMigration()
                .build()

            val remindersDao = db.remindersDao()
            val reminder: ReminderInfo = remindersDao.getReminderById(uid).get(0)

            if (message.isNotBlank()) {
                reminder.message = message
            }

            if (time.isNotBlank()) {
                reminder.reminder_time = time
                reminder.reminder_date = date
            }

            reminder.location_x = if (locationX.isNotBlank()) locationX.toInt() else 0
            reminder.location_y = if (locationX.isNotBlank()) locationX.toInt() else 0
            remindersDao.update(reminder)

            deleteOldNotificationJob(uid.toInt())

            if (reminder.reminder_active) {
                createNotificationJob(date, time, uid.toInt(), message)
            }
        }

        Toast.makeText(this, "Reminder saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    // create notification
    private fun createNotificationJob(date: String, time: String, uuid: Int, message: String) {
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
            Reminders.setReminderWithWorkManager(
                applicationContext,
                uuid,
                paymentCalender.timeInMillis,
                message
            )
        }
    }

    private fun deleteOldNotificationJob(uid: Int) {
        Reminders.cancelReminder(applicationContext, uid)
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
            )
                .fallbackToDestructiveMigration()
                .build()

            val remindersDao = db.remindersDao()
            val reminder: ReminderInfo = remindersDao.getReminderById(uid).get(0)

            findViewById<EditText>(R.id.reminderMessage).setText(reminder.message)
            if (reminder.location_x != 0 && reminder.location_y != 0) {
                findViewById<EditText>(R.id.reminderLocationX).setText(reminder.location_x.toString())
                findViewById<EditText>(R.id.reminderLocationY).setText(reminder.location_x.toString())
            }
            findViewById<EditText>(R.id.reminderTime).setText(reminder.reminder_time)
            findViewById<EditText>(R.id.reminderDate).setText(reminder.reminder_date)
        }
    }
}