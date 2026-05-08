package de.libutzki.c25k.data

import androidx.room.TypeConverter
import de.libutzki.c25k.domain.PlanSessionStatus
import de.libutzki.c25k.domain.SegmentType

class Converters {
    @TypeConverter
    fun fromSegmentType(value: SegmentType): String = value.name

    @TypeConverter
    fun toSegmentType(value: String): SegmentType = SegmentType.valueOf(value)

    @TypeConverter
    fun fromPlanSessionStatus(value: PlanSessionStatus): String = value.name

    @TypeConverter
    fun toPlanSessionStatus(value: String): PlanSessionStatus = PlanSessionStatus.valueOf(value)
}
