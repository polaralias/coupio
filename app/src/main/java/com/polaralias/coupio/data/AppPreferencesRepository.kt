package com.polaralias.coupio.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.appPreferencesDataStore by preferencesDataStore(name = "app_preferences")

class AppPreferencesRepository(private val context: Context) {
    private object Keys {
        val issuerName = stringPreferencesKey("issuer_name")
    }

    val issuerName: Flow<String> = context.appPreferencesDataStore.data
        .map { preferences -> preferences[Keys.issuerName].orEmpty() }
        .distinctUntilChanged()

    suspend fun setIssuerName(name: String) {
        context.appPreferencesDataStore.edit { preferences ->
            val cleaned = name.trim()
            if (cleaned.isEmpty()) {
                preferences.remove(Keys.issuerName)
            } else {
                preferences[Keys.issuerName] = cleaned
            }
        }
    }
}
