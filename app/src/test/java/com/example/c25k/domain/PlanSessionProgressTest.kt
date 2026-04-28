package com.example.c25k.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlanSessionProgressTest {
    @Test
    fun nextSuggestedSession_returnsFirstPending() {
        val sessions = listOf(
            session(id = 1, status = PlanSessionStatus.SKIPPED),
            session(id = 2, status = PlanSessionStatus.PENDING),
            session(id = 3, status = PlanSessionStatus.PENDING)
        )

        assertEquals(2L, sessions.nextSuggestedSession()?.id)
    }

    @Test
    fun latestCompletedSession_returnsMostRecentCompletion() {
        val sessions = listOf(
            session(id = 1, status = PlanSessionStatus.COMPLETED, completedAt = 1000L),
            session(id = 2, status = PlanSessionStatus.COMPLETED, completedAt = 3000L),
            session(id = 3, status = PlanSessionStatus.SKIPPED)
        )

        assertEquals(2L, sessions.latestCompletedSession()?.id)
    }

    @Test
    fun latestCompletedSession_returnsNullWithoutCompletedSessions() {
        val sessions = listOf(
            session(id = 1, status = PlanSessionStatus.PENDING),
            session(id = 2, status = PlanSessionStatus.SKIPPED)
        )

        assertNull(sessions.latestCompletedSession())
    }

    private fun session(
        id: Long,
        status: PlanSessionStatus,
        completedAt: Long? = null
    ) = PlanSessionModel(
        id = id,
        week = 1,
        day = id.toInt(),
        segments = emptyList(),
        status = status,
        latestCompletedWorkoutId = completedAt?.let { id * 10 },
        latestCompletedAtEpochMs = completedAt
    )
}
