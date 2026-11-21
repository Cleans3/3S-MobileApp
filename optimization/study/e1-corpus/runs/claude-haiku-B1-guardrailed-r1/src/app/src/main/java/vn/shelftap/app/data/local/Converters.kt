package vn.shelftap.app.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class Converters {

    private val listSerializer = ListSerializer(String.serializer())

    @TypeConverter
    fun listToJson(values: List<String>): String = Json.encodeToString(listSerializer, values)

    @TypeConverter
    fun jsonToList(json: String): List<String> = Json.decodeFromString(listSerializer, json)
}
