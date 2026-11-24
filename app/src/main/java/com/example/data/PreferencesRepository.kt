package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.intelPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "intel_discovery_prefs"
)

data class UserPreferences(
    val theme: ThemePreference = ThemePreference.DARK,
    val aiModel: AiModelOption = AiModelOption.GEMINI_3_8_FLASH,
    val responseDepth: ResponseDepth = ResponseDepth.CONCISE
)

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val THEME = stringPreferencesKey("theme_preference")
        val AI_MODEL = stringPreferencesKey("ai_model_option")
        val RESPONSE_DEPTH = stringPreferencesKey("response_depth")
    }

    val preferences: Flow<UserPreferences> = context.intelPreferencesDataStore.data.map { prefs ->
        UserPreferences(
            theme = ThemePreference.entries.firstOrNull { it.name == prefs[Keys.THEME] }
                ?: ThemePreference.DARK,
            aiModel = AiModelOption.fromRaw(prefs[Keys.AI_MODEL]),
            responseDepth = ResponseDepth.fromRaw(prefs[Keys.RESPONSE_DEPTH])
        )
    }

    suspend fun setTheme(theme: ThemePreference) {
        context.intelPreferencesDataStore.edit { it[Keys.THEME] = theme.name }
    }

    suspend fun setAiModel(model: AiModelOption) {
        context.intelPreferencesDataStore.edit { it[Keys.AI_MODEL] = model.name }
    }

    suspend fun setResponseDepth(depth: ResponseDepth) {
        context.intelPreferencesDataStore.edit { it[Keys.RESPONSE_DEPTH] = depth.name }
    }
}
