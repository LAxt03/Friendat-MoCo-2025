package com.friendat.data.sources.local // Oder dein entsprechendes Paket

import androidx.room.Database
import androidx.room.RoomDatabase
import com.friendat.data.model.WifiLocation
import com.friendat.data.sources.local.dao.WifiLocationDao

@Database(entities = [WifiLocation::class], version = 1, exportSchema = false)
// Bei Schema-Änderungen (neue Felder, Tabellen etc.) musst du die 'version' erhöhen
// und eine Migration implementieren, oder die DB beim Entwickeln löschen und neu erstellen lassen.
// exportSchema = false ist für den Anfang okay, für komplexere Projekte solltest du es auf true setzen
// und das Schema in dein Versionskontrollsystem einchecken.
abstract class AppDatabase : RoomDatabase() {

    abstract fun wifiLocationDao(): WifiLocationDao // Stellt das DAO bereit

    // Hier könntest du weitere DAOs für andere Entitäten hinzufügen

    // Ein Companion Object für eine Singleton-Instanz ist nicht mehr nötig/empfohlen,
    // wenn du Hilt für die Datenbankinstanziierung verwendest. Hilt kümmert sich darum.
}