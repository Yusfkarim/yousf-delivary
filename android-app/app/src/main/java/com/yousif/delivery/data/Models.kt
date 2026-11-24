package com.yousif.delivery.data

/** A secondary phone number with its own messenger flags. */
data class ExtraPhone(
    val number: String = "",
    val hasWhatsapp: Boolean = false,
    val hasViber: Boolean = false,
)

/** Local sync state for Room-first writes. */
enum class SyncStatus {
    SYNCED,
    PENDING_UPSERT,
    PENDING_DELETE,
}

/**
 * One customer in the driver's private phonebook.
 *
 * Deliberately contains no order items, prices, fees or delivery statuses —
 * this app is a contacts + GPS locator, nothing else.
 */
data class Customer(
    val id: String,
    val name: String,
    val primaryPhone: String,
    val hasWhatsapp: Boolean = false,
    val hasViber: Boolean = false,
    val extraPhones: List<ExtraPhone> = emptyList(),
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val addressNote: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
) {
    val hasCoordinates: Boolean get() = latitude != 0.0 || longitude != 0.0
    val allPhones: List<String> get() = listOf(primaryPhone) + extraPhones.map { it.number }
}

/** Values produced by the add/edit sheet. */
data class CustomerDraft(
    val id: String? = null,
    val name: String = "",
    val primaryPhone: String = "",
    val hasWhatsapp: Boolean = false,
    val hasViber: Boolean = false,
    val extraPhones: List<ExtraPhone> = emptyList(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val addressNote: String = "",
)

enum class PushOutcome { SYNCED, PENDING, UNAUTHORIZED }
