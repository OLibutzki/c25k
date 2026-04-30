package com.example.c25k.domain

enum class SegmentType {
    RUN,
    WALK,
    WARMUP,
    COOLDOWN
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

const val DEFAULT_WARMUP_COOLDOWN_DURATION_SEC = 5 * 60
const val MAX_WARMUP_COOLDOWN_DURATION_SEC = 15 * 60

data class PlanSegmentModel(
    val segmentOrder: Int,
    val type: SegmentType,
    val durationSec: Int,
    val countsTowardWorkout: Boolean = true
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

fun PlanSessionModel.withWarmupCooldownDuration(durationSec: Int): PlanSessionModel {
    if (segments.isEmpty()) return this
    val normalizedDurationSec = durationSec.coerceIn(0, MAX_WARMUP_COOLDOWN_DURATION_SEC)
    val adjustedSegments = buildList {
        if (normalizedDurationSec > 0) {
            add(
                PlanSegmentModel(
                    segmentOrder = -1,
                    type = SegmentType.WARMUP,
                    durationSec = normalizedDurationSec,
                    countsTowardWorkout = false
                )
            )
        }
        addAll(segments.map { it.copy(countsTowardWorkout = true) })
        if (normalizedDurationSec > 0) {
            add(
                PlanSegmentModel(
                    segmentOrder = -1,
                    type = SegmentType.COOLDOWN,
                    durationSec = normalizedDurationSec,
                    countsTowardWorkout = false
                )
            )
        }
    }

    return copy(
        segments = adjustedSegments.mapIndexed { index, segment ->
            segment.copy(segmentOrder = index)
        }
    )
}

fun PlanSessionModel.isTrackedSegment(segmentOrder: Int): Boolean {
    return segments.firstOrNull { it.segmentOrder == segmentOrder }?.countsTowardWorkout == true
}

fun PlanSessionModel.trackedSegmentIndex(segmentOrder: Int): Int? {
    if (!isTrackedSegment(segmentOrder)) return null
    return segments
        .count { it.segmentOrder < segmentOrder && it.countsTowardWorkout }
}
