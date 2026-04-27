package com.example.c25k.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {
    @Query("SELECT COUNT(*) FROM plan_sessions")
    suspend fun countSessions(): Int

    @Insert
    suspend fun insertSession(session: PlanSessionEntity): Long

    @Insert
    suspend fun insertSegments(segments: List<PlanSegmentEntity>)

    @Query("SELECT * FROM plan_sessions ORDER BY orderInPlan")
    fun observeSessions(): Flow<List<PlanSessionEntity>>

    @Query("SELECT * FROM plan_sessions WHERE completedWorkoutId IS NULL ORDER BY orderInPlan LIMIT 1")
    suspend fun getNextSession(): PlanSessionEntity?

    @Query("SELECT * FROM plan_sessions WHERE id = :id")
    suspend fun getSession(id: Long): PlanSessionEntity?

    @Query("SELECT * FROM plan_segments WHERE sessionId = :sessionId ORDER BY segmentOrder")
    suspend fun getSegmentsForSession(sessionId: Long): List<PlanSegmentEntity>

    @Query("UPDATE plan_sessions SET completedWorkoutId = :workoutId WHERE id = :sessionId")
    suspend fun markSessionComplete(sessionId: Long, workoutId: Long)
}

@Dao
interface WorkoutDao {
    @Insert
    suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Insert
    suspend fun insertWorkoutSegments(segments: List<WorkoutSegmentEntity>)

    @Insert
    suspend fun insertTrackPoints(points: List<TrackPointEntity>)

    @Query("SELECT * FROM workouts ORDER BY startedAtEpochMs DESC")
    fun observeWorkoutHistory(): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun getWorkout(id: Long): WorkoutEntity?

    @Query("SELECT * FROM workout_segments WHERE workoutId = :workoutId ORDER BY segmentOrder")
    suspend fun getWorkoutSegments(workoutId: Long): List<WorkoutSegmentEntity>

    @Query("SELECT * FROM track_points WHERE workoutId = :workoutId ORDER BY timestampEpochMs")
    suspend fun getTrackPoints(workoutId: Long): List<TrackPointEntity>
}
