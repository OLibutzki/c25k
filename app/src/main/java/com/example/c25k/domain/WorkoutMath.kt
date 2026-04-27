package com.example.c25k.domain

object WorkoutMath {
    fun paceSecPerKm(distanceMeters: Double, durationSec: Long): Double? {
        if (distanceMeters <= 0.0 || durationSec <= 0L) return null
        return durationSec / (distanceMeters / 1000.0)
    }
}
