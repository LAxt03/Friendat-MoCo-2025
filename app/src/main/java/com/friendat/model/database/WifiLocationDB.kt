package com.friendat.model.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.friendat.model.database.daos.WifiLocationDao
import com.friendat.model.database.entity.WifiLocation

import androidx.room.TypeConverters
import com.friendat.model.Converters

@Database(
    entities = [WifiLocation::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wifiLocationDao(): WifiLocationDao
}