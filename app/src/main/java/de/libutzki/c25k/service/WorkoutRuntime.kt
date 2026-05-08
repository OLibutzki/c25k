package de.libutzki.c25k.service

import android.content.Context
import android.content.Intent
import de.libutzki.c25k.domain.PlanSegmentModel
import de.libutzki.c25k.domain.SegmentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class WorkoutStatus {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED
}

data class WorkoutState(
    val status: WorkoutStatus = WorkoutStatus.IDLE,
    val completedWorkoutId: Long? = null,
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
    const val ACTION_START = "de.libutzki.c25k.action.START"
    const val ACTION_PAUSE = "de.libutzki.c25k.action.PAUSE"
    const val ACTION_RESUME = "de.libutzki.c25k.action.RESUME"
    const val ACTION_STOP = "de.libutzki.c25k.action.STOP"

    const val EXTRA_SESSION_ID = "session_id"

    private val _state = MutableStateFlow(WorkoutState())
    val state: StateFlow<WorkoutState> = _state.asStateFlow()

    fun updateState(state: WorkoutState) {
        _state.value = state
    }

    fun clearCompletion() {
        if (_state.value.status == WorkoutStatus.COMPLETED) {
            _state.value = WorkoutState(status = WorkoutStatus.IDLE)
        }
    }

    fun startService(context: Context, action: String, sessionId: Long? = null) {
        val intent = Intent(context, WorkoutForegroundService::class.java).apply {
            this.action = action
            sessionId?.let { putExtra(EXTRA_SESSION_ID, it) }
        }
        context.startForegroundService(intent)
    }
}
