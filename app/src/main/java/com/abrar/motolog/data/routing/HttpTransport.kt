package com.abrar.motolog.data.routing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

/**
 * Lightweight abstraction over HTTP networking.
 * Enables easy mock testing and prevents tight coupling to a specific HTTP client.
 */
interface HttpTransport {
    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): HttpResponse
}

data class HttpResponse(
    val statusCode: Int,
    val body: String
)

/**
 * Standard Android/JVM HTTP transport using [HttpURLConnection].
 * Zero extra dependencies, fully asynchronous on [Dispatchers.IO], with timeout and size bounds.
 */
@Singleton
class DefaultHttpTransport @Inject constructor() : HttpTransport {

    companion object {
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 15_000
        private const val MAX_RESPONSE_BYTES = 5 * 1024 * 1024 // 5 MB
    }

    override suspend fun get(url: String, headers: Map<String, String>): HttpResponse =
        withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                coroutineContext.ensureActive()
                val targetUrl = URL(url)
                connection = (targetUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    useCaches = false
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "MotoLog-Android/2.0")
                    for ((k, v) in headers) {
                        setRequestProperty(k, v)
                    }
                }

                coroutineContext.ensureActive()
                val statusCode = connection.responseCode

                val stream = if (statusCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream ?: connection.inputStream
                }

                val body = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                    val builder = StringBuilder()
                    val buffer = CharArray(4096)
                    var bytesRead = 0
                    var totalChars = 0

                    while (reader.read(buffer).also { bytesRead = it } != -1) {
                        coroutineContext.ensureActive()
                        totalChars += bytesRead
                        if (totalChars > MAX_RESPONSE_BYTES) {
                            throw IllegalStateException("Response payload exceeded maximum allowed size of $MAX_RESPONSE_BYTES bytes")
                        }
                        builder.append(buffer, 0, bytesRead)
                    }
                    builder.toString()
                }

                HttpResponse(statusCode = statusCode, body = body)
            } catch (e: SocketTimeoutException) {
                throw e
            } finally {
                connection?.disconnect()
            }
        }
}
