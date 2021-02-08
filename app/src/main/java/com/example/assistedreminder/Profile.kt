package com.example.assistedreminder

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class Profile : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val spName = getString(R.string.sharedPreference);
        val spMode = Context.MODE_PRIVATE

        val currentUsername : String? = applicationContext.getSharedPreferences(spName, spMode)
            .getString("username", "")

        val currentPassword : String? =  applicationContext.getSharedPreferences(spName, spMode)
            .getString("password", "")

        if (currentUsername != null && currentUsername.isNotBlank()) {
            findViewById<EditText>(R.id.profileUsername).setText(currentUsername.toString())
        }

        if (currentPassword != null && currentPassword.isNotBlank()) {
            findViewById<EditText>(R.id.profilePassword).setText(currentPassword.toString())
        }

        findViewById<Button>(R.id.profileBottomButtom).setOnClickListener {
            val newUsername : String = findViewById<EditText>(R.id.profileUsername).text.toString()
            val newPassword : String = findViewById<EditText>(R.id.profilePassword).text.toString()

            if (newUsername.isNotBlank() && newPassword.isNotBlank()) {
                applicationContext.getSharedPreferences(spName, spMode)
                    .edit()
                    .putString("username", newUsername)
                    .putString("password", newPassword)
                    .apply()

                Toast.makeText(this, "Data saved", Toast.LENGTH_SHORT).show()

            } else {
                Toast.makeText(this, "Invalid data", Toast.LENGTH_SHORT).show()
            }
        }
    }
}