package com.example.c25k.domain

enum class SegmentType {
    RUN,
    WALK
}

enum class PlanSessionStatus {
    PENDING,
    COMPLETED,
    SKIPPED
}

enum class AppLanguage(val tag: String) {
    SYSTEM("system"),
    EN("en"),
    DE("de");

    companion object {
        fun fromTag(tag: String): AppLanguage = entries.firstOrNull { it.tag == tag } ?: SYSTEM
    }
}

enum class WorkoutDebugMode(val tag: String, val durationDivisor: Int) {
    OFF("off", 1),
    X10("x10", 10),
    X60("x60", 60);

    companion object {
        fun fromTag(tag: String): WorkoutDebugMode =
            entries.firstOrNull { it.tag == tag } ?: OFF
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
    val status: PlanSessionStatus,
    val latestCompletedWorkoutId: Long?,
    val latestCompletedAtEpochMs: Long?
)

data class WorkoutSummary(
    val id: Long,
    val sessionId: Long,
    val week: Int?,
    val day: Int?,
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

fun List<PlanSessionModel>.nextSuggestedSession(): PlanSessionModel? =
    firstOrNull { it.status == PlanSessionStatus.PENDING }

fun List<PlanSessionModel>.latestCompletedSession(): PlanSessionModel? =
    filter { it.status == PlanSessionStatus.COMPLETED && it.latestCompletedAtEpochMs != null }
        .maxByOrNull { it.latestCompletedAtEpochMs ?: Long.MIN_VALUE }

fun PlanSessionModel.withDurationsDividedBy(divisor: Int): PlanSessionModel {
    if (divisor <= 1) return this
    return copy(
        segments = segments.map { segment ->
            segment.copy(durationSec = maxOf(1, (segment.durationSec + divisor - 1) / divisor))
        }
    )
}
