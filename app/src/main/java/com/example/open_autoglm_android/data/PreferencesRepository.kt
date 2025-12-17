package com.example.open_autoglm_android.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object PreferenceKeys {
    val API_KEY = stringPreferencesKey("api_key")
    val BASE_URL = stringPreferencesKey("base_url")
    val MODEL_NAME = stringPreferencesKey("model_name")
}

class PreferencesRepository(private val context: Context) {
    
    val apiKey: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.API_KEY]
    }
    
    val baseUrl: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.BASE_URL] ?: "https://open.bigmodel.cn/api/paas/v4"
    }
    
    val modelName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.MODEL_NAME] ?: "autoglm-phone"
    }
    
    suspend fun saveApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.API_KEY] = apiKey
        }
    }
    
    suspend fun saveBaseUrl(baseUrl: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.BASE_URL] = baseUrl
        }
    }
    
    suspend fun saveModelName(modelName: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.MODEL_NAME] = modelName
        }
    }
    
    suspend fun getApiKeySync(): String? {
        return context.dataStore.data.map { it[PreferenceKeys.API_KEY] }.firstOrNull()
    }
    
    suspend fun getBaseUrlSync(): String {
        return context.dataStore.data.map { 
            it[PreferenceKeys.BASE_URL] ?: "https://open.bigmodel.cn/api/paas/v4"
        }.firstOrNull() ?: "https://open.bigmodel.cn/api/paas/v4"
    }
    
    suspend fun getModelNameSync(): String {
        return context.dataStore.data.map { 
            it[PreferenceKeys.MODEL_NAME] ?: "autoglm-phone"
        }.firstOrNull() ?: "autoglm-phone"
    }
}
