package com.friendat.data.sources.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.friendat.data.model.FriendStatus
import com.friendat.data.model.WifiLocation
import com.friendat.data.sources.local.dao.FriendStatusDao
import com.friendat.data.sources.local.dao.WifiLocationDao


//Vorher war Datenstruktur anders, Migration ist dafür da um die alte Datenstruktur in die neue zu konvertieren.
//Alte Datensätze könnten noch exisitieren deswegen lassen wir diese hier stehen.
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {

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

//Ist für Room Datenbanken wichtig.
@Database(
    entities = [WifiLocation::class, FriendStatus::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun wifiLocationDao(): WifiLocationDao
    abstract fun friendStatusDao(): FriendStatusDao

}
