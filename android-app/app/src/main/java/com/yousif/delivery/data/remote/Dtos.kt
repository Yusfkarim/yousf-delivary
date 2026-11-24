package com.yousif.delivery.data.remote

data class ExtraPhoneDto(
    val number: String? = null,
    val hasWhatsapp: Boolean = false,
    val hasViber: Boolean = false,
)

data class CustomerDto(
    val id: String? = null,
    val kind: String? = null,
    val name: String? = null,
    val primaryPhone: String? = null,
    val hasWhatsapp: Boolean = false,
    val hasViber: Boolean = false,
    val extraPhones: List<ExtraPhoneDto>? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val addressNote: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

data class CustomerListResponse(
    val customers: List<CustomerDto>? = null,
    val count: Int = 0,
)

data class CustomerResponse(val customer: CustomerDto? = null)

data class VerifyResponse(val authenticated: Boolean = false)

data class ErrorResponse(val error: String? = null, val message: String? = null)
