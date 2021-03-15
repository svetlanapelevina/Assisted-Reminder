package com.example.assistedreminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.example.assistedreminder.db.AppDatabase
import com.example.assistedreminder.db.ReminderInfo
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.*

class ReminderEdit : AppCompatActivity() {
    lateinit var timePicker: TimePickerHelper
    lateinit var datePicker: DatePickerHelper
    var MY_REQUEST_CODE: Int = 1

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geofencingClient: GeofencingClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminder_edit)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        geofencingClient = LocationServices.getGeofencingClient(this)

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

        findViewById<EditText>(R.id.reminderLocation).setOnClickListener {
            startActivityForResult(
                Intent(applicationContext, MapActivity::class.java)
                    .putExtra("uid", getCurrentReminderUid()),
                MY_REQUEST_CODE
            )
        }
        findViewById<EditText>(R.id.reminderLocation).isFocusable = false
    }

    // get location after map activity and set it to the EditText
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == MY_REQUEST_CODE) {
                if (data != null)
                    findViewById<EditText>(R.id.reminderLocation).setText(data.getStringExtra("location"));
            }
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
        val location: String =
            findViewById<TextView>(R.id.reminderLocation).text.toString()
        val time: String = findViewById<TextView>(R.id.reminderTime).text.toString()
        val date: String = findViewById<TextView>(R.id.reminderDate).text.toString();

        if (message.isBlank()) {
            Toast.makeText(this, "Fill in message ", Toast.LENGTH_SHORT).show()
            return
        }

        if ((time.isBlank() || date.isBlank()) && (location.isBlank())) {
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

            reminder.reminder_active = true
            reminder.reminder_seen = false

            val locationParts =
                if (location.isNotBlank()) location.split(" ").toTypedArray() else null
            reminder.location_x =
                if (locationParts != null && locationParts[0].isNotBlank()) locationParts[0].toDouble() else 0.0
            reminder.location_y =
                if (locationParts != null && locationParts[1].isNotBlank()) locationParts[1].toDouble() else 0.0
            remindersDao.update(reminder)

            deleteOldNotificationJob(uid.toInt())
            ReminderAddNew.removeGeofence(applicationContext, reminder.key)

            if (reminder.reminder_active) {
                createNotificationJob(
                    date,
                    time,
                    uid.toInt(),
                    message,
                    reminder.location_x,
                    reminder.location_y,
                    reminder.key
                )
            }

            val database = Firebase.database
            val reference = database.getReference("reminders")
            if (reminder.key.isNotBlank()) {
                val firebaseReminder = reference.child(reminder.key)
                firebaseReminder.setValue(reminder)
            }
        }

        Toast.makeText(this, "Reminder saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    // create notification
    private fun createNotificationJob(date: String, time: String, uuid: Int, message: String,
        locationX: Double, locationY: Double, key: String) {

        val locationSet = locationX != 0.0 && locationY != 0.0

        if (date.isNotBlank() && !locationSet) {
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

        if (locationSet) {
            ReminderAddNew.removeGeofence(applicationContext, key)
            val position = LatLng(locationX, locationY)
            ReminderAddNew.createGeoFence(
                applicationContext,
                this@ReminderEdit,
                position,
                key,
                uuid.toString(),
                geofencingClient
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
    @SuppressLint("SetTextI18n")
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
            if (reminder.location_x != 0.0 && reminder.location_y != 0.0) {
                findViewById<EditText>(R.id.reminderLocation).setText("${reminder.location_x} ${reminder.location_y}")
            }
            findViewById<EditText>(R.id.reminderTime).setText(reminder.reminder_time)
            findViewById<EditText>(R.id.reminderDate).setText(reminder.reminder_date)
        }
    }
}