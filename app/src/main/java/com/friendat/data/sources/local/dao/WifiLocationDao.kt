package com.friendat.data.sources.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.friendat.data.model.WifiLocation
import kotlinx.coroutines.flow.Flow

@Dao
interface WifiLocationDao {

    /**
     * Fügt einen neuen WifiLocation-Eintrag hinzu.
     * Wenn ein Eintrag mit derselben ID bereits existiert, wird er ersetzt.
     */
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insert(wifiLocation: WifiLocation)

    /**
     * Fügt mehrere WifiLocation-Einträge hinzu.
     */
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertAll(wifiLocations: List<WifiLocation>)

    /**
     * Aktualisiert einen bestehenden WifiLocation-Eintrag.
     */
    @Update
    suspend fun update(wifiLocation: WifiLocation)

    /**
     * Löscht einen WifiLocation-Eintrag.
     */
    @Delete
    suspend fun delete(wifiLocation: WifiLocation)

    /**
     * Ruft einen spezifischen WifiLocation-Eintrag anhand seiner ID ab.
     * Nützlich für Detailansichten oder Bearbeitungen.
     */
    @Query("SELECT * FROM wifi_locations WHERE id = :id AND userId = :userId")
    suspend fun getById(id: String, userId: String): WifiLocation?

    /**
     * Ruft alle WifiLocation-Einträge für einen bestimmten Benutzer ab.
     * Dies ist die Funktion, die der LocationCheckWorker verwenden wird.
     */
    @Query("SELECT * FROM wifi_locations WHERE userId = :userId ORDER BY name ASC")
    suspend fun getAllLocationsForUserSuspend(userId: String): List<WifiLocation>

    /**
     * Ruft alle WifiLocation-Einträge für einen bestimmten Benutzer als Flow ab.
     * Nützlich für die UI, um auf Datenbankänderungen reaktiv zu reagieren.
     */
    @Query("SELECT * FROM wifi_locations WHERE userId = :userId ORDER BY name ASC")
    fun getAllLocationsForUserFlow(userId: String): Flow<List<WifiLocation>>

    /**
     * Löscht alle WifiLocation-Einträge für einen bestimmten Benutzer.
     * Nützlich z.B. beim Logout.
     */
    @Query("DELETE FROM wifi_locations WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    /**
     * Löscht alle WifiLocation-Einträge aus der Tabelle.
     * Vorsicht bei der Verwendung!
     */
    @Query("DELETE FROM wifi_locations")
    suspend fun deleteAll() // Sei vorsichtig mit dieser Funktion
}