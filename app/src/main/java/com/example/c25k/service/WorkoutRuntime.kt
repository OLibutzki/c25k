package com.example.c25k.service

import android.content.Context
import android.content.Intent
import com.example.c25k.domain.PlanSegmentModel
import com.example.c25k.domain.SegmentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class WorkoutPhase {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED
}

data class WorkoutState(
    val phase: WorkoutPhase = WorkoutPhase.IDLE,
    val sessionId: Long? = null,
    val week: Int? = null,
    val day: Int? = null,
    val segments: List<PlanSegmentModel> = emptyList(),
    val currentSegmentType: SegmentType? = null,
    val currentSegmentOrder: Int = 0,
    val segmentRemainingSec: Int = 0,
    val elapsedSec: Long = 0,
    val totalDistanceMeters: Double = 0.0,
    val currentPaceSecPerKm: Double? = null,
    val runPaceSecPerKm: Double? = null,
    val walkPaceSecPerKm: Double? = null,
    val errorMessage: String? = null
)

object WorkoutRuntime {
    const val ACTION_START = "com.example.c25k.action.START"
    const val ACTION_PAUSE = "com.example.c25k.action.PAUSE"
    const val ACTION_RESUME = "com.example.c25k.action.RESUME"
    const val ACTION_STOP = "com.example.c25k.action.STOP"

    const val EXTRA_SESSION_ID = "session_id"

    private val _state = MutableStateFlow(WorkoutState())
    val state: StateFlow<WorkoutState> = _state.asStateFlow()

    fun updateState(state: WorkoutState) {
        _state.value = state
    }

    fun startService(context: Context, action: String, sessionId: Long? = null) {
        val intent = Intent(context, WorkoutForegroundService::class.java).apply {
            this.action = action
            sessionId?.let { putExtra(EXTRA_SESSION_ID, it) }
        }
        context.startForegroundService(intent)
    }
}
