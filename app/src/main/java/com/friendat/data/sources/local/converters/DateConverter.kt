package com.friendat.data.sources.local.converters

import androidx.room.TypeConverter
import java.util.Date

object DateConverter {
    @TypeConverter
    fun toTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun toDate(timestamp: Long?): Date? {
        return timestamp?.let { Date(it) }
    }
}
