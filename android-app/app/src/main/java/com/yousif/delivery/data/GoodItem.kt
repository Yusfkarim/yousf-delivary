package com.yousif.delivery.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * One row of «کاڵای کڕیار» — something the driver has to carry for a person.
 *
 * It is deliberately a plain checklist item: a title, how many, an optional note
 * and *optionally* a customer from the phonebook (it can stay empty, so the
 * driver can write a load before he knows who it belongs to).
 *
 * Everything is stored on the phone only (Room): goods never leave the device.
 */
@Entity(tableName = "goods")
data class GoodEntity(
    @PrimaryKey val id: String,
    val title: String,
    val quantity: Int,
    val note: String,
    val customerId: String?,
    val customerName: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

/** What the UI works with. */
data class GoodItem(
    val id: String,
    val title: String,
    val quantity: Int = 1,
    val note: String = "",
    val customerId: String? = null,
    val customerName: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val hasCustomer: Boolean get() = !customerName.isNullOrBlank()
    val total: Int get() = if (quantity < 0) 0 else quantity
}

fun GoodEntity.toDomain(): GoodItem = GoodItem(
    id = id,
    title = title,
    quantity = quantity,
    note = note,
    customerId = customerId,
    customerName = customerName,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun GoodItem.toEntity(): GoodEntity = GoodEntity(
    id = id.ifBlank { UUID.randomUUID().toString() },
    title = title.trim(),
    quantity = quantity.coerceAtLeast(0),
    note = note,
    customerId = customerId,
    customerName = customerName,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

@Dao
interface GoodDao {

    @Query("SELECT * FROM goods ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<GoodEntity>>

    @Query("SELECT * FROM goods")
    suspend fun allOnce(): List<GoodEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: GoodEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<GoodEntity>)

    @Query("DELETE FROM goods WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE goods SET quantity = :quantity, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setQuantity(id: String, quantity: Int, updatedAt: Long)

    /** When a customer is removed, his goods stay but lose the link. */
    @Query("UPDATE goods SET customerId = NULL, customerName = NULL WHERE customerId = :customerId")
    suspend fun detachCustomer(customerId: String)
}
