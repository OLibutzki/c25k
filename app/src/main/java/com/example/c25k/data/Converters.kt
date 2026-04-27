package com.example.c25k.data

import androidx.room.TypeConverter
import com.example.c25k.domain.SegmentType

class Converters {
    @TypeConverter
    fun fromSegmentType(value: SegmentType): String = value.name

    @TypeConverter
    fun toSegmentType(value: String): SegmentType = SegmentType.valueOf(value)
}
