package com.yousif.delivery.data.remote

import com.google.gson.Gson
import com.yousif.delivery.BuildConfig
import com.yousif.delivery.data.Customer
import com.yousif.delivery.data.ExtraPhone
import com.yousif.delivery.data.Kinds
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
    /** The address that actually answered — saved so the next call is direct. */
    val baseUrl: String? = null,
) {
    companion object {
        fun <T> failure(code: Int, message: String?, baseUrl: String? = null): ApiResponse<T> =
            ApiResponse(ok = false, code = code, value = null, message = message, baseUrl = baseUrl)
    }
}

fun Customer.toDto(): CustomerDto = CustomerDto(
    id = id,
    kind = Kinds.normalize(kind),
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
    kind = Kinds.normalize(kind),
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
 *
 * Every call carries the `x-app-password` header, and every call is **resilient**:
 * if the saved address answers `404` (a URL saved with `/api` on the end, a moved
 * deployment, an old build) the same request is retried against the clean host and
 * the built-in default until one of them really answers. Whichever address wins is
 * returned in [ApiResponse.baseUrl] so it can be stored and used directly next time.
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
        val raw = run(baseUrl, "POST", "/api/auth/verify", password, null)
        if (!raw.ok) return ApiResponse.failure(raw.code, raw.message, raw.baseUrl)
        val parsed = parse<VerifyResponse>(raw.value)
        return ApiResponse(
            ok = true,
            code = raw.code,
            value = parsed?.authenticated == true,
            baseUrl = raw.baseUrl,
        )
    }

    fun list(baseUrl: String, password: String): ApiResponse<List<CustomerDto>> {
        val raw = run(baseUrl, "GET", "/api/customers", password, null)
        if (!raw.ok) return ApiResponse.failure(raw.code, raw.message, raw.baseUrl)
        val parsed = parse<CustomerListResponse>(raw.value)
        return ApiResponse(
            ok = true,
            code = raw.code,
            value = parsed?.customers.orEmpty(),
            baseUrl = raw.baseUrl,
        )
    }

    fun upsert(baseUrl: String, password: String, customer: Customer): ApiResponse<CustomerDto> {
        val body = gson.toJson(customer.toDto())
        val raw = run(baseUrl, "POST", "/api/customers", password, body)
        if (!raw.ok) return ApiResponse.failure(raw.code, raw.message, raw.baseUrl)
        val parsed = parse<CustomerResponse>(raw.value)
        val saved = parsed?.customer
            ?: return ApiResponse.failure(500, "invalid response", raw.baseUrl)
        return ApiResponse(ok = true, code = raw.code, value = saved, baseUrl = raw.baseUrl)
    }

    fun delete(baseUrl: String, password: String, id: String): ApiResponse<Boolean> {
        val raw = run(baseUrl, "DELETE", "/api/customers/$id", password, null)
        if (!raw.ok) return ApiResponse.failure(raw.code, raw.message, raw.baseUrl)
        return ApiResponse(ok = true, code = raw.code, value = true, baseUrl = raw.baseUrl)
    }

    /* ------------------------------------------------------- address handling */

    /** Fixes the shape of a typed address: scheme, spaces, trailing slashes. */
    fun sanitize(raw: String): String {
        var value = raw.trim().trimEnd('/')
        if (value.isEmpty()) return ""
        if (!value.startsWith("http://", ignoreCase = true) &&
            !value.startsWith("https://", ignoreCase = true)
        ) {
            value = "https://$value"
        }
        return value.trimEnd('/')
    }

    /** `…/api`, `…/api/customers`, `…/api/auth/verify` are addresses *with a route*. */
    fun bareHost(raw: String): String {
        var value = sanitize(raw)
        for (suffix in listOf("/api/customers", "/api/auth/verify", "/api/health", "/api")) {
            if (value.endsWith(suffix, ignoreCase = true)) {
                value = value.dropLast(suffix.length)
                break
            }
        }
        return value.trimEnd('/')
    }

    /**
     * Every address worth trying for [saved], best first and without duplicates:
     * the saved one, the same host without a route, then the built-in default.
     */
    fun candidates(saved: String): List<String> {
        val list = mutableListOf<String>()
        fun add(candidate: String) {
            val clean = sanitize(candidate)
            if (clean.isNotEmpty() && list.none { existing -> existing.equals(clean, ignoreCase = true) }) {
                list += clean
            }
        }
        add(saved)
        add(bareHost(saved))
        add(BuildConfig.API_BASE_URL)
        return list
    }

    /* ------------------------------------------------------------- transport */

    private data class Raw(
        val ok: Boolean,
        val code: Int,
        val value: String?,
        val message: String?,
        val baseUrl: String,
    )

    /**
     * Sends one request. A `404` / "protected" answer only means *this address*
     * is wrong, so the next candidate is tried; a `401`, `429` or a timeout is the
     * same everywhere and is returned straight away.
     */
    private fun run(
        baseUrl: String,
        method: String,
        path: String,
        password: String,
        body: String?,
    ): Raw {
        var last: Raw? = null
        for (candidate in candidates(baseUrl)) {
            val result = attempt(candidate, method, path, password, body)
            if (result.ok) return result
            last = result
            if (result.code != 404 && result.code != CODE_PROTECTED) return result
        }
        return last ?: Raw(false, CODE_OFFLINE, null, "no address", sanitize(baseUrl))
    }

    private fun attempt(
        baseUrl: String,
        method: String,
        path: String,
        password: String,
        body: String?,
    ): Raw {
        val url = sanitize(baseUrl) + path
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
            return Raw(false, CODE_OFFLINE, null, "نیشانی سێرڤەر هەڵەیە", baseUrl)
        }

        return try {
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                val trimmed = text.trimStart()
                val location = response.header("Location").orEmpty()

                // Vercel sends 302/307 to vercel.com/sso when Deployment Protection is on.
                if (response.code in 300..399 || location.contains("vercel.com/sso", ignoreCase = true)) {
                    Raw(false, CODE_PROTECTED, null, location, baseUrl)
                } else if (response.isSuccessful) {
                    if (trimmed.isEmpty() || trimmed.startsWith("{") || trimmed.startsWith("[")) {
                        Raw(true, response.code, text.ifBlank { "{}" }, null, baseUrl)
                    } else {
                        // 200 with an HTML login page = protected (or a wrong host).
                        Raw(false, CODE_PROTECTED, null, "non-json response", baseUrl)
                    }
                } else {
                    val parsed = runCatching { gson.fromJson(text, ErrorResponse::class.java) }.getOrNull()
                    Raw(
                        ok = false,
                        code = response.code,
                        value = null,
                        message = parsed?.message ?: parsed?.error ?: "HTTP ${response.code}",
                        baseUrl = baseUrl,
                    )
                }
            }
        } catch (error: IOException) {
            Raw(false, CODE_OFFLINE, null, error.message, baseUrl)
        } catch (error: Exception) {
            Raw(false, CODE_OFFLINE, null, error.message, baseUrl)
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
