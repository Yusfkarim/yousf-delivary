package com.yousif.delivery.data

import android.content.Context
import com.yousif.delivery.data.remote.ApiClient
import com.yousif.delivery.data.remote.CustomerDto
import com.yousif.delivery.data.remote.toDomain
import com.yousif.delivery.session.SessionStore
import com.yousif.delivery.sync.SyncScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Result of a full sync. Carries the HTTP code so the UI can explain *why* a
 * sync failed instead of showing one generic message.
 */
data class SyncResult(
    val ok: Boolean,
    val code: Int = 200,
    val message: String? = null,
)

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

    /** Every record (customers + shops) once, for the JSON backup. */
    suspend fun allOnce(): List<Customer> = withContext(Dispatchers.IO) {
        dao.allOnce().map { it.toDomain() }
    }

    /**
     * Merges a backup file into Room. The freshest copy wins, so importing an
     * old backup never overwrites something edited later on this phone.
     */
    suspend fun importRecords(records: List<Customer>): Int = withContext(Dispatchers.IO) {
        if (records.isEmpty()) return@withContext 0
        val local = dao.allOnce().associateBy { it.id }
        val toWrite = records.filter { incoming ->
            val existing = local[incoming.id]
            existing == null || incoming.updatedAt >= existing.updatedAt
        }
        if (toWrite.isNotEmpty()) {
            dao.upsertAll(toWrite.map { it.copy(syncStatus = SyncStatus.PENDING_UPSERT).toEntity() })
            pushPending()
        }
        toWrite.size
    }

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
    suspend fun syncAll(): Boolean = syncDetailed().ok

    /** Same as [syncAll] but reports the failure code/message for the UI. */
    suspend fun syncDetailed(): SyncResult = withContext(Dispatchers.IO) {
        val pushed = pushPending()
        val result = api.list(session.baseUrl, session.password)
        if (!result.ok) {
            return@withContext SyncResult(ok = false, code = result.code, message = result.message)
        }
        applyRemote(result.value.orEmpty())
        SyncResult(ok = pushed, code = 200, message = if (pushed) null else "pending push")
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
        applyRemote(result.value.orEmpty())
        true
    }

    /** Merges the cloud rows into Room without touching unsynced local rows. */
    private suspend fun applyRemote(remote: List<CustomerDto>) {
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
