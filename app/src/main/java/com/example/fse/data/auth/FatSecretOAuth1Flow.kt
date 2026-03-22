package com.example.fse.data.auth

import com.example.fse.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * FatSecret 3-legged OAuth 1.0 flow.
 * Uses oob (out-of-band) — user copies verifier from browser and pastes into app.
 * @see <a href="https://platform.fatsecret.com/docs/guides/authentication/oauth1/three-legged">3-Legged OAuth</a>
 */
class FatSecretOAuth1Flow(
    private val oauth1TokenStore: OAuth1TokenStore
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val requestTokenUrl = "https://authentication.fatsecret.com/oauth/request_token"
    private val authorizeUrl = "https://authentication.fatsecret.com/oauth/authorize"
    private val accessTokenUrl = "https://authentication.fatsecret.com/oauth/access_token"

    data class RequestTokenResult(
        val requestToken: String,
        val requestTokenSecret: String,
        val authorizeUrl: String
    )

    suspend fun getRequestToken(): Result<RequestTokenResult> = withContext(Dispatchers.IO) {
        if (BuildConfig.FATSECRET_CLIENT_ID.isBlank() || BuildConfig.FATSECRET_CLIENT_SECRET.isBlank()) {
            return@withContext Result.failure(IllegalStateException(
                "FatSecret credentials missing. Add fatsecret_client_id and fatsecret_client_secret to local.properties"
            ))
        }

        val extraParams = mapOf("oauth_callback" to "oob")
        val oauthParams = OAuth1Signer.buildOAuthParams(
            extraParams = extraParams,
            consumerKey = BuildConfig.FATSECRET_CLIENT_ID,
            consumerSecret = BuildConfig.FATSECRET_CLIENT_SECRET,
            token = null,
            tokenSecret = null,
            method = "POST",
            url = requestTokenUrl
        )

        val bodyBuilder = FormBody.Builder()
        bodyBuilder.add("oauth_callback", "oob")
        oauthParams.forEach { (k, v) -> bodyBuilder.add(k, v) }
        val request = Request.Builder()
            .url(requestTokenUrl)
            .post(bodyBuilder.build())
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            return@withContext Result.failure(Exception("Request token failed: ${response.code} ${response.body?.string()}"))
        }

        val bodyStr = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
        val parsed = parseOAuthResponse(bodyStr)
        val requestToken = parsed["oauth_token"] ?: return@withContext Result.failure(Exception("Missing oauth_token"))
        val requestTokenSecret = parsed["oauth_token_secret"] ?: return@withContext Result.failure(Exception("Missing oauth_token_secret"))

        Result.success(
            RequestTokenResult(
                requestToken = requestToken,
                requestTokenSecret = requestTokenSecret,
                authorizeUrl = "$authorizeUrl?oauth_token=$requestToken"
            )
        )
    }

    suspend fun exchangeForAccessToken(requestToken: String, requestTokenSecret: String, verifier: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (BuildConfig.FATSECRET_CLIENT_ID.isBlank() || BuildConfig.FATSECRET_CLIENT_SECRET.isBlank()) {
                return@withContext Result.failure(IllegalStateException("FatSecret credentials missing"))
            }

            val extraParams = mapOf("oauth_token" to requestToken, "oauth_verifier" to verifier)
            val oauthParams = OAuth1Signer.buildOAuthParams(
                extraParams = extraParams,
                consumerKey = BuildConfig.FATSECRET_CLIENT_ID,
                consumerSecret = BuildConfig.FATSECRET_CLIENT_SECRET,
                token = requestToken,
                tokenSecret = requestTokenSecret,
                method = "GET",
                url = accessTokenUrl
            )

            val allParams = (oauthParams + extraParams).toSortedMap()
            val queryString = allParams.entries.joinToString("&") { (k, v) ->
                "${OAuth1Signer.percentEncode(k)}=${OAuth1Signer.percentEncode(v)}"
            }
            val request = Request.Builder()
                .url("$accessTokenUrl?$queryString")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Access token failed: ${response.code} ${response.body?.string()}"))
            }

            val bodyStr = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
            val parsed = parseOAuthResponse(bodyStr)
            val accessToken = parsed["oauth_token"] ?: return@withContext Result.failure(Exception("Missing oauth_token"))
            val accessTokenSecret = parsed["oauth_token_secret"] ?: return@withContext Result.failure(Exception("Missing oauth_token_secret"))

            oauth1TokenStore.saveTokens(accessToken, accessTokenSecret)
            Result.success(Unit)
        }

    private fun parseOAuthResponse(body: String): Map<String, String> =
        body.split("&").associate { part ->
            val (k, v) = part.split("=", limit = 2).let { (a, b) -> a to (b ?: "") }
            k to java.net.URLDecoder.decode(v, Charsets.UTF_8)
        }
}
