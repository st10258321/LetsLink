package com.example.letslink.API_related

import androidx.room.TypeConverter
import java.util.UUID

class UUIDConverter {
    @TypeConverter
    fun fromUUID(uuid: UUID?): String? {
        return uuid?.toString()
    }

    @TypeConverter
    fun toUUID(uuidString: String?): UUID? {
        return uuidString?.let { UUID.fromString(it) }
    }
}