package com.smartjirani.core.common.result

import kotlinx.coroutines.delay

inline fun <T, R> Resource<T>.map(transform: (T) -> R): Resource<R> =
    when (this) {
        is Resource.Success -> Resource.Success(transform(data))
        is Resource.Error -> this
        is Resource.Loading -> this
    }

/**
 * Retry transient server errors (e.g., PGRST303 JWT clock skew) with exponential backoff.
 */
private const val MAX_RETRIES = 2
private const val INITIAL_DELAY_MS = 1000L

suspend fun <T> safeCall(block: suspend () -> T): Resource<T> {
    var lastException: Exception? = null
    repeat(MAX_RETRIES + 1) { attempt ->
        try {
            return Resource.Success(block())
        } catch (e: Exception) {
            lastException = e
            val message = e.message ?: ""
            // Retry on transient JWT clock skew errors from PostgREST
            if (message.contains("JWT issued at future", ignoreCase = true) && attempt < MAX_RETRIES) {
                delay(INITIAL_DELAY_MS * (1 shl attempt))
            } else {
                return Resource.Error(presentableMessage(e), e)
            }
        }
    }
    return Resource.Error(presentableMessage(lastException!!), lastException)
}

/**
 * Postgrest/Ktor failures carry the full request dump — URL, query and headers,
 * including the bearer token. Banners must never show that, so keep only the
 * leading cause and cap it.
 */
private const val MAX_ERROR_MESSAGE = 200

private fun presentableMessage(e: Exception): String {
    val raw = e.message ?: return "Unknown error"
    val cause =
        raw
            // Postgrest appends the request URL and headers (incl. the bearer token) after the cause.
            .replace(Regex("""(?is)\s*url\s*:.*"""), " ")
            .replace(Regex("""(?is)\s*headers?\s*[:=].*"""), " ")
            .replace(Regex("""(?i)authorization\s*[=:]\s*\S+"""), " ")
            .trim()
            .replace(Regex("""\s{2,}"""), " ")
    return cause.take(MAX_ERROR_MESSAGE).ifBlank { "Unknown error" }
}
