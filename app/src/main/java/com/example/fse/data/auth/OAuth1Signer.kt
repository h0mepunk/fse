package com.example.fse.data.auth

import java.net.URLEncoder
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * OAuth 1.0a HMAC-SHA1 signing for FatSecret 3-legged flow.
 * @see <a href="https://platform.fatsecret.com/docs/guides/authentication/oauth1">FatSecret OAuth 1.0</a>
 */
object OAuth1Signer {

    private const val SIGNATURE_METHOD = "HMAC-SHA1"
    private const val VERSION = "1.0"

    /**
     * Builds OAuth 1.0 parameters map (for body or query string).
     * Use this when the server expects params in body instead of Authorization header.
     */
    fun buildOAuthParams(
        extraParams: Map<String, String>,
        consumerKey: String,
        consumerSecret: String,
        token: String? = null,
        tokenSecret: String? = null,
        method: String,
        url: String
    ): Map<String, String> {
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        val nonce = java.util.UUID.randomUUID().toString().replace("-", "")

        val oauthParams = mutableMapOf(
            "oauth_consumer_key" to consumerKey,
            "oauth_signature_method" to SIGNATURE_METHOD,
            "oauth_timestamp" to timestamp,
            "oauth_nonce" to nonce,
            "oauth_version" to VERSION
        )
        if (token != null) oauthParams["oauth_token"] = token
        val allParams = (oauthParams + extraParams).toSortedMap()

        val paramString = allParams.entries.joinToString("&") { (k, v) ->
            "${percentEncode(k)}=${percentEncode(v)}"
        }
        val signatureBase = "${method.uppercase(Locale.US)}&${percentEncode(normalizeUrl(url))}&${percentEncode(paramString)}"
        val signingKey = "${percentEncode(consumerSecret)}&${tokenSecret?.let { percentEncode(it) } ?: ""}"
        val signature = signHmacSha1(signatureBase, signingKey)

        return oauthParams + ("oauth_signature" to signature)
    }

    /**
     * Builds Authorization header value for OAuth 1.0 request.
     * @param method HTTP method (GET, POST)
     * @param url Full request URL
     * @param extraParams Additional params to include in signature (query or body)
     * @param consumerKey FatSecret client ID
     * @param consumerSecret FatSecret client secret
     * @param token OAuth token (request or access)
     * @param tokenSecret OAuth token secret (request or access)
     */
    fun buildAuthorizationHeader(
        method: String,
        url: String,
        extraParams: Map<String, String>,
        consumerKey: String,
        consumerSecret: String,
        token: String? = null,
        tokenSecret: String? = null
    ): String {
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        val nonce = java.util.UUID.randomUUID().toString().replace("-", "")

        val oauthParams = mutableMapOf(
            "oauth_consumer_key" to consumerKey,
            "oauth_signature_method" to SIGNATURE_METHOD,
            "oauth_timestamp" to timestamp,
            "oauth_nonce" to nonce,
            "oauth_version" to VERSION
        )
        if (token != null) oauthParams["oauth_token"] = token
        val allParams = (oauthParams + extraParams).toSortedMap()

        val paramString = allParams.entries.joinToString("&") { (k, v) ->
            "${percentEncode(k)}=${percentEncode(v)}"
        }
        val signatureBase = "${method.uppercase(Locale.US)}&${percentEncode(normalizeUrl(url))}&${percentEncode(paramString)}"
        val signingKey = "${percentEncode(consumerSecret)}&${tokenSecret?.let { percentEncode(it) } ?: ""}"
        val signature = signHmacSha1(signatureBase, signingKey)

        val authParams = (oauthParams + ("oauth_signature" to signature)).toSortedMap()
        return "OAuth " + authParams.entries.joinToString(", ") { (k, v) ->
            "${percentEncode(k)}=\"${percentEncode(v)}\""
        }
    }

    fun percentEncode(s: String): String =
        URLEncoder.encode(s, Charsets.UTF_8)
            .replace("+", "%20")
            .replace("*", "%2A")
            .replace("%7E", "~")

    private fun normalizeUrl(url: String): String {
        val questionMark = url.indexOf('?')
        return if (questionMark >= 0) url.substring(0, questionMark) else url
    }

    private fun signHmacSha1(data: String, key: String): String {
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA1"))
        val bytes = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
    }
}
