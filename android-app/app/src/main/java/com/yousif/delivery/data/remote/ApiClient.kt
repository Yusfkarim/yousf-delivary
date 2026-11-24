package com.yousif.delivery.data.remote

import com.google.gson.Gson
import com.yousif.delivery.data.Customer
import com.yousif.delivery.data.ExtraPhone
import com.yousif.delivery.data.SyncStatus
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Result of one API call.
 * [code] == 0 means the request never reached the server (offline / timeout).
 */
data class ApiResponse<T>(
    val ok: Boolean,
    val code: Int,
    val value: T?,
    val message: String? = null,
) {
    companion object {
        fun <T> failure(code: Int, message: String?): ApiResponse<T> =
            ApiResponse(ok = false, code = code, value = null, message = message)
    }
}

fun Customer.toDto(): CustomerDto = CustomerDto(
    id = id,
    name = name,
    primaryPhone = primaryPhone,
    hasWhatsapp = hasWhatsapp,
    hasViber = hasViber,
    extraPhones = extraPhones.map { ExtraPhoneDto(it.number, it.hasWhatsapp, it.hasViber) },
    latitude = latitude,
    longitude = longitude,
    addressNote = addressNote,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun CustomerDto.toDomain(syncStatus: SyncStatus): Customer = Customer(
    id = id ?: "",
    name = name ?: "",
    primaryPhone = primaryPhone ?: "",
    hasWhatsapp = hasWhatsapp,
    hasViber = hasViber,
    extraPhones = extraPhones.orEmpty().mapNotNull { dto ->
        val number = dto.number ?: return@mapNotNull null
        ExtraPhone(number, dto.hasWhatsapp, dto.hasViber)
    },
    latitude = latitude,
    longitude = longitude,
    addressNote = addressNote ?: "",
    createdAt = if (createdAt > 0) createdAt else System.currentTimeMillis(),
    updatedAt = if (updatedAt > 0) updatedAt else System.currentTimeMillis(),
    syncStatus = syncStatus,
)

/**
 * Thin OkHttp + Gson client for the Vercel API.
 * Every call carries the `x-app-password` header.
 */
class ApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        // Vercel Deployment Protection answers with a 302 to vercel.com. Follow it
        // ourselves so the app can tell "protected" apart from "offline".
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    private val gson = Gson()
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    fun verify(baseUrl: String, password: String): ApiResponse<Boolean> {
        val raw = execute(baseUrl, "POST", "/api/auth/verify", password, null)
        if (!raw.ok) return ApiResponse.failure(raw.code, raw.message)
        val parsed = parse<VerifyResponse>(raw.value)
        return ApiResponse(ok = true, code = raw.code, value = parsed?.authenticated == true)
    }

    fun list(baseUrl: String, password: String): ApiResponse<List<CustomerDto>> {
        val raw = execute(baseUrl, "GET", "/api/customers", password, null)
        if (!raw.ok) return ApiResponse.failure(raw.code, raw.message)
        val parsed = parse<CustomerListResponse>(raw.value)
        return ApiResponse(ok = true, code = raw.code, value = parsed?.customers.orEmpty())
    }

    fun upsert(baseUrl: String, password: String, customer: Customer): ApiResponse<CustomerDto> {
        val body = gson.toJson(customer.toDto())
        val raw = execute(baseUrl, "POST", "/api/customers", password, body)
        if (!raw.ok) return ApiResponse.failure(raw.code, raw.message)
        val parsed = parse<CustomerResponse>(raw.value)
        val saved = parsed?.customer
            ?: return ApiResponse.failure(500, "invalid response")
        return ApiResponse(ok = true, code = raw.code, value = saved)
    }

    fun delete(baseUrl: String, password: String, id: String): ApiResponse<Boolean> {
        val raw = execute(baseUrl, "DELETE", "/api/customers/$id", password, null)
        if (!raw.ok) return ApiResponse.failure(raw.code, raw.message)
        return ApiResponse(ok = true, code = raw.code, value = true)
    }

    private fun execute(
        baseUrl: String,
        method: String,
        path: String,
        password: String,
        body: String?,
    ): ApiResponse<String> {
        val url = baseUrl.trim().trimEnd('/') + path
        val request = try {
            val builder = Request.Builder()
                .url(url)
                .header("x-app-password", password)
                .header("Accept", "application/json")
            when (method) {
                "GET" -> builder.get()
                "DELETE" -> builder.delete()
                else -> builder.method(method, (body ?: "{}").toRequestBody(jsonMedia))
            }
            builder.build()
        } catch (error: IllegalArgumentException) {
            return ApiResponse.failure(0, "نیشانی سێرڤەر هەڵەیە")
        }

        return try {
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                val trimmed = text.trimStart()
                val location = response.header("Location").orEmpty()

                // Vercel sends 302/307 to vercel.com/sso when Deployment Protection is on.
                if (response.code in 300..399 || location.contains("vercel.com/sso", ignoreCase = true)) {
                    ApiResponse.failure(CODE_PROTECTED, location)
                } else if (response.isSuccessful) {
                    if (trimmed.isEmpty() || trimmed.startsWith("{") || trimmed.startsWith("[")) {
                        ApiResponse(ok = true, code = response.code, value = text.ifBlank { "{}" })
                    } else {
                        // 200 with an HTML login page = protected (or a wrong host).
                        ApiResponse.failure(CODE_PROTECTED, "non-json response")
                    }
                } else {
                    val parsed = runCatching { gson.fromJson(text, ErrorResponse::class.java) }.getOrNull()
                    ApiResponse.failure(response.code, parsed?.message ?: parsed?.error ?: "HTTP ${response.code}")
                }
            }
        } catch (error: IOException) {
            ApiResponse.failure(0, error.message)
        } catch (error: Exception) {
            ApiResponse.failure(0, error.message)
        }
    }

    companion object {
        /** The request never reached the server (no network / DNS / TLS). */
        const val CODE_OFFLINE = 0

        /** The server answered with Vercel's SSO redirect or an HTML login page. */
        const val CODE_PROTECTED = -1
    }

    private inline fun <reified T> parse(text: String?): T? {
        if (text.isNullOrBlank()) return null
        return try {
            gson.fromJson(text, T::class.java)
        } catch (error: Exception) {
            null
        }
    }
}
