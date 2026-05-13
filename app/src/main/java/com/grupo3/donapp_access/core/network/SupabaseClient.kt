package com.grupo3.donapp_access.core.network

import com.grupo3.donapp_access.BuildConfig
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object SupabaseClient {
    private const val API_PATH = "rest/v1"
    private const val CONNECT_TIMEOUT_MS = 12_000
    private const val READ_TIMEOUT_MS = 12_000

    fun get(table: String, query: Map<String, String> = emptyMap()): String {
        val queryString = query.entries.joinToString("&") { (key, value) ->
            "${key.urlEncode()}=${value.urlEncode()}"
        }
        val endpoint = buildString {
            append(BuildConfig.SUPABASE_URL.trimEnd('/'))
            append('/')
            append(API_PATH)
            append('/')
            append(table)
            if (queryString.isNotEmpty()) {
                append('?')
                append(queryString)
            }
        }

        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            setRequestProperty("Accept", "application/json")
        }

        return connection.useResponse { code, body ->
            if (code in 200..299) {
                body
            } else {
                throw IOException("Supabase HTTP $code: $body")
            }
        }
    }

    private fun String.urlEncode(): String = URLEncoder.encode(this, "UTF-8")

    private inline fun HttpURLConnection.useResponse(
        block: (code: Int, body: String) -> String
    ): String {
        return try {
            val code = responseCode
            val stream = if (code in 200..299) inputStream else errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            block(code, body)
        } finally {
            disconnect()
        }
    }
}
