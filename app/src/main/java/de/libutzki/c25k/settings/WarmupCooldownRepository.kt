package de.libutzki.c25k.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import de.libutzki.c25k.domain.DEFAULT_WARMUP_COOLDOWN_DURATION_SEC
import de.libutzki.c25k.domain.MAX_WARMUP_COOLDOWN_DURATION_SEC
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class WarmupCooldownRepository(private val context: Context) {
    private val key = intPreferencesKey("warmup_cooldown_duration_sec")

    fun observeDurationSec(): Flow<Int> {
        return context.settingsDataStore.data.map { prefs ->
            (prefs[key] ?: DEFAULT_WARMUP_COOLDOWN_DURATION_SEC)
                .coerceIn(0, MAX_WARMUP_COOLDOWN_DURATION_SEC)
        }
    }

    suspend fun getDurationSec(): Int = observeDurationSec().first()

    suspend fun setDurationSec(durationSec: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[key] = durationSec.coerceIn(0, MAX_WARMUP_COOLDOWN_DURATION_SEC)
        }
    }
}
