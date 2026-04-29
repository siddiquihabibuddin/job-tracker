package com.jobtracker.android.core.data.db

import androidx.room.TypeConverter
import com.jobtracker.android.core.domain.model.AppStatus

class Converters {

    @TypeConverter
    fun statusToString(value: AppStatus): String = value.name

    @TypeConverter
    fun stringToStatus(value: String): AppStatus = AppStatus.valueOf(value)

    @TypeConverter
    fun listToString(list: List<String>?): String = list?.joinToString(SEP).orEmpty()

    @TypeConverter
    fun stringToList(value: String?): List<String> =
        if (value.isNullOrEmpty()) emptyList() else value.split(SEP)

    private companion object {
        const val SEP = "\u001F"
    }
}
