package com.example.assistedreminder

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject

class Registration : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val spName = getString(R.string.sharedPreference);
        val spMode = Context.MODE_PRIVATE

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        findViewById<TextView>(R.id.profileHeader).setText("Registration")
        findViewById<Button>(R.id.profileBottomButtom).setText("Log up")


        findViewById<Button>(R.id.profileBottomButtom).setOnClickListener {
            val username: String =
                findViewById<EditText>(R.id.profileUsername).text.toString()
            val password: String =
                findViewById<EditText>(R.id.profilePassword).text.toString()

            if (username.isNotBlank() && password.isNotBlank()) {
                var credentials: Map<String, String> = loadProfileData()

                if (username in credentials.keys) {
                    Toast.makeText(this, "User with this username already exist", Toast.LENGTH_SHORT).show()
                } else {
                    credentials += mapOf(username to password)
                    saveMap(credentials)

                    applicationContext
                        .getSharedPreferences(spName, spMode)
                        .edit()
                        .putInt("LoginStatus", 1)
                        .apply()

                    startActivity(
                        Intent(applicationContext, Reminders::class.java)
                    )

                    Toast.makeText(this, "User created", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Fill in username and password", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadProfileData(): Map<String, String>{
        val spName = getString(R.string.sharedPreference);
        val spMode = Context.MODE_PRIVATE

        val credentialsMap: Map<String, String> = HashMap()

        val credentialsJSON = applicationContext.getSharedPreferences(spName, spMode)
            .getString("credentials", JSONObject().toString())

        val credentials = JSONObject(credentialsJSON)
        val keysItr = credentials.keys()
        while (keysItr.hasNext()) {
            val key = keysItr.next()
            credentialsMap[key]
        }

        Toast.makeText(this, JSONObject(credentialsMap).toString(), Toast.LENGTH_SHORT).show()

        return credentialsMap
    }

    private fun saveMap(inputMap: Map<String, String>) {
        val spName = getString(R.string.sharedPreference)
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