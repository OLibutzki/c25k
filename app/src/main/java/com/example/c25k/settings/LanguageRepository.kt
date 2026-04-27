package com.example.c25k.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.c25k.domain.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class LanguageRepository(private val context: Context) {
    private val key = stringPreferencesKey("app_language")

    fun observeLanguage(): Flow<AppLanguage> {
        return context.dataStore.data.map { prefs ->
            AppLanguage.fromTag(prefs[key] ?: AppLanguage.EN.tag)
        }
    }

    suspend fun getLanguage(): AppLanguage = observeLanguage().first()

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { prefs ->
            prefs[key] = language.tag
        }
    }
}
