package com.example.fse.data.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.oauth1DataStore: DataStore<Preferences> by preferencesDataStore(name = "fse_oauth1")

/**
 * Stores OAuth 1.0 access token and secret for FatSecret Profile API.
 */
class OAuth1TokenStore(private val context: Context) {

    private val accessTokenKey = stringPreferencesKey("oauth1_access_token")
    private val accessTokenSecretKey = stringPreferencesKey("oauth1_access_token_secret")

    val hasTokens: Flow<Boolean> = context.oauth1DataStore.data.map { prefs ->
        prefs[accessTokenKey] != null && prefs[accessTokenSecretKey] != null
    }

    suspend fun getTokens(): Pair<String, String>? {
        val token = context.oauth1DataStore.data.map { it[accessTokenKey] }.first() ?: return null
        val secret = context.oauth1DataStore.data.map { it[accessTokenSecretKey] }.first() ?: return null
        return Pair(token, secret)
    }

    suspend fun saveTokens(accessToken: String, accessTokenSecret: String) {
        context.oauth1DataStore.edit { prefs ->
            prefs[accessTokenKey] = accessToken
            prefs[accessTokenSecretKey] = accessTokenSecret
        }
    }

    suspend fun clearTokens() {
        context.oauth1DataStore.edit { it.clear() }
    }
}
