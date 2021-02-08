package com.example.assistedreminder

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.AsyncTask
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


class MainActivity : AppCompatActivity() {

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

                var credentials: Map<String, String> = loadProfileData()

                if (username !in credentials.keys) {
                    Toast.makeText(this, "This user doesn't exist", Toast.LENGTH_SHORT).show()
                } else {
                    if (credentials[username].equals(password)) {
                        applicationContext
                            .getSharedPreferences(spName, spMode)
                            .edit()
                            .putInt("LoginStatus", 1)
                            .apply()

                        startActivity(
                            Intent(applicationContext, Reminders::class.java)
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
            startActivity(Intent(applicationContext, Reminders::class.java))
        }
    }

    private fun loadProfileData(): Map<String, String> {
        val spName = getString(R.string.sharedPreference);
        val spMode = Context.MODE_PRIVATE

        var credentialsMap: Map<String, String> = HashMap()

        val credentialsJSON = applicationContext.getSharedPreferences(spName, spMode)
            .getString(
                "credentials", JSONObject()
                    .toString()
            )

        val credentials = JSONObject(credentialsJSON)

        val keysItr = credentials.keys()
        while (keysItr.hasNext()) {
            val key = keysItr.next()
            credentialsMap += mapOf(key to credentials[key].toString())
        }

        Toast.makeText(this, JSONObject(credentialsMap).toString(), Toast.LENGTH_SHORT).show()

        return credentialsMap
    }

    private fun saveMap(inputMap: Map<String, String>) {
        val spName = getString(R.string.sharedPreference);
        val spMode = Context.MODE_PRIVATE

        val jsonString = JSONObject(inputMap).toString()

        applicationContext.getSharedPreferences(spName, spMode)
            .edit()
            .remove("credentials")
            .apply()

        applicationContext.getSharedPreferences(spName, spMode)
            .edit()
            .putString("credentials", jsonString)
            .apply()
    }
}