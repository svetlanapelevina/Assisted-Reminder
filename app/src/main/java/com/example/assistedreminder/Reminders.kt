package com.example.assistedreminder

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.room.Room
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.assistedreminder.db.AppDatabase
import com.example.assistedreminder.db.ReminderInfo
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class Reminders : AppCompatActivity() {
    private lateinit var remindersListView: ListView
    private var showAllReminders: Boolean = false

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var geofencingClient: GeofencingClient
    var MY_REQUEST_CODE: Int = 1

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

        findViewById<Button>(R.id.fakeGPS).setOnClickListener {
            startActivityForResult(
                Intent(applicationContext, MapActivity::class.java),
                MY_REQUEST_CODE
            )
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

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        geofencingClient = LocationServices.getGeofencingClient(this)
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

                refreshRemindersListView(showAllReminders)

                // delete from Firebase database
                val database = Firebase.database
                val reference1 = database.getReference("reminders")
                if (selectedReminder.key.isNotBlank()) {
                    reference1.child(selectedReminder.key).removeValue()
                }

                // cancel pending time based reminder
                cancelReminder(applicationContext, selectedReminder.uid!!)
                ReminderAddNew.removeGeofence(applicationContext, selectedReminder.key)
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
                }
            } else {
                remindersListView.adapter = null
                Toast.makeText(applicationContext, "No reminders now", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // change reminder_active by change Switch in the reminders list
    fun changeReminderSeen(position: Int) {
        val selectedReminder = remindersListView.adapter.getItem(position) as ReminderInfo

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

        val database = Firebase.database
        val reference = database.getReference("reminders")
        if (selectedReminder.key.isNotBlank()) {
            val firebaseReminder = reference.child(selectedReminder.key)
            firebaseReminder.setValue(selectedReminder)
        }

        // add or delete notification job
        if (selectedReminder.reminder_active) {

            if (selectedReminder.location_x == 0.0) {
                createNotificationJob(
                    selectedReminder.reminder_date,
                    selectedReminder.reminder_time,
                    selectedReminder.uid!!,
                    selectedReminder.message
                )
            } else {
                val positionLatLng = LatLng(selectedReminder.location_x, selectedReminder.location_y)
                ReminderAddNew.createGeoFence(
                    applicationContext,
                    this@Reminders,
                    positionLatLng,
                    selectedReminder.key,
                    selectedReminder.uid.toString(),
                    geofencingClient
                )
            }

            Toast.makeText(this, "Reminder is on", Toast.LENGTH_SHORT).show()
        } else {
            cancelReminder(applicationContext, selectedReminder.uid!!)

            ReminderAddNew.removeGeofence(applicationContext, selectedReminder.key)

            Toast.makeText(this, "Reminder is off", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
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

    // get location after map activity and set it to the EditText
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == MY_REQUEST_CODE) {
                val locationParts = data?.getStringExtra("location")?.split(" ")
                val x = locationParts?.get(0)?.toDouble()
                val y = locationParts?.get(1)?.toDouble()

                if (isMockLocationEnabled()) {
                    setMock(x!!, y!!)
                }
            }
        }
    }

    private fun isMockLocationEnabled(): Boolean {
        return true
    }

    // create mock location by the coordinates
    private fun setMock(latitude: Double, longitude: Double) {
        var mLocationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (!mLocationManager.isLocationEnabled) {
            mLocationManager.addTestProvider(
                LocationManager.GPS_PROVIDER,
                false,
                false,
                false,
                false,
                true,
                true,
                true,
                0,
                3
            )
            mLocationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);
        }

        val mockLocation = Location(LocationManager.GPS_PROVIDER)
        mockLocation.latitude = latitude
        mockLocation.longitude = longitude
        mockLocation.accuracy = 3f
        mockLocation.time = System.currentTimeMillis()
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            mockLocation.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }

        mLocationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, mockLocation)
    }

}