package com.friendat.data.sources.local // Oder dein entsprechendes Paket

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration // Wichtig für Migrationen
import androidx.sqlite.db.SupportSQLiteDatabase // Wichtig für Migrationen
import com.friendat.data.model.FriendStatus   // <<< IMPORTIEREN (deine neue Entity)
import com.friendat.data.model.WifiLocation
import com.friendat.data.sources.local.dao.FriendStatusDao // <<< IMPORTIEREN (dein neues DAO)
import com.friendat.data.sources.local.dao.WifiLocationDao

// --- MIGRATION DEFINIEREN ---
// Migration von Version 1 (nur WifiLocation) auf Version 2 (WifiLocation + FriendStatus)
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // SQL-Befehl zum Erstellen der neuen Tabelle friend_received_status
        // MIT den Feldern iconId und colorHex
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `friend_received_status` (" +
                    "`friendId` TEXT NOT NULL, " +
                    "`locationName` TEXT, " +
                    "`bssid` TEXT, " +
                    "`isOnline` INTEGER NOT NULL, " +
                    "`iconId` TEXT, " +                 // Spalte für Icon ID
                    "`colorHex` TEXT, " +               // Spalte für Farb-Hex
                    "`lastUpdateTimestamp` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`friendId`))"
        )
    }
}

@Database(
    entities = [WifiLocation::class, FriendStatus::class], // <<< FriendStatus::class hinzugefügt
    version = 2, // <<< Version auf 2 erhöht
    exportSchema = false // Kann für den Anfang so bleiben, für Produktion später auf true und Schema exportieren
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun wifiLocationDao(): WifiLocationDao
    abstract fun friendStatusDao(): FriendStatusDao // <<< Neues DAO für FriendStatus hinzugefügt

    // Das Companion Object für die Singleton-Instanziierung wird entfernt,
    // da Hilt dies über ein @Module und @Provides übernehmen wird.
    // Wenn du Hilt noch nicht für die Datenbank konfiguriert hast, sag Bescheid,
    // dann können wir das Companion Object vorerst beibehalten oder das Hilt-Modul erstellen.
}
