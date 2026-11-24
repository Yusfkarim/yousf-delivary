package com.yousif.delivery.util

import com.yousif.delivery.data.Customer
import com.yousif.delivery.data.ExtraPhone
import com.yousif.delivery.data.Kinds
import com.yousif.delivery.data.SyncStatus
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

/**
 * Plain-JSON backup: one file with every customer and shop, phones, messenger
 * flags and coordinates. It is deliberately human readable so the driver can
 * keep it in Drive / Telegram and restore it on a new phone.
 */
object BackupCodec {

    const val FILE_PREFIX = "yousif-delivery-backup"

    data class Entry(
        val id: String? = null,
        val kind: String? = null,
        val name: String? = null,
        val primaryPhone: String? = null,
        val hasWhatsapp: Boolean = false,
        val hasViber: Boolean = false,
        val extraPhones: List<Extra>? = null,
        val latitude: Double = 0.0,
        val longitude: Double = 0.0,
        val addressNote: String? = null,
        val createdAt: Long = 0L,
        val updatedAt: Long = 0L,
    )

    data class Extra(
        val number: String? = null,
        val hasWhatsapp: Boolean = false,
        val hasViber: Boolean = false,
    )

    data class Payload(
        val app: String = "yousif-delivery",
        val schema: Int = 2,
        val exportedAt: Long = System.currentTimeMillis(),
        val records: List<Entry>? = null,
    )

    private val gson: Gson = GsonBuilder().setPrettyPrinting().serializeNulls().create()

    fun fileName(): String {
        val stamp = java.text.SimpleDateFormat("yyyy-MM-dd-HHmm", java.util.Locale.US)
            .format(java.util.Date())
        return "$FILE_PREFIX-$stamp.json"
    }

    fun encode(records: List<Customer>): String {
        val payload = Payload(
            records = records.map { customer ->
                Entry(
                    id = customer.id,
                    kind = Kinds.normalize(customer.kind),
                    name = customer.name,
                    primaryPhone = customer.primaryPhone,
                    hasWhatsapp = customer.hasWhatsapp,
                    hasViber = customer.hasViber,
                    extraPhones = customer.extraPhones.map {
                        Extra(it.number, it.hasWhatsapp, it.hasViber)
                    },
                    latitude = customer.latitude,
                    longitude = customer.longitude,
                    addressNote = customer.addressNote,
                    createdAt = customer.createdAt,
                    updatedAt = customer.updatedAt,
                )
            },
        )
        return gson.toJson(payload)
    }

    fun decodeToCustomers(json: String): List<Customer> {
        val text = json.trim()
        if (text.isEmpty()) return emptyList()
        val entries: List<Entry> = try {
            if (text.startsWith("[")) {
                val type = object : TypeToken<List<Entry>>() {}.type
                gson.fromJson<List<Entry>>(text, type).orEmpty()
            } else {
                gson.fromJson(text, Payload::class.java)?.records.orEmpty()
            }
        } catch (error: Exception) {
            emptyList()
        }
        val now = System.currentTimeMillis()
        return entries.mapNotNull { entry ->
            val name = entry.name?.trim().orEmpty()
            if (name.isEmpty()) return@mapNotNull null
            Customer(
                id = entry.id?.takeIf { it.isNotBlank() } ?: java.util.UUID.randomUUID().toString(),
                kind = Kinds.normalize(entry.kind),
                name = name,
                primaryPhone = entry.primaryPhone?.trim().orEmpty(),
                hasWhatsapp = entry.hasWhatsapp,
                hasViber = entry.hasViber,
                extraPhones = entry.extraPhones.orEmpty().mapNotNull { extra ->
                    val number = extra.number?.trim().orEmpty()
                    if (number.isEmpty()) null else ExtraPhone(number, extra.hasWhatsapp, extra.hasViber)
                },
                latitude = entry.latitude,
                longitude = entry.longitude,
                addressNote = entry.addressNote?.trim().orEmpty(),
                createdAt = if (entry.createdAt > 0) entry.createdAt else now,
                updatedAt = if (entry.updatedAt > 0) entry.updatedAt else now,
                syncStatus = SyncStatus.PENDING_UPSERT,
            )
        }
    }
}
