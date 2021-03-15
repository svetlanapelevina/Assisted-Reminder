package com.example.assistedreminder

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.example.assistedreminder.db.AppDatabase
import com.example.assistedreminder.db.ReminderInfo
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.sql.Time
import java.time.LocalDateTime
import java.util.*
import kotlin.random.Random

class ReminderAddNew : AppCompatActivity() {
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

        findViewById<TextView>(R.id.reminderTopTitle).text = "Add New Reminder"
        findViewById<Button>(R.id.reminderBottomButton).text = "add new reminder"

        findViewById<Button>(R.id.reminderBottomButton).setOnClickListener {
            addNewReminder()
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
                Intent(applicationContext, MapActivity::class.java),
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

    // show TimePicker spinner for time input
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

    // insert reminder to DB
    fun addNewReminder() {
        val message: String = findViewById<EditText>(R.id.reminderMessage).text.toString();
        val location: String = findViewById<EditText>(R.id.reminderLocation).text.toString();
        val time: String = findViewById<TextView>(R.id.reminderTime).text.toString();
        val date: String = findViewById<TextView>(R.id.reminderDate).text.toString();

        if (message.isBlank()) {
            Toast.makeText(this, "Fill in message ", Toast.LENGTH_SHORT).show()
            return
        }

        if ((time.isBlank() || date.isBlank()) && (location.isBlank())) {
            Toast.makeText(this, "Fill in location or time ", Toast.LENGTH_SHORT).show()
            return
        }

        val creatorUsername: String = applicationContext.getSharedPreferences(
            getString(R.string.sharedPreference), Context.MODE_PRIVATE
        ).getString("currentUsername", "") ?: ""

        val locationParts = if (location.isNotBlank()) location.split(" ").toTypedArray() else null

        val locationX = locationParts?.get(0)?.toDouble() ?: 0.0
        val locationY = locationParts?.get(1)?.toDouble() ?: 0.0

        val reminder = ReminderInfo(
            null,
            key = "",
            message = message,
            location_x = locationX,
            location_y = locationY,
            creation_time = java.util.Calendar.getInstance().toString(),
            reminder_time = time,
            reminder_date = date,
            reminder_active = true,
            creator_id = creatorUsername,
            reminder_seen = false
        )

        // add data to the firebase db
        val database = Firebase.database
        val reference = database.getReference("reminders")
        val key = reference.push().key
        reminder.key = key!!
        reference.child(key).setValue(reminder)


        // add reminder to Room db and create notification
        AsyncTask.execute {
            val db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java, getString(R.string.dbFileName)
            )
                .fallbackToDestructiveMigration()
                .build()

            val remindersDao = db.remindersDao()
            val uuid = remindersDao.insert(reminder).toInt()

            createNotificationJob(date, time, uuid, message, locationX, locationY, key)
        }

        Toast.makeText(this, "Reminder added", Toast.LENGTH_SHORT).show()

        finish()
    }

    // create notification
    private fun createNotificationJob(
        date: String,
        time: String,
        uuid: Int,
        message: String,
        locationX: Double,
        locationY: Double,
        key: String
    ) {
        val locationSet = locationX != 0.0 && locationY != 0.0

        if (date.isNotBlank() && !locationSet) {
            //convert date  string value to Date format using dd.mm.yyyy
            // here it is assumed that date is in dd.mm.yyyy
            val dateparts = date.split(".").toTypedArray()
            val timeparts = if (time != "") time.split(" : ").toTypedArray() else null
            val paymentCalender = GregorianCalendar(
                dateparts[2].toInt(),
                dateparts[1].toInt() - 1,
                dateparts[0].toInt(),
                if (timeparts != null) timeparts[0].toInt() else 0,
                if (timeparts != null) timeparts[1].toInt() else 0
            )

            //set reminder
            Reminders.setReminderWithWorkManager(
                applicationContext,
                uuid,
                paymentCalender.timeInMillis,
                message
            )
        }

        // TODO change geofence object and add data
        if (locationSet) {
            val position = LatLng(locationX, locationY)
            createGeoFence(applicationContext, this@ReminderAddNew,  position, key, uuid.toString(), geofencingClient)
        }
    }

    companion object {
        fun createGeoFence(
            context: Context,
            acivity: Activity,
            location: LatLng,
            key: String,
            uid: String,
            geofencingClient: GeofencingClient
        ) {
            val geofence = Geofence.Builder()
                .setRequestId(GEOFENCE_ID)
                .setCircularRegion(location.latitude, location.longitude, GEOFENCE_RADIUS.toFloat())
                .setExpirationDuration(GEOFENCE_EXPIRATION.toLong())
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_DWELL)
                .setLoiteringDelay(GEOFENCE_DWELL_DELAY)
                .build()

            val geofenceRequest = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build()

            val intent = Intent(context, GeofenceReceiver::class.java)
                .putExtra("key", key)
                .putExtra("uid", uid)

            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        acivity,
                        arrayOf(
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        ),
                        GEOFENCE_LOCATION_REQUEST_CODE
                    )
                } else {
                    geofencingClient.addGeofences(geofenceRequest, pendingIntent)
                }
            } else {
                geofencingClient.addGeofences(geofenceRequest, pendingIntent)
            }
        }

        fun removeGeofences(context: Context, triggeringGeofenceList: MutableList<Geofence>) {
            val geofenceIdList = mutableListOf<String>()
            for (entry in triggeringGeofenceList) {
                geofenceIdList.add(entry.requestId)
            }
            LocationServices.getGeofencingClient(context).removeGeofences(geofenceIdList)
        }

        fun removeGeofence(context: Context, key: String) {
            LocationServices.getGeofencingClient(context).removeGeofences(listOf(key))
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun showNotification(context: Context?, message: String) {
            val CHANNEL_ID = "REMINDER_NOTIFICATION_CHANNEL"
            var notificationId = 1515
            notificationId += Random(notificationId).nextInt(1, 30)

            val notificationBuilder =
                NotificationCompat.Builder(context!!.applicationContext, CHANNEL_ID)
                    .setSmallIcon(R.drawable.baseline)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setContentText(message)
                    .setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText(message)
                    )
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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
    }
}