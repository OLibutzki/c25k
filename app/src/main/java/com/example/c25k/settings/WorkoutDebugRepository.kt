package com.example.c25k.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.c25k.domain.WorkoutDebugMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class WorkoutDebugRepository(private val context: Context) {
    private val key = stringPreferencesKey("workout_debug_mode")

    fun observeMode(): Flow<WorkoutDebugMode> {
        return context.settingsDataStore.data.map { prefs ->
            WorkoutDebugMode.fromTag(prefs[key] ?: WorkoutDebugMode.OFF.tag)
        }
    }

    suspend fun getMode(): WorkoutDebugMode = observeMode().first()

    suspend fun setMode(mode: WorkoutDebugMode) {
        context.settingsDataStore.edit { prefs ->
            prefs[key] = mode.tag
        }
    }
}
