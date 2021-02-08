package com.example.assistedreminder

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class Reminder(
    var message: String,
    var location_x: Int,
    var location_y: Int,
    var creation_time: String)

class ReminderAdapter(
    private val context: Context,
    private val dataSource: ArrayList<Reminder>
) : BaseAdapter() {

    private val inflater: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getCount(): Int {
        return dataSource.size
    }

    override fun getItem(position: Int): Any {
        return dataSource[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val rowView = inflater.inflate(R.layout.reminder_item, parent, false)

        val messageTextView = rowView.findViewById(R.id.message) as TextView
        val locationXTextView = rowView.findViewById(R.id.location_x) as TextView
        val locationYTextView = rowView.findViewById(R.id.location_y) as TextView
        val timeTextView = rowView.findViewById(R.id.creation_time) as TextView

        val reminder = getItem(position) as Reminder

        messageTextView.text = reminder.message
        locationXTextView.text = reminder.location_x.toString()
        locationYTextView.text = reminder.location_y.toString()
        timeTextView.text = reminder.creation_time

        //Picasso.with(context).load(reminder.imageUrl).placeholder(R.mipmap.ic_launcher).into(thumbnailImageView)

        return rowView
    }
}