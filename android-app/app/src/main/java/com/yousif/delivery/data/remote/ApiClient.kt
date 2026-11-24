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
                if (response.isSuccessful) {
                    ApiResponse(ok = true, code = response.code, value = text)
                } else {
                    val parsed = runCatching { gson.fromJson(text, ErrorResponse::class.java) }.getOrNull()
                    ApiResponse.failure(response.code, parsed?.message ?: parsed?.error)
                }
            }
        } catch (error: IOException) {
            ApiResponse.failure(0, error.message)
        } catch (error: Exception) {
            ApiResponse.failure(0, error.message)
        }
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
