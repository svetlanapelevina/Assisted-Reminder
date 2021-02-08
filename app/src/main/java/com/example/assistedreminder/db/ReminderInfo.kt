package com.example.assistedreminder.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.assistedreminder.R

@Entity(tableName = "remindersInfo")
data class ReminderInfo (
    @PrimaryKey(autoGenerate = true) var uid: Int?,
    @ColumnInfo(name = "message") val message: String,
    @ColumnInfo(name = "location_x") var location_x: Int,
    @ColumnInfo(name = "location_y") var location_y: Int,
    @ColumnInfo(name = "creation_time") var creation_time: String
)