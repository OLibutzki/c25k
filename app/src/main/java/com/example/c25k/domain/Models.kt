package com.example.c25k.domain

enum class SegmentType {
    RUN,
    WALK
}

enum class AppLanguage(val tag: String) {
    EN("en"),
    DE("de");

    companion object {
        fun fromTag(tag: String): AppLanguage = entries.firstOrNull { it.tag == tag } ?: EN
    }
}

data class PlanSegmentModel(
    val segmentOrder: Int,
    val type: SegmentType,
    val durationSec: Int
)

data class PlanSessionModel(
    val id: Long,
    val week: Int,
    val day: Int,
    val segments: List<PlanSegmentModel>,
    val completedWorkoutId: Long?
)

data class WorkoutSummary(
    val id: Long,
    val startedAtEpochMs: Long,
    val completedAtEpochMs: Long,
    val distanceMeters: Double,
    val avgPaceSecPerKm: Double?
)

data class SegmentStats(
    val type: SegmentType,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val durationSec: Long,
    val distanceMeters: Double,
    val paceSecPerKm: Double?
)

data class TrackPointModel(
    val latitude: Double,
    val longitude: Double,
    val timestampEpochMs: Long,
    val accuracyMeters: Float,
    val speedMps: Float,
    val segmentType: SegmentType
)
