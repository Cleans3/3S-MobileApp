package vn.shelftap.app.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter fun fromList(value: List<String>): String = value.joinToString("\u001f")
    @TypeConverter fun toList(value: String): List<String> = if (value.isEmpty()) emptyList() else value.split("\u001f")
}
