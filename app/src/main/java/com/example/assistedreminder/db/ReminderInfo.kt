package com.example.assistedreminder.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.assistedreminder.R
import java.sql.Time

@Entity(tableName = "remindersInfo")
data class ReminderInfo (
    @PrimaryKey(autoGenerate = true) var uid: Int?,
    @ColumnInfo(name = "message") var message: String,
    @ColumnInfo(name = "location_x") var location_x: Int,
    @ColumnInfo(name = "location_y") var location_y: Int,
    @ColumnInfo(name = "reminder_time") var reminder_time: String,
    @ColumnInfo(name = "reminder_date") var reminder_date: String,
    @ColumnInfo(name = "creation_time") var creation_time: String,
    @ColumnInfo(name = "creator_id") var creator_id: String,
    @ColumnInfo(name = "reminder_active") var reminder_active: Boolean,
    @ColumnInfo(name = "reminder_seen") var reminder_seen: Boolean
)