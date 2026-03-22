package com.example.fse.data.api

import com.example.fse.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType

object FatSecretClient {

    private const val BASE_URL = "https://platform.fatsecret.com/"

    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    fun createApi(accessToken: String): FatSecretApi {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) {
                        HttpLoggingInterceptor.Level.BODY
                    } else HttpLoggingInterceptor.Level.NONE
                }
            )
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(FatSecretApi::class.java)
    }

    /** Platform API (foods.search, food.get) with OAuth 1.0 - use when user has connected account. */
    fun createPlatformApiWithOAuth1(accessToken: String, accessTokenSecret: String): FatSecretApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(OAuth1SigningInterceptor(accessToken, accessTokenSecret))
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) {
                        HttpLoggingInterceptor.Level.BODY
                    } else HttpLoggingInterceptor.Level.NONE
                }
            )
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(FatSecretApi::class.java)
    }

    fun createProfileApi(accessToken: String, accessTokenSecret: String): FatSecretProfileApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(OAuth1SigningInterceptor(accessToken, accessTokenSecret))
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) {
                        HttpLoggingInterceptor.Level.BODY
                    } else HttpLoggingInterceptor.Level.NONE
                }
            )
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(FatSecretProfileApi::class.java)
    }

    fun createOAuthClient(): FatSecretOAuthApi {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val credentials = java.util.Base64.getEncoder().encodeToString(
                    "${BuildConfig.FATSECRET_CLIENT_ID}:${BuildConfig.FATSECRET_CLIENT_SECRET}".toByteArray(Charsets.UTF_8)
                )
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Basic $credentials")
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl("https://oauth.fatsecret.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(FatSecretOAuthApi::class.java)
    }
}
