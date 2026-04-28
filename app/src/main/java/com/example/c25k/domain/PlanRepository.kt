package com.example.c25k.domain

import com.example.c25k.data.PlanDao
import com.example.c25k.data.PlanSegmentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlanRepository(private val planDao: PlanDao) {
    fun observeAllSessions(): Flow<List<PlanSessionModel>> {
        return planDao.observeSessions().map { sessions ->
            sessions.map { session ->
                val segments = planDao.getSegmentsForSession(session.id).map { it.toModel() }
                PlanSessionModel(
                    id = session.id,
                    week = session.week,
                    day = session.day,
                    segments = segments,
                    status = session.status,
                    latestCompletedWorkoutId = session.latestCompletedWorkoutId,
                    latestCompletedAtEpochMs = session.latestCompletedAtEpochMs
                )
            }
        }
    }

    suspend fun getSession(id: Long): PlanSessionModel? {
        val session = planDao.getSession(id) ?: return null
        val segments = planDao.getSegmentsForSession(session.id).map { it.toModel() }
        return PlanSessionModel(
            id = session.id,
            week = session.week,
            day = session.day,
            segments = segments,
            status = session.status,
            latestCompletedWorkoutId = session.latestCompletedWorkoutId,
            latestCompletedAtEpochMs = session.latestCompletedAtEpochMs
        )
    }

    suspend fun markComplete(sessionId: Long, workoutId: Long, completedAtEpochMs: Long) {
        planDao.updateCompletion(
            sessionId = sessionId,
            status = PlanSessionStatus.COMPLETED,
            workoutId = workoutId,
            completedAtEpochMs = completedAtEpochMs
        )
    }

    suspend fun markSkipped(sessionId: Long) {
        val session = planDao.getSession(sessionId) ?: return
        if (session.status == PlanSessionStatus.COMPLETED) return
        planDao.updateStatus(sessionId, PlanSessionStatus.SKIPPED)
    }
}

private fun PlanSegmentEntity.toModel(): PlanSegmentModel {
    return PlanSegmentModel(
        segmentOrder = segmentOrder,
        type = type,
        durationSec = durationSec
    )
}
