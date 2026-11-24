package com.yousif.delivery.data

import android.content.Context
import com.yousif.delivery.data.remote.ApiClient
import com.yousif.delivery.data.remote.toDomain
import com.yousif.delivery.session.SessionStore
import com.yousif.delivery.sync.SyncScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Room-first repository:
 *  1. every change lands in Room immediately (instant UI, survives app kill)
 *  2. the same change is pushed to Vercel Postgres right away
 *  3. on failure the row stays PENDING and WorkManager retries when online
 */
class CustomerRepository(
    private val context: Context,
    private val dao: CustomerDao,
    private val session: SessionStore,
    private val api: ApiClient,
) {

    val customers: Flow<List<Customer>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun save(customer: Customer): PushOutcome = withContext(Dispatchers.IO) {
        val pending = customer.copy(
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING_UPSERT,
        )
        dao.upsert(pending.toEntity())
        pushSingle(pending)
    }

    suspend fun delete(customer: Customer): PushOutcome = withContext(Dispatchers.IO) {
        dao.upsert(customer.copy(syncStatus = SyncStatus.PENDING_DELETE).toEntity())
        pushDelete(customer.id)
    }

    /** Pushes every pending row, then downloads the full cloud dataset. */
    suspend fun syncAll(): Boolean = withContext(Dispatchers.IO) {
        val pushed = pushPending()
        val pulled = pullRemote()
        pushed && pulled
    }

    suspend fun pushPending(): Boolean = withContext(Dispatchers.IO) {
        var allSynced = true
        for (entity in dao.pending()) {
            val status = runCatching { SyncStatus.valueOf(entity.syncStatus) }
                .getOrDefault(SyncStatus.SYNCED)
            val outcome = when (status) {
                SyncStatus.PENDING_DELETE -> pushDelete(entity.id)
                else -> pushSingle(entity.toDomain())
            }
            if (outcome != PushOutcome.SYNCED) allSynced = false
        }
        allSynced
    }

    /** Fetch-all used on login / fresh install / manual sync. */
    suspend fun pullRemote(): Boolean = withContext(Dispatchers.IO) {
        val result = api.list(session.baseUrl, session.password)
        if (!result.ok) return@withContext false

        val remote = result.value.orEmpty()
        val local = dao.allOnce().associateBy { it.id }
        val toWrite = remote.mapNotNull { dto ->
            val existing = local[dto.id]
            // Never overwrite a row that still has unsynced local changes.
            if (existing != null && existing.syncStatus != SyncStatus.SYNCED.name) {
                null
            } else {
                dto.toDomain(SyncStatus.SYNCED).toEntity()
            }
        }
        if (toWrite.isNotEmpty()) dao.upsertAll(toWrite)
        true
    }

    private suspend fun pushSingle(customer: Customer): PushOutcome {
        val result = api.upsert(session.baseUrl, session.password, customer)
        if (result.ok) {
            val saved = result.value
            if (saved != null) {
                dao.upsert(saved.toDomain(SyncStatus.SYNCED).toEntity())
                return PushOutcome.SYNCED
            }
        }
        SyncScheduler.schedule(context)
        return if (result.code == 401) PushOutcome.UNAUTHORIZED else PushOutcome.PENDING
    }

    private suspend fun pushDelete(id: String): PushOutcome {
        val result = api.delete(session.baseUrl, session.password, id)
        if (result.ok) {
            dao.deleteById(id)
            return PushOutcome.SYNCED
        }
        if (result.code == 404) {
            // Already gone on the server — treat as success.
            dao.deleteById(id)
            return PushOutcome.SYNCED
        }
        SyncScheduler.schedule(context)
        return if (result.code == 401) PushOutcome.UNAUTHORIZED else PushOutcome.PENDING
    }
}
