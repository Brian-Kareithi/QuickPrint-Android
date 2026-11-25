package com.quickprint.app.data

import com.quickprint.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/** An error with a message that is safe to show to the customer. */
class ApiException(message: String, val status: Int = 0) : IOException(message)

/** Thin HTTP client for the QuickPrint backend (same endpoints the website uses). */
object Api {
    val baseUrl: String = BuildConfig.API_BASE_URL.trimEnd('/')
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.MINUTES)
        .build()

    suspend fun get(path: String, headers: Map<String, String> = emptyMap()): String =
        execute(builder(path, headers).get().build())

    suspend fun send(method: String, path: String, body: JSONObject? = null, headers: Map<String, String> = emptyMap()): String =
        execute(builder(path, headers).method(method, (body ?: JSONObject()).toString().toRequestBody(jsonType)).build())

    suspend fun post(path: String, body: RequestBody): String =
        execute(builder(path, emptyMap()).post(body).build())

    private fun builder(path: String, headers: Map<String, String>): Request.Builder {
        val b = Request.Builder().url(baseUrl + path).header("Accept", "application/json")
        headers.forEach { (k, v) -> b.header(k, v) }
        return b
    }

    private suspend fun execute(request: Request): String = withContext(Dispatchers.IO) {
        try {
            http.newCall(request).execute().use { res ->
                val text = res.body?.string().orEmpty()
                if (!res.isSuccessful) throw ApiException(errorMessage(res.code, text), res.code)
                text
            }
        } catch (e: ApiException) {
            throw e
        } catch (e: SocketTimeoutException) {
            throw ApiException("The server took too long to respond. Please try again.")
        } catch (e: IOException) {
            throw ApiException("Can't reach the server. Check your internet connection and try again.")
        }
    }

    private fun errorMessage(code: Int, body: String): String {
        if (code == 502 || code == 503 || code == 504) return "The server is temporarily unavailable. Please try again in a moment."
        val msg = runCatching {
            when (val m = JSONObject(body).opt("message")) {
                is JSONArray -> (0 until m.length()).joinToString(", ") { m.optString(it) }
                is String -> m
                else -> null
            }
        }.getOrNull()
        return if (msg.isNullOrBlank()) "Request failed (HTTP $code)" else msg
    }
}

fun JSONObject.str(key: String): String? = if (isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }
