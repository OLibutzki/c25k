package com.example.c25k

import com.example.c25k.domain.WorkoutMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutMathTest {
    @Test
    fun `paceSecPerKm returns expected value`() {
        val pace = WorkoutMath.paceSecPerKm(distanceMeters = 1000.0, durationSec = 360)
        assertEquals(360.0, pace ?: 0.0, 0.001)
    }

    @Test
    fun `paceSecPerKm returns null for invalid data`() {
        assertNull(WorkoutMath.paceSecPerKm(distanceMeters = 0.0, durationSec = 200))
        assertNull(WorkoutMath.paceSecPerKm(distanceMeters = 1200.0, durationSec = 0))
    }
}
