package com.example.assistedreminder

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import org.json.JSONObject

class Profile : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        showCurrentUserData()

        findViewById<Button>(R.id.profileBottomButtom).setOnClickListener {
            saveNewUserData()
        }
    }

    // Add data to TextView on layout
    private fun showCurrentUserData() {
        val currentUsername: String = getCurrentUsername()
        val currentUserData: Map<String, String> = getCurrentUserData(currentUsername)
        val username: String? = currentUserData["username"]
        val password: String? = currentUserData["password"]
        val name: String? = currentUserData["name"]
        val country: String? = currentUserData["country"]

        if (username != null && username.isNotBlank()) {
            findViewById<EditText>(R.id.profileUsername).setText(username)
        }

        if (password != null && password.isNotBlank()) {
            findViewById<EditText>(R.id.profilePassword).setText(password)
        }

        if (name != null && name.isNotBlank()) {
            findViewById<EditText>(R.id.profileName).setText(name)
        }

        if (country != null && country.isNotBlank()) {
            findViewById<EditText>(R.id.profileCountry).setText(country)
        }
    }

    // Retrieve Shared Preference data
    private fun getCurrentUserData(currentUsername: String?): Map<String, String> {
        val spName = getString(R.string.sharedPreference);
        val spMode = Context.MODE_PRIVATE

        val logsTable: Map<String, String> =
            applicationContext.getSharedPreferences(spName, spMode).all as Map<String, String>

        return toMap(logsTable[currentUsername] ?: "")
    }

    // Convert JSON string to Map<String,String>
    private fun toMap(jsonString: String): Map<String, String> {
        val jsonObject = JSONObject(jsonString)
        var mapResult: Map<String, String> = HashMap()

        val keysItr = jsonObject.keys()
        while (keysItr.hasNext()) {
            val key = keysItr.next()
            mapResult += mapOf(key to jsonObject[key].toString())
        }

        return mapResult
    }

    // Retrieve current username from Shared Preferences
    private fun getCurrentUsername(): String {
        val username = applicationContext.getSharedPreferences(
            getString(R.string.sharedPreference), Context.MODE_PRIVATE
        ).getString("currentUsername", "") ?: ""

        return username
    }

    // Save new data to Shared Preferences
    private fun saveNewUserData() {
        val spName = getString(R.string.sharedPreference);
        val spMode = Context.MODE_PRIVATE
        val currentUsername: String = getCurrentUsername()

        val newUsername: String = findViewById<EditText>(R.id.profileUsername).text.toString()
        val newPassword: String = findViewById<EditText>(R.id.profilePassword).text.toString()
        val newName: String = findViewById<EditText>(R.id.profileName).text.toString()
        val newCountry: String = findViewById<EditText>(R.id.profileCountry).text.toString()

        if (newUsername.isNotBlank() && newPassword.isNotBlank()) {
            var newCredentials: Map<String, String> = HashMap()
            newCredentials += mapOf(
                "username" to newUsername,
                "password" to newPassword,
                "name" to newName,
                "country" to newCountry
            )

            val jsonString = JSONObject(newCredentials).toString()

            applicationContext.getSharedPreferences(spName, spMode)
                .edit()
                .putString(currentUsername, jsonString)
                .apply()

            Toast.makeText(this, "Data saved", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Invalid data", Toast.LENGTH_SHORT).show()
        }
    }
}