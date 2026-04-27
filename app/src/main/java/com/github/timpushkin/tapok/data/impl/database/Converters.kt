package com.github.timpushkin.tapok.data.impl.database

import androidx.room.TypeConverter
import kotlin.time.Instant

class Converters {
    @TypeConverter
    fun instantToString(instant: Instant?) = instant?.toString()

    @TypeConverter
    fun stringToInstant(str: String?) = str?.let { Instant.parse(str) }
}
