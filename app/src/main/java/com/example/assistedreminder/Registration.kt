package com.example.assistedreminder

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject

class Registration : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        findViewById<TextView>(R.id.profileHeader).setText("Registration")
        findViewById<Button>(R.id.profileBottomButtom).setText("Log up")

        findViewById<Button>(R.id.profileBottomButtom).setOnClickListener {
            saveUserAndStartNewActivity()
        }
    }

    // Get all Shared Preference data
    private fun getAllData(): MutableMap<String, String> {
        val spName = getString(R.string.sharedPreference);
        val spMode = Context.MODE_PRIVATE

        val logsTable = applicationContext.getSharedPreferences(
            spName,
            spMode
        ).all as MutableMap<String, String>

        return logsTable
    }

    // Get all Shared Preference data
    private fun loadProfileData(): Map<String, String> {
        val spName = getString(R.string.sharedPreference);
        val spMode = Context.MODE_PRIVATE

        val logsTable = applicationContext.getSharedPreferences(
            spName,
            spMode
        ).all as MutableMap<String, String>
        return logsTable
    }

    // Check is User data corrent and save to Shared Preferene if true
    private fun saveUserAndStartNewActivity() {
        val spName = getString(R.string.sharedPreference);
        val spMode = Context.MODE_PRIVATE

        val username: String =
            findViewById<EditText>(R.id.profileUsername).text.toString()
        val password: String =
            findViewById<EditText>(R.id.profilePassword).text.toString()
        val name: String =
            findViewById<EditText>(R.id.profileName).text.toString()
        val country: String =
            findViewById<EditText>(R.id.profileCountry).text.toString()

        if (username.isNotBlank() && password.isNotBlank()) {
            var credentials: Map<String, String> = loadProfileData()

            if (username in credentials.keys) {
                Toast.makeText(this, "User with this username already exist", Toast.LENGTH_SHORT)
                    .show()
            } else {
                credentials += mapOf(
                    "username" to username,
                    "password" to password,
                    "name" to name,
                    "country" to country
                )
                saveMap(username, credentials)

                applicationContext
                    .getSharedPreferences(spName, spMode)
                    .edit()
                    .putInt("LoginStatus", 1)
                    .apply()

                applicationContext
                    .getSharedPreferences(spName, spMode)
                    .edit()
                    .putString("currentUsername", username)
                    .apply()

                startActivity(
                    Intent(applicationContext, Reminders::class.java).putExtra("username", username)
                )

                Toast.makeText(this, "User created", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Fill in username and password", Toast.LENGTH_SHORT).show()
        }
    }

    // save data to Shared Preferences
    private fun saveMap(username: String, userData: Map<String, String>) {
        val spName = getString(R.string.sharedPreference)
        val spMode = Context.MODE_PRIVATE

        val jsonString = JSONObject(userData).toString()

        applicationContext.getSharedPreferences(spName, spMode)
            .edit()
            .putString(username, jsonString)
            .apply()
    }
}