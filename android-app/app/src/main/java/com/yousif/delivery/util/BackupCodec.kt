package com.yousif.delivery.util

import com.yousif.delivery.data.Customer
import com.yousif.delivery.data.ExtraPhone
import com.yousif.delivery.data.GoodItem
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

    /** «کاڵای کڕیار» travels in the same file. */
    data class GoodEntry(
        val id: String? = null,
        val title: String? = null,
        val quantity: Int = 1,
        val note: String? = null,
        val customerId: String? = null,
        val customerName: String? = null,
        val createdAt: Long = 0L,
        val updatedAt: Long = 0L,
    )

    data class Payload(
        val app: String = "yousif-delivery",
        val schema: Int = 3,
        val exportedAt: Long = System.currentTimeMillis(),
        val records: List<Entry>? = null,
        val goods: List<GoodEntry>? = null,
    )

    private val gson: Gson = GsonBuilder().setPrettyPrinting().serializeNulls().create()

    fun fileName(): String {
        val stamp = java.text.SimpleDateFormat("yyyy-MM-dd-HHmm", java.util.Locale.US)
            .format(java.util.Date())
        return "$FILE_PREFIX-$stamp.json"
    }

    fun encode(records: List<Customer>, goods: List<GoodItem> = emptyList()): String {
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
            goods = goods.map { item ->
                GoodEntry(
                    id = item.id,
                    title = item.title,
                    quantity = item.quantity,
                    note = item.note,
                    customerId = item.customerId,
                    customerName = item.customerName,
                    createdAt = item.createdAt,
                    updatedAt = item.updatedAt,
                )
            },
        )
        return gson.toJson(payload)
    }

    fun decodeGoods(json: String): List<GoodItem> {
        val text = json.trim()
        if (text.isEmpty() || text.startsWith("[")) return emptyList()
        val payload = try {
            gson.fromJson(text, Payload::class.java)
        } catch (error: Exception) {
            null
        }
        val now = System.currentTimeMillis()
        var mediaSeq = 0
        return payload?.goods.orEmpty().mapNotNull { entry ->
            val title = entry.title?.trim().orEmpty()
            if (title.isEmpty()) return@mapNotNull null
            mediaSeq += 1
            GoodItem(
                id = entry.id?.takeIf { it.isNotBlank() } ?: java.util.UUID.randomUUID().toString(),
                title = title,
                quantity = entry.quantity.coerceIn(0, 9999),
                note = entry.note?.trim().orEmpty(),
                customerId = entry.customerId?.takeIf { it.isNotBlank() },
                customerName = entry.customerName?.takeIf { it.isNotBlank() },
                createdAt = if (entry.createdAt > 0) entry.createdAt else now + mediaSeq,
                updatedAt = if (entry.updatedAt > 0) entry.updatedAt else now + mediaSeq,
            )
        }
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
