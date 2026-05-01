package com.example.c25k.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlanSessionProgressTest {
    @Test
    fun nextSessionFromPlanProgress_returnsFirstSession_whenNothingCompleted() {
        val sessions = listOf(
            session(id = 1, orderInPlan = 1, status = PlanSessionStatus.PENDING),
            session(id = 2, orderInPlan = 2, status = PlanSessionStatus.PENDING),
            session(id = 3, orderInPlan = 3, status = PlanSessionStatus.PENDING)
        )

        assertEquals(1L, sessions.nextSessionFromPlanProgress()?.id)
    }

    @Test
    fun nextSessionFromPlanProgress_returnsSessionAfterFurthestCompleted() {
        val sessions = listOf(
            session(id = 1, orderInPlan = 1, status = PlanSessionStatus.PENDING),
            session(id = 2, orderInPlan = 2, status = PlanSessionStatus.COMPLETED, completedAt = 1000L),
            session(id = 3, orderInPlan = 3, status = PlanSessionStatus.PENDING)
        )

        assertEquals(3L, sessions.nextSessionFromPlanProgress()?.id)
    }

    @Test
    fun furthestCompletedSession_returnsHighestOrderCompleted() {
        val sessions = listOf(
            session(id = 1, orderInPlan = 1, status = PlanSessionStatus.COMPLETED, completedAt = 1000L),
            session(id = 2, orderInPlan = 2, status = PlanSessionStatus.PENDING),
            session(id = 3, orderInPlan = 3, status = PlanSessionStatus.COMPLETED, completedAt = 3000L)
        )

        assertEquals(3L, sessions.furthestCompletedSession()?.id)
    }

    @Test
    fun furthestCompletedSession_returnsNullWithoutCompletedSessions() {
        val sessions = listOf(
            session(id = 1, orderInPlan = 1, status = PlanSessionStatus.PENDING),
            session(id = 2, orderInPlan = 2, status = PlanSessionStatus.PENDING)
        )

        assertNull(sessions.furthestCompletedSession())
    }

    private fun session(
        id: Long,
        orderInPlan: Int,
        status: PlanSessionStatus,
        completedAt: Long? = null
    ) = PlanSessionModel(
        id = id,
        week = 1,
        day = id.toInt(),
        orderInPlan = orderInPlan,
        segments = emptyList(),
        status = status,
        latestCompletedWorkoutId = completedAt?.let { id * 10 },
        latestCompletedAtEpochMs = completedAt
    )
}
