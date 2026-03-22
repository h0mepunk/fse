package com.example.fse.data.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fse_token")

class TokenStore(private val context: Context) {

    private val accessTokenKey = stringPreferencesKey("access_token")
    private val expiresAtKey = stringPreferencesKey("expires_at")

    suspend fun saveToken(token: String, expiresInSeconds: Long) {
        context.dataStore.edit { prefs ->
            prefs[accessTokenKey] = token
            prefs[expiresAtKey] = (System.currentTimeMillis() / 1000 + expiresInSeconds).toString()
        }
    }

    suspend fun getValidToken(): String? {
        val token = context.dataStore.data.map { it[accessTokenKey] }.first() ?: return null
        val expiresAt = context.dataStore.data.map { it[expiresAtKey]?.toLongOrNull() }.first() ?: return null
        if (System.currentTimeMillis() / 1000 >= expiresAt - 60) return null  // refresh 1 min before
        return token
    }

    suspend fun clearToken() {
        context.dataStore.edit { it.clear() }
    }
}
