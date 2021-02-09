package com.example.assistedreminder

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject


class MainActivity() : AppCompatActivity() {
    var currentUsername: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        val spName = getString(R.string.sharedPreference);
        val spMode = Context.MODE_PRIVATE

        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        findViewById<Button>(R.id.registrationButton).setOnClickListener {
            startActivity(
                Intent(applicationContext, Registration::class.java)
            )
        }

        findViewById<Button>(R.id.loginButton).setOnClickListener {
            val username: String =
                findViewById<EditText>(R.id.editTextTextPersonName).text.toString()
            val password: String =
                findViewById<EditText>(R.id.editTextTextPassword).text.toString()

            // Validation of username and password
            if (username.isNotBlank() && password.isNotBlank()) {
                val credentials: Map<String, String> = getAllData()

                if (username !in credentials.keys) {
                    Toast.makeText(this, "This user doesn't exist", Toast.LENGTH_SHORT).show()
                } else {
                    if (isUserPasswordCorrect(username, password)) {
                        setCurrentUserAndStartActivity(username)
                        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Incorrect password for this username", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Fill in username and password", Toast.LENGTH_SHORT).show()
            }
        }

        checkLoginStatus()
    }

    override fun onResume() {
        super.onResume()
        checkLoginStatus()
    }

    // Dynamic login
    private fun checkLoginStatus() {
        val spName = getString(R.string.sharedPreference);
        val spMode = Context.MODE_PRIVATE

        val loginStatus = applicationContext
            .getSharedPreferences(spName, spMode)
            .getInt("LoginStatus", 0)

        if (loginStatus == 1) {
            startActivity(
                Intent(applicationContext, Reminders::class.java)
            )
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

    // Check if user password correct (True) or not (False)
    private fun isUserPasswordCorrect(username: String, password: String): Boolean {
        val data: Map<String, String> = getAllData()

        val userData = data[username]
        val currentUserCredentialsMap : Map<String, String> = toMap(userData?:"")

        return currentUserCredentialsMap["password"] == password
    }

    // Convert JSON string to Map<String,String>
    private fun toMap(jsonString:String):Map<String, String> {
        val jsonObject = JSONObject(jsonString)
        var mapResult: Map<String, String> = HashMap()

        val keysItr = jsonObject.keys()
        while (keysItr.hasNext()) {
            val key = keysItr.next()
            mapResult += mapOf(key to jsonObject[key].toString())
        }

        return mapResult
    }

    // Set current user and start Reminder Actiivty
    private fun setCurrentUserAndStartActivity(username:String) {
        val spName = getString(R.string.sharedPreference);
        val spMode = Context.MODE_PRIVATE

        currentUsername = username

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
            Intent(applicationContext, Reminders::class.java)
        )
    }
}