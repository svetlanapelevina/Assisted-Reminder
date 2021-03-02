package com.example.assistedreminder

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Switch
import android.widget.TextView
import com.example.assistedreminder.db.ReminderInfo

class ReminderAdapter(context: Context, private val dataSource: List<ReminderInfo>, listener: Reminders) : BaseAdapter() {
    private val listener = listener

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
        val timeTextView = rowView.findViewById(R.id.time) as TextView
        val dateTextView = rowView.findViewById(R.id.date) as TextView
        val seenSwitch = rowView.findViewById(R.id.seen) as Switch

        val reminder = getItem(position) as ReminderInfo

        messageTextView.text = reminder.message

        timeTextView.text = reminder.reminder_time
        dateTextView.text = reminder.reminder_date
        seenSwitch.isChecked = reminder.reminder_active
        seenSwitch.isEnabled = !reminder.reminder_seen

        // change reminder_active by clicking on Switch
        rowView.findViewById<View>(R.id.seen).setOnClickListener {
            listener.changeReminderSeen(position)
        }

        return rowView
    }


}