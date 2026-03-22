package com.example.fse.data.api

import com.example.fse.BuildConfig
import com.example.fse.data.auth.OAuth1Signer
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp interceptor that adds OAuth 1.0 HMAC-SHA1 signature to requests.
 */
class OAuth1SigningInterceptor(
    private val accessToken: String,
    private val accessTokenSecret: String
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val url = original.url

        val queryParams = url.queryParameterNames.associateWith { name ->
            url.queryParameter(name) ?: ""
        }.filter { it.value.isNotEmpty() }

        val authHeader = OAuth1Signer.buildAuthorizationHeader(
            method = original.method,
            url = url.toString(),
            extraParams = queryParams,
            consumerKey = BuildConfig.FATSECRET_CLIENT_ID,
            consumerSecret = BuildConfig.FATSECRET_CLIENT_SECRET,
            token = accessToken,
            tokenSecret = accessTokenSecret
        )

        val signed = original.newBuilder()
            .addHeader("Authorization", authHeader)
            .build()
        return chain.proceed(signed)
    }
}
