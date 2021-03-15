package com.example.assistedreminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.room.Room
import com.example.assistedreminder.db.AppDatabase
import com.example.assistedreminder.db.ReminderInfo
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import java.sql.Time
import java.time.LocalDateTime
import java.util.*

class GeofenceReceiver : BroadcastReceiver() {
    lateinit var key: String
    lateinit var uid: String

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null) {
            val geofencingEvent = GeofencingEvent.fromIntent(intent)
            val geofencingTransition = geofencingEvent.geofenceTransition

            if (geofencingTransition == Geofence.GEOFENCE_TRANSITION_ENTER || geofencingTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {
                // Retrieve data from intent
                if (intent != null) {
                    key = intent.getStringExtra("key")!!
                    uid = intent.getStringExtra("uid")!!
                }

                val firebase = Firebase.database
                val reference = firebase.getReference("reminders")

                val reminderListener = object : ValueEventListener {
                    @RequiresApi(Build.VERSION_CODES.O)
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val reminder = snapshot.getValue<ReminderInfo>()
                        if (reminder != null && reminder.reminder_active) {

                            // check time
                            if (!checkDateTime(reminder)) {
                                return
                            }

                            // show reminder
                            ReminderAddNew
                                .showNotification(
                                    context.applicationContext,
                                    "${reminder.message} \nLocation \nLat: ${reminder.location_x} - Lon: ${reminder.location_y}"
                                )

                            // change Room database
                            AsyncTask.execute {
                                val db = Room.databaseBuilder(
                                    context,
                                    AppDatabase::class.java, "remindersInfo"
                                )
                                    .fallbackToDestructiveMigration()
                                    .build()

                                val remindersDao = db.remindersDao()
                                val reminder1: ReminderInfo =
                                    remindersDao.getReminderById(uid)[0]
                                reminder1.reminder_seen = true
                                reminder1.reminder_active = false
                                remindersDao.update(reminder1)
                            }

                            //change Firebase database
                            val database = Firebase.database
                            val reference1 = database.getReference("reminders")
                            if (reminder.key.isNotBlank()) {
                                val firebaseReminder = reference1.child(reminder.key)
                                reminder.reminder_seen = true
                                reminder.reminder_active = false
                                firebaseReminder.setValue(reminder)
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        println("reminder:onCancelled: ${error.details}")
                    }
                }

                val child = reference.child(key)
                child.addValueEventListener(reminderListener)

                val triggeringGeofences = geofencingEvent.triggeringGeofences
                // remove geofence
                ReminderAddNew.removeGeofences(context, triggeringGeofences)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkDateTime(reminder: ReminderInfo) : Boolean {
        if (reminder.reminder_date.isNotBlank() && reminder.reminder_time.isNotBlank()) {
            val dateParts = reminder.reminder_date.split(".").toTypedArray()
            val timeParts = reminder.reminder_time.split(" : ").toTypedArray()

            val date = Date(dateParts[2].toInt(), dateParts[1].toInt(), dateParts[0].toInt())
            val time = Time(timeParts[0].toInt(), timeParts[1].toInt(), 0)
            val now = LocalDateTime.now()
            if (now.dayOfMonth != date.date || now.monthValue != date.month ||
                now.minute != time.minutes || now.hour != time.hours) {
                return false
            }
        }
        return true
    }
}