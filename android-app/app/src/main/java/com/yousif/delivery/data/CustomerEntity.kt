package com.yousif.delivery.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val id: String,
    val kind: String = Kinds.CUSTOMER,
    val name: String,
    val primaryPhone: String,
    val hasWhatsapp: Boolean,
    val hasViber: Boolean,
    val extraPhonesJson: String,
    val latitude: Double,
    val longitude: Double,
    val addressNote: String,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: String,
)

private val gson = Gson()
private val extraPhonesType = object : TypeToken<List<ExtraPhone>>() {}.type

fun encodeExtraPhones(value: List<ExtraPhone>): String =
    if (value.isEmpty()) "[]" else gson.toJson(value, extraPhonesType)

fun decodeExtraPhones(json: String): List<ExtraPhone> {
    if (json.isBlank()) return emptyList()
    return try {
        gson.fromJson<List<ExtraPhone>>(json, extraPhonesType) ?: emptyList()
    } catch (error: Exception) {
        emptyList()
    }
}

fun CustomerEntity.toDomain(): Customer = Customer(
    id = id,
    kind = Kinds.normalize(kind),
    name = name,
    primaryPhone = primaryPhone,
    hasWhatsapp = hasWhatsapp,
    hasViber = hasViber,
    extraPhones = decodeExtraPhones(extraPhonesJson),
    latitude = latitude,
    longitude = longitude,
    addressNote = addressNote,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = runCatching { SyncStatus.valueOf(syncStatus) }.getOrDefault(SyncStatus.SYNCED),
)

fun Customer.toEntity(): CustomerEntity = CustomerEntity(
    id = id,
    kind = Kinds.normalize(kind),
    name = name,
    primaryPhone = primaryPhone,
    hasWhatsapp = hasWhatsapp,
    hasViber = hasViber,
    extraPhonesJson = encodeExtraPhones(extraPhones),
    latitude = latitude,
    longitude = longitude,
    addressNote = addressNote,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = syncStatus.name,
)
