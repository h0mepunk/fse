package com.example.fse.data.auth

import com.example.fse.BuildConfig
import com.example.fse.data.api.FatSecretApi
import com.example.fse.data.api.FatSecretClient
import com.example.fse.data.api.FatSecretOAuthApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Obtains OAuth 2.0 access token for FatSecret API.
 * NOTE: FatSecret requires token requests from whitelisted IPs (usually a backend).
 * Add your server IP in FatSecret dashboard, or use a BFF for production.
 */
class FatSecretAuth(
    private val tokenStore: TokenStore
) {
    private var oauthClient: FatSecretOAuthApi? = null

    suspend fun getAccessToken(): Result<String> = withContext(Dispatchers.IO) {
        tokenStore.getValidToken()?.let { return@withContext Result.success(it) }

        if (BuildConfig.FATSECRET_CLIENT_ID.isBlank() || BuildConfig.FATSECRET_CLIENT_SECRET.isBlank()) {
            return@withContext Result.failure(IllegalStateException(
                "FatSecret credentials missing. Add fatsecret_client_id and fatsecret_client_secret to local.properties"
            ))
        }

        val client = oauthClient ?: FatSecretClient.createOAuthClient().also { oauthClient = it }
        val response = client.getToken()
        if (!response.isSuccessful) {
            val body = response.errorBody()?.string() ?: "Unknown error"
            return@withContext Result.failure(Exception("Token request failed: ${response.code()} $body"))
        }
        val tokenResponse = response.body() ?: return@withContext Result.failure(Exception("Empty token response"))
        tokenStore.saveToken(tokenResponse.accessToken, tokenResponse.expiresIn)
        Result.success(tokenResponse.accessToken)
    }

    fun createApi(token: String): FatSecretApi = FatSecretClient.createApi(token)
}
