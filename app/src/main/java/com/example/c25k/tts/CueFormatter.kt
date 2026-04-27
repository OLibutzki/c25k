package com.example.c25k.tts

import android.content.Context
import com.example.c25k.R
import com.example.c25k.domain.SegmentType

class CueFormatter(private val context: Context) {
    fun transitionCue(type: SegmentType, durationSec: Int): String {
        val duration = formatDuration(durationSec)
        return when (type) {
            SegmentType.RUN -> context.getString(R.string.tts_start_running, duration)
            SegmentType.WALK -> context.getString(R.string.tts_start_walking, duration)
        }
    }

    fun prepareCue(nextType: SegmentType): String {
        return when (nextType) {
            SegmentType.RUN -> context.getString(R.string.tts_prepare_running)
            SegmentType.WALK -> context.getString(R.string.tts_prepare_walking)
        }
    }

    fun completeCue(): String = context.getString(R.string.tts_workout_complete)

    private fun formatDuration(durationSec: Int): String {
        val minutes = durationSec / 60
        val seconds = durationSec % 60
        return if (seconds == 0 && minutes > 0) {
            context.resources.getQuantityString(R.plurals.duration_minutes, minutes, minutes)
        } else {
            context.resources.getQuantityString(R.plurals.duration_seconds, durationSec, durationSec)
        }
    }
}
