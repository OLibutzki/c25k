package com.example.c25k.domain

import com.example.c25k.data.TrackPointEntity
import com.example.c25k.data.WorkoutDao
import com.example.c25k.data.WorkoutEntity
import com.example.c25k.data.WorkoutSegmentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class WorkoutPersistRequest(
    val sessionId: Long,
    val startedAtEpochMs: Long,
    val completedAtEpochMs: Long,
    val distanceMeters: Double,
    val avgPaceSecPerKm: Double?,
    val segments: List<SegmentStats>,
    val points: List<TrackPointCapture>
)

data class TrackPointCapture(
    val segmentOrder: Int,
    val latitude: Double,
    val longitude: Double,
    val timestampEpochMs: Long,
    val accuracyMeters: Float,
    val speedMps: Float,
    val segmentType: SegmentType
)

data class WorkoutDetail(
    val workout: WorkoutSummary,
    val segments: List<SegmentStats>,
    val points: List<TrackPointModel>
)

class WorkoutRepository(private val workoutDao: WorkoutDao) {
    fun observeHistory(): Flow<List<WorkoutSummary>> {
        return workoutDao.observeWorkoutHistory().map { entities ->
            entities.map {
                WorkoutSummary(
                    id = it.id,
                    startedAtEpochMs = it.startedAtEpochMs,
                    completedAtEpochMs = it.completedAtEpochMs,
                    distanceMeters = it.distanceMeters,
                    avgPaceSecPerKm = it.avgPaceSecPerKm
                )
            }
        }
    }

    suspend fun persistWorkout(request: WorkoutPersistRequest): Long {
        val workoutId = workoutDao.insertWorkout(
            WorkoutEntity(
                sessionId = request.sessionId,
                startedAtEpochMs = request.startedAtEpochMs,
                completedAtEpochMs = request.completedAtEpochMs,
                distanceMeters = request.distanceMeters,
                avgPaceSecPerKm = request.avgPaceSecPerKm
            )
        )

        val segmentEntities = request.segments.mapIndexed { index, seg ->
            WorkoutSegmentEntity(
                workoutId = workoutId,
                segmentOrder = index,
                type = seg.type,
                startEpochMs = seg.startEpochMs,
                endEpochMs = seg.endEpochMs,
                durationSec = seg.durationSec,
                distanceMeters = seg.distanceMeters,
                paceSecPerKm = seg.paceSecPerKm
            )
        }
        workoutDao.insertWorkoutSegments(segmentEntities)

        val pointEntities = request.points.map {
            TrackPointEntity(
                workoutId = workoutId,
                segmentOrder = it.segmentOrder,
                latitude = it.latitude,
                longitude = it.longitude,
                timestampEpochMs = it.timestampEpochMs,
                accuracyMeters = it.accuracyMeters,
                speedMps = it.speedMps,
                segmentType = it.segmentType
            )
        }
        workoutDao.insertTrackPoints(pointEntities)
        return workoutId
    }

    suspend fun getWorkoutDetail(workoutId: Long): WorkoutDetail? {
        val workout = workoutDao.getWorkout(workoutId) ?: return null
        val segments = workoutDao.getWorkoutSegments(workoutId).map {
            SegmentStats(
                type = it.type,
                startEpochMs = it.startEpochMs,
                endEpochMs = it.endEpochMs,
                durationSec = it.durationSec,
                distanceMeters = it.distanceMeters,
                paceSecPerKm = it.paceSecPerKm
            )
        }
        val points = workoutDao.getTrackPoints(workoutId).map {
            TrackPointModel(
                latitude = it.latitude,
                longitude = it.longitude,
                timestampEpochMs = it.timestampEpochMs,
                accuracyMeters = it.accuracyMeters,
                speedMps = it.speedMps,
                segmentType = it.segmentType
            )
        }
        return WorkoutDetail(
            workout = WorkoutSummary(
                id = workout.id,
                startedAtEpochMs = workout.startedAtEpochMs,
                completedAtEpochMs = workout.completedAtEpochMs,
                distanceMeters = workout.distanceMeters,
                avgPaceSecPerKm = workout.avgPaceSecPerKm
            ),
            segments = segments,
            points = points
        )
    }
}
