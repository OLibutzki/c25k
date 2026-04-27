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
                    completedWorkoutId = session.completedWorkoutId
                )
            }
        }
    }

    suspend fun getNextSession(): PlanSessionModel? {
        val session = planDao.getNextSession() ?: return null
        val segments = planDao.getSegmentsForSession(session.id).map { it.toModel() }
        return PlanSessionModel(
            id = session.id,
            week = session.week,
            day = session.day,
            segments = segments,
            completedWorkoutId = session.completedWorkoutId
        )
    }

    suspend fun getSession(id: Long): PlanSessionModel? {
        val session = planDao.getSession(id) ?: return null
        val segments = planDao.getSegmentsForSession(session.id).map { it.toModel() }
        return PlanSessionModel(
            id = session.id,
            week = session.week,
            day = session.day,
            segments = segments,
            completedWorkoutId = session.completedWorkoutId
        )
    }

    suspend fun markComplete(sessionId: Long, workoutId: Long) {
        planDao.markSessionComplete(sessionId, workoutId)
    }
}

private fun PlanSegmentEntity.toModel(): PlanSegmentModel {
    return PlanSegmentModel(
        segmentOrder = segmentOrder,
        type = type,
        durationSec = durationSec
    )
}
