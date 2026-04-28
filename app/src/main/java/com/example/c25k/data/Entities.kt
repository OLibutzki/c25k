package com.example.c25k.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.c25k.domain.PlanSessionStatus
import com.example.c25k.domain.SegmentType

@Entity(tableName = "plan_sessions")
data class PlanSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val week: Int,
    val day: Int,
    val orderInPlan: Int,
    val status: PlanSessionStatus = PlanSessionStatus.PENDING,
    val latestCompletedWorkoutId: Long? = null,
    val latestCompletedAtEpochMs: Long? = null
)

@Entity(
    tableName = "plan_segments",
    foreignKeys = [
        ForeignKey(
            entity = PlanSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class PlanSegmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val segmentOrder: Int,
    val type: SegmentType,
    val durationSec: Int
)

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val startedAtEpochMs: Long,
    val completedAtEpochMs: Long,
    val distanceMeters: Double,
    val avgPaceSecPerKm: Double?
)

@Entity(
    tableName = "workout_segments",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workoutId")]
)
data class WorkoutSegmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val segmentOrder: Int,
    val type: SegmentType,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val durationSec: Long,
    val distanceMeters: Double,
    val paceSecPerKm: Double?
)

@Entity(
    tableName = "track_points",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workoutId"), Index("segmentOrder")]
)
data class TrackPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val segmentOrder: Int,
    val latitude: Double,
    val longitude: Double,
    val timestampEpochMs: Long,
    val accuracyMeters: Float,
    val speedMps: Float,
    val segmentType: SegmentType
)
