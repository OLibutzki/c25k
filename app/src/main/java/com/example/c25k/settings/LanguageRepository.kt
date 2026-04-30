package com.example.c25k.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.c25k.domain.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class LanguageRepository(private val context: Context) {
    private val key = stringPreferencesKey("app_language")

    fun observeLanguage(): Flow<AppLanguage> {
        return context.settingsDataStore.data.map { prefs ->
            AppLanguage.fromTag(prefs[key] ?: AppLanguage.SYSTEM.tag)
        }
    }

    suspend fun getLanguage(): AppLanguage = observeLanguage().first()

    suspend fun setLanguage(language: AppLanguage) {
        context.settingsDataStore.edit { prefs ->
            prefs[key] = language.tag
        }
    }
}
