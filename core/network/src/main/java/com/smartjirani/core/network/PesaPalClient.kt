package com.smartjirani.core.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

data class PesaPalOrder(
    val orderTrackingId: String,
    val merchantReference: String,
    val redirectUrl: String,
    val responseCode: String,
    val responseMessage: String,
)

data class PesaPalPaymentStatus(
    val orderTrackingId: String,
    val status: String,
    val amount: String?,
    val paymentMethod: String?,
    val description: String?,
) {
    val isCompleted: Boolean get() = status.equals("COMPLETED", ignoreCase = true)
    val isFailed: Boolean get() = status.equals("FAILED", ignoreCase = true)
    val isPending: Boolean get() = !isCompleted && !isFailed
}

@Serializable
private data class TokenRequest(
    @SerialName("consumer_key") val consumerKey: String,
    @SerialName("consumer_secret") val consumerSecret: String,
)

@Serializable
private data class TokenResponse(
    val token: String? = null,
)

@Serializable
private data class OrderRequest(
    @SerialName("idempotency_key") val idempotencyKey: String,
    val currency: String = "KES",
    val amount: Double,
    val description: String,
    @SerialName("callback_url") val callbackUrl: String = "",
    @SerialName("notification_id") val notificationId: String = "",
    @SerialName("return_url") val returnUrl: String = "",
    val reference: String,
)

@Serializable
private data class OrderResponse(
    @SerialName("order_tracking_id") val orderTrackingId: String? = null,
    @SerialName("merchant_reference") val merchantReference: String? = null,
    @SerialName("redirect_url") val redirectUrl: String? = null,
    @SerialName("response_code") val responseCode: String? = null,
    @SerialName("response_message") val responseMessage: String? = null,
)

@Serializable
private data class StatusResponse(
    @SerialName("order_tracking_id") val orderTrackingId: String? = null,
    val status: String? = null,
    val amount: String? = null,
    @SerialName("payment_method") val paymentMethod: String? = null,
    val description: String? = null,
    @SerialName("response_message") val responseMessage: String? = null,
)

/**
 * Minimal PesaPal v3 REST client: bearer-token auth, order creation, status polling.
 * All calls are suspend functions so repositories can decide retry/refresh policy.
 */
class PesaPalClient(
    private val config: PesaPalConfig,
) {
    val isConfigured: Boolean get() = config.isConfigured

    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }

    private val httpClient: HttpClient by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) { json(json) }
            expectSuccess = false
        }
    }

    @Volatile
    private var cachedToken: String? = null

    private suspend fun token(): String {
        cachedToken?.let { return it }
        val response: TokenResponse =
            httpClient
                .post("${config.baseUrl}/auth/RequestToken") {
                    contentType(ContentType.Application.Json)
                    setBody(TokenRequest(config.consumerKey, config.consumerSecret))
                }.body()
        return response.token?.also { cachedToken = it }
            ?: throw IllegalStateException("PesaPal returned no token")
    }

    suspend fun createOrder(
        amountKes: Double,
        description: String,
        reference: String,
        returnUrl: String = "",
    ): PesaPalOrder {
        val body =
            OrderRequest(
                idempotencyKey = UUID.randomUUID().toString(),
                amount = amountKes,
                description = description,
                callbackUrl = config.callbackUrl,
                returnUrl = returnUrl,
                reference = reference,
            )
        val response: OrderResponse =
            httpClient
                .post("${config.baseUrl}/Transactions/OrderRequest") {
                    header(HttpHeaders.Authorization, "Bearer ${token()}")
                    header("X-Idempotency-Key", body.idempotencyKey)
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }.body()
        val trackingId =
            response.orderTrackingId
                ?: throw IllegalStateException(response.responseMessage ?: "PesaPal order failed")
        return PesaPalOrder(
            orderTrackingId = trackingId,
            merchantReference = response.merchantReference ?: reference,
            redirectUrl = response.redirectUrl.orEmpty(),
            responseCode = response.responseCode.orEmpty(),
            responseMessage = response.responseMessage.orEmpty(),
        )
    }

    suspend fun getPaymentStatus(orderTrackingId: String): PesaPalPaymentStatus {
        val response: StatusResponse =
            httpClient
                .get("${config.baseUrl}/Transactions/GetTransactionStatus") {
                    header(HttpHeaders.Authorization, "Bearer ${token()}")
                    header("X-Idempotency-Key", UUID.randomUUID().toString())
                    url {
                        parameters.append("OrderTrackingId", orderTrackingId)
                    }
                }.body()
        return PesaPalPaymentStatus(
            orderTrackingId = response.orderTrackingId ?: orderTrackingId,
            status = response.status ?: "UNKNOWN",
            amount = response.amount,
            paymentMethod = response.paymentMethod,
            description = response.description,
        )
    }
}
