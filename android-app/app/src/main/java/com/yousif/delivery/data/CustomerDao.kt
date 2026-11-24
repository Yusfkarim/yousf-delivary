package com.yousif.delivery.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {

    /** Everything the UI shows (pending deletes stay hidden until the server confirms). */
    @Query("SELECT * FROM customers WHERE syncStatus != 'PENDING_DELETE' ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers")
    suspend fun allOnce(): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun byId(id: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE syncStatus != 'SYNCED'")
    suspend fun pending(): List<CustomerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CustomerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<CustomerEntity>)

    @Query("UPDATE customers SET syncStatus = :status WHERE id = :id")
    suspend fun setStatus(id: String, status: String)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteById(id: String)
}
