package com.smartjirani.feature.civic.channels

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

data class HttpResponse(
    val status: Int,
    val body: String,
)

fun FakeHttp(handler: suspend (url: String, body: String) -> HttpResponse): HttpClient {
    val engine =
        MockEngine { request ->
            val url = request.url.toString()
            val body =
                when (val content = request.body) {
                    is io.ktor.http.content.TextContent -> content.text
                    else -> ""
                }
            val resp = handler(url, body)
            respond(
                content = resp.body,
                status = HttpStatusCode.fromValue(resp.status),
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
    return HttpClient(engine)
}
