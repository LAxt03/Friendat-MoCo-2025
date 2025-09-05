package com.friendat.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.friendat.data.model.LastSentLocationStatus // Stelle sicher, dass das Modell korrekt ist
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

object LastStatusRepository {

    private const val PREFS_NAME = "FriendatLocationStatusPrefs"
    private const val KEY_LAST_STATUS = "last_sent_location_status"
    private val gson = Gson()

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveLastSentStatus(context: Context, status: LastSentLocationStatus) {
        try {
            val jsonStatus = gson.toJson(status)
            getPreferences(context).edit().putString(KEY_LAST_STATUS, jsonStatus).apply()
            Log.d("LastStatusRepo", "Saved last sent status: $status")
        } catch (e: Exception) {
            Log.e("LastStatusRepo", "Error saving last sent status", e)
        }
    }

    fun getLastSentStatus(context: Context): LastSentLocationStatus? {
        val jsonStatus = getPreferences(context).getString(KEY_LAST_STATUS, null)
        return if (jsonStatus != null) {
            try {
                gson.fromJson(jsonStatus, LastSentLocationStatus::class.java)
            } catch (e: JsonSyntaxException) {
                Log.e("LastStatusRepo", "Error parsing JSON for last status", e)
                null
            } catch (e: Exception) { // Breitere Fehlerbehandlung
                Log.e("LastStatusRepo", "Generic error loading last status", e)
                null
            }
        } else {
            null
        }
    }

    fun clearLastSentStatus(context: Context) {
        getPreferences(context).edit().remove(KEY_LAST_STATUS).apply()
        Log.d("LastStatusRepo", "Cleared last sent status.")
    }
}
