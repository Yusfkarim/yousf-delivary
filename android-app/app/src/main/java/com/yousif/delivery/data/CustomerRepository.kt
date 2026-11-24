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
import java.util.Locale

/**
 * Result of a full sync. Carries the HTTP code so the UI can explain *why* a
 * sync failed instead of showing one generic message.
 */
data class SyncResult(
    val ok: Boolean,
    val code: Int = 200,
    val message: String? = null,
)

/** What an import actually did — the driver is told exactly what happened. */
data class ImportReport(
    /** Brand-new records written to the phone. */
    val added: Int = 0,
    /** Records that already existed and were replaced by the newer copy. */
    val updated: Int = 0,
    /** Copies that were older than what the phone already had. */
    val skipped: Int = 0,
    /** Duplicate copies folded into one (and removed from the server too). */
    val merged: Int = 0,
    /** Goods rows carried by the same file. */
    val goods: Int = 0,
) {
    val written: Int get() = added + updated
    val untouched: Boolean get() = added == 0 && updated == 0 && merged == 0 && goods == 0
}

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
     * Merges a backup file into Room, then folds every duplicate away.
     *
     * A record that already existed under the same id is **replaced** (never
     * copied twice), and two records that describe the same person under
     * different ids are merged into one. The merged-away ids are deleted on the
     * server as well, so a second import of the same file cannot leave the
     * database with two of everything.
     */
    suspend fun importRecords(records: List<Customer>): ImportReport = withContext(Dispatchers.IO) {
        if (records.isEmpty()) return@withContext ImportReport()
        val local = dao.allOnce()
            .filter { it.syncStatus != SyncStatus.PENDING_DELETE.name }
            .associateBy { it.id }

        var added = 0
        var updated = 0
        var skipped = 0
        val toWrite = mutableListOf<CustomerEntity>()

        records.forEach { incoming ->
            val existing = local[incoming.id]
            when {
                existing == null -> {
                    added += 1
                    toWrite += incoming.copy(syncStatus = SyncStatus.PENDING_UPSERT).toEntity()
                }

                incoming.updatedAt >= existing.updatedAt -> {
                    updated += 1
                    toWrite += incoming.copy(syncStatus = SyncStatus.PENDING_UPSERT).toEntity()
                }

                else -> skipped += 1
            }
        }

        if (toWrite.isNotEmpty()) {
            dao.upsertAll(toWrite)
            pushPending()
        }

        ImportReport(added = added, updated = updated, skipped = skipped, merged = mergeDuplicates())
    }

    /**
     * Finds records that describe the same person (same name and same phone) and
     * keeps the richest copy — extra phones, the location and the note of the
     * others are folded in, and the leftover copies are deleted everywhere.
     */
    suspend fun mergeDuplicates(): Int = withContext(Dispatchers.IO) {
        val all = dao.allOnce()
            .filter { it.syncStatus != SyncStatus.PENDING_DELETE.name }
            .map { it.toDomain() }
        if (all.size < 2) return@withContext 0

        val groups = LinkedHashMap<String, MutableList<Customer>>()
        all.forEach { customer ->
            groups.getOrPut(duplicateKey(customer)) { mutableListOf() }.add(customer)
        }

        var merged = 0
        val survivors = mutableListOf<Customer>()
        val duplicates = mutableListOf<Customer>()
        groups.values.forEach { group ->
            if (group.size < 2) return@forEach
            val ordered = group.sortedWith(
                compareByDescending<Customer> { richness(it) }.thenBy { it.createdAt },
            )
            var survivor = ordered.first()
            ordered.drop(1).forEach { other ->
                survivor = fold(survivor, other)
                duplicates += other
                merged += 1
            }
            survivors += survivor
        }

        if (merged == 0) return@withContext 0

        val now = System.currentTimeMillis()
        dao.upsertAll(
            survivors.map {
                it.copy(updatedAt = now, syncStatus = SyncStatus.PENDING_UPSERT).toEntity()
            },
        )
        duplicates.forEach {
            dao.upsert(it.copy(syncStatus = SyncStatus.PENDING_DELETE).toEntity())
        }
        pushPending()
        merged
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
        var failures = 0
        for (entity in dao.pending()) {
            val status = runCatching { SyncStatus.valueOf(entity.syncStatus) }
                .getOrDefault(SyncStatus.SYNCED)
            val outcome = when (status) {
                SyncStatus.PENDING_DELETE -> pushDelete(entity.id)
                else -> pushSingle(entity.toDomain())
            }
            if (outcome != PushOutcome.SYNCED) {
                allSynced = false
                failures += 1
                // No network: stop after two tries instead of waiting a timeout per
                // record — the rows stay pending and WorkManager retries later.
                if (failures >= 2) break
            }
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

    /** Same name + same phone ⇒ the same person, imported twice. */
    private fun duplicateKey(customer: Customer): String {
        val name = customer.name.trim()
            .lowercase(Locale.ROOT)
            .replace(multipleSpaces, " ")
        val phone = customer.allPhones
            .map { number -> number.filter(Char::isDigit) }
            .firstOrNull { digits -> digits.length >= 7 }
            ?.takeLast(9)
            .orEmpty()
        return "$name|$phone"
    }

    private fun richness(customer: Customer): Int =
        (if (customer.hasCoordinates) 4 else 0) +
            (if (customer.primaryPhone.isNotBlank()) 2 else 0) +
            (if (customer.addressNote.isNotBlank()) 1 else 0) +
            customer.extraPhones.size +
            (if (customer.name.length > 4) 1 else 0)

    private fun fold(primary: Customer, other: Customer): Customer = primary.copy(
        primaryPhone = primary.primaryPhone.ifBlank { other.primaryPhone },
        hasWhatsapp = primary.hasWhatsapp || other.hasWhatsapp,
        hasViber = primary.hasViber || other.hasViber,
        extraPhones = (primary.extraPhones + other.extraPhones)
            .filter { it.number.isNotBlank() }
            .distinctBy { it.number.filter(Char::isDigit).takeLast(9) },
        latitude = if (primary.hasCoordinates) primary.latitude else other.latitude,
        longitude = if (primary.hasCoordinates) primary.longitude else other.longitude,
        addressNote = primary.addressNote.ifBlank { other.addressNote },
        createdAt = minOf(primary.createdAt, other.createdAt),
        updatedAt = maxOf(primary.updatedAt, other.updatedAt),
        syncStatus = SyncStatus.PENDING_UPSERT,
    )
}

private val multipleSpaces = Regex("\\s+")
