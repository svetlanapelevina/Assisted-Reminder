package com.example.assistedreminder

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.example.assistedreminder.db.AppDatabase
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

            if (username.isNotBlank() && password.isNotBlank()) {

                var credentials: Map<String, String> = getAllData()

                if (username !in credentials.keys) {
                    Toast.makeText(this, "This user doesn't exist", Toast.LENGTH_SHORT).show()
                } else {
                    if (checkPassword(username, password)) {
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
                            Intent(applicationContext, Reminders::class.java).putExtra("username", currentUsername)
                        )

                        Toast.makeText(this, getString(R.string.login), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(
                            this,
                            "Incorrect password for this username",
                            Toast.LENGTH_SHORT
                        ).show()
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

    private fun checkLoginStatus() {
        val spName = getString(R.string.sharedPreference);
        val spMode = Context.MODE_PRIVATE

        val loginStatus = applicationContext
            .getSharedPreferences(spName, spMode)
            .getInt("LoginStatus", 0)

        if (loginStatus == 1) {
            startActivity(Intent(applicationContext, Reminders::class.java).putExtra("username", currentUsername))
        }
    }

    private fun getAllData(): MutableMap<String, String> {
        val spName = getString(R.string.sharedPreference);
        val spMode = Context.MODE_PRIVATE

        val logsTable = applicationContext.getSharedPreferences(spName, spMode).all as MutableMap<String, String>
        return logsTable
    }

    private fun checkPassword(username: String, password: String): Boolean {
        var data: Map<String, String> = getAllData()

        var userData = data[username]

        val credentials = JSONObject(userData)

        var credentialsMap: Map<String, String> = HashMap()

        val keysItr = credentials.keys()
        while (keysItr.hasNext()) {
            val key = keysItr.next()
            credentialsMap += mapOf(key to credentials[key].toString())
        }

        return credentialsMap["password"] == password
    }
}