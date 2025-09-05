package com.friendat.utils // Oder wo du deine Hilfsfunktionen hast

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileLogger {

    private const val APP_CREATE_LOG_FILE = "app_create_log.txt"
    private const val WORKER_LOG_FILE = "worker_log.txt"
    private const val NETWORK_CALLBACK_LOG_FILE = "network_callback_log.txt"
    private const val WIFI_UTILS_LOG_FILE = "wifi_utils_log.txt" // Für getCurrentBssid

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    // Methode für allgemeine Logs, kann für verschiedene Dateien verwendet werden
    private fun appendLog(context: Context, fileName: String, tag: String, message: String) {
        try {
            val file = File(context.getExternalFilesDir(null), fileName) // App-spezifisches Verzeichnis
            val timestamp = dateFormat.format(Date())
            file.appendText("[$timestamp] $tag: $message\n")
        } catch (e: Exception) {
            // Fehler beim Schreiben ins Log ignorieren, da dies nur eine Debug-Hilfe ist
            // android.util.Log.e("FileLogger", "Error writing to $fileName", e) // Optional: Logcat-Eintrag über den Fehler
        }
    }

    fun logAppCreate(context: Context, message: String) {
        appendLog(context, APP_CREATE_LOG_FILE, "MyFriendatApplication", message)
    }

    fun logWorker(context: Context, tag: String, message: String) {
        appendLog(context, WORKER_LOG_FILE, tag, message)
    }

    fun logNetworkCallback(context: Context, tag: String, message: String) {
        appendLog(context, NETWORK_CALLBACK_LOG_FILE, tag, message)
    }

    fun logWifiUtils(context: Context, tag: String, message: String) {
        appendLog(context, WIFI_UTILS_LOG_FILE, tag, message)
    }

    // Optional: Eine Methode zum Löschen alter Logs beim App-Start (für saubere Tests)
    fun clearAllLogs(context: Context) {
        try {
            File(context.getExternalFilesDir(null), APP_CREATE_LOG_FILE).delete()
            File(context.getExternalFilesDir(null), WORKER_LOG_FILE).delete()
            File(context.getExternalFilesDir(null), NETWORK_CALLBACK_LOG_FILE).delete()
            File(context.getExternalFilesDir(null), WIFI_UTILS_LOG_FILE).delete()
        } catch (e: Exception) { /* ignore */ }
    }
}
