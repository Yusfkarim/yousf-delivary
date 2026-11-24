package com.yousif.delivery.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yousif.delivery.AppGraph
import com.yousif.delivery.BuildConfig
import com.yousif.delivery.data.Customer
import com.yousif.delivery.data.CustomerDraft
import com.yousif.delivery.data.GoodEntity
import com.yousif.delivery.data.GoodItem
import com.yousif.delivery.data.ImportReport
import com.yousif.delivery.data.toDomain
import com.yousif.delivery.data.toEntity
import com.yousif.delivery.data.PushOutcome
import com.yousif.delivery.data.SyncResult
import com.yousif.delivery.data.SyncStatus
import com.yousif.delivery.data.remote.ApiClient
import com.yousif.delivery.location.LocationProvider
import com.yousif.delivery.util.Diagnostics
import com.yousif.delivery.util.GeoUtils
import com.yousif.delivery.util.PhoneUtils
import com.yousif.delivery.util.TextUtils as TextUtilsNumber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID

data class LocationFix(val latitude: Double, val longitude: Double)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val session = AppGraph.session(application)
    private val repository = AppGraph.repository(application)
    private val api = ApiClient()
    private val locationProvider = LocationProvider(application)
    private val goodDao = AppGraph.goods(application)
    private var stopLocation: (() -> Unit)? = null

    private val _loggedIn = MutableStateFlow(session.loggedIn)
    val loggedIn: StateFlow<Boolean> = _loggedIn.asStateFlow()

    private val _baseUrl = MutableStateFlow(session.baseUrl)
    val baseUrl: StateFlow<String> = _baseUrl.asStateFlow()

    private val _themeMode = MutableStateFlow(session.themeMode)
    val themeMode: StateFlow<Int> = _themeMode.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _nearestFirst = MutableStateFlow(true)
    val nearestFirst: StateFlow<Boolean> = _nearestFirst.asStateFlow()

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _myLocation = MutableStateFlow<LocationFix?>(null)
    val myLocation: StateFlow<LocationFix?> = _myLocation.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _diagnostics = MutableStateFlow<List<String>?>(null)
    val diagnostics: StateFlow<List<String>?> = _diagnostics.asStateFlow()

    private val allRecords: StateFlow<List<Customer>> = repository.customers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** People only — the shops live in their own list below. */
    val customers: StateFlow<List<Customer>> = allRecords
        .map { list -> list.filter { customer -> !customer.isShop } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Shops use exactly the same fields as customers, only a different badge. */
    val shops: StateFlow<List<Customer>> = allRecords
        .map { list -> list.filter { customer -> customer.isShop } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** «کاڵای کڕیار» — local only, newest first. */
    val goods: StateFlow<List<GoodItem>> = goodDao.observeAll()
        .map { rows -> rows.map(GoodEntity::toDomain) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pendingCount: StateFlow<Int> = allRecords
        .map { list -> list.count { it.syncStatus != SyncStatus.SYNCED } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val visibleCustomers: StateFlow<List<Customer>> =
        combine(allRecords, _query, _nearestFirst, _myLocation) { list, query, nearest, fix ->
            arrange(list.filter { customer -> !customer.isShop }, query, nearest, fix)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val visibleShops: StateFlow<List<Customer>> =
        combine(allRecords, _query, _nearestFirst, _myLocation) { list, query, nearest, fix ->
            arrange(list.filter { customer -> customer.isShop }, query, nearest, fix)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Search + "nearest first" ordering, shared by both lists. */
    private fun arrange(
        list: List<Customer>,
        query: String,
        nearest: Boolean,
        fix: LocationFix?,
    ): List<Customer> {
        val needle = query.trim()
        val digits = PhoneUtils.digits(needle)
        val filtered = if (needle.isEmpty()) {
            list
        } else {
            list.filter { customer ->
                customer.name.contains(needle, ignoreCase = true) ||
                    customer.addressNote.contains(needle, ignoreCase = true) ||
                    customer.allPhones.any { phone ->
                        phone.contains(needle) ||
                            (digits.isNotEmpty() && PhoneUtils.normalize(phone).contains(digits))
                    }
            }
        }

        return if (nearest && fix != null) {
            filtered.sortedBy { customer ->
                if (customer.hasCoordinates) {
                    GeoUtils.distanceMeters(
                        fix.latitude,
                        fix.longitude,
                        customer.latitude,
                        customer.longitude,
                    )
                } else {
                    Double.MAX_VALUE
                }
            }
        } else {
            filtered.sortedByDescending { it.updatedAt }
        }
    }

    fun setThemeMode(mode: Int) {
        _themeMode.value = mode
        session.themeMode = mode
    }

    fun setQuery(value: String) {
        _query.value = value
    }

    fun setNearestFirst(value: Boolean) {
        _nearestFirst.value = value
    }

    fun distanceTo(customer: Customer): Double? {
        val fix = _myLocation.value ?: return null
        if (!customer.hasCoordinates) return null
        return GeoUtils.distanceMeters(fix.latitude, fix.longitude, customer.latitude, customer.longitude)
    }

    // ── Auth ────────────────────────────────────────────────────────────

    fun unlock(password: String) {
        val candidate = password.trim()
        if (candidate.isEmpty()) {
            emit("وشەی نهێنی بنووسە")
            return
        }
        viewModelScope.launch {
            _busy.value = true
            _authError.value = null
            val result = api.verify(_baseUrl.value, candidate)
            when {
                result.ok && result.value == true -> enterApp(candidate)
                result.ok -> fail("وشەی نهێنی هەڵەیە")
                result.code == ApiClient.CODE_OFFLINE && session.password == candidate ->
                    enterApp(candidate, offline = true)
                matchesLocalPassword(candidate) -> enterApp(candidate, offline = true)
                else -> fail(explain(result.code))
            }
            _busy.value = false
        }
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun dismissDiagnostics() {
        _diagnostics.value = null
    }

    /** Runs the DNS / TCP / HTTP checks and shows the result on the lock screen. */
    fun diagnose() {
        viewModelScope.launch {
            _busy.value = true
            _diagnostics.value = withContext(Dispatchers.IO) { Diagnostics.run(_baseUrl.value) }
            _busy.value = false
        }
    }

    private fun matchesLocalPassword(candidate: String): Boolean {
        val digest = MessageDigest.getInstance("SHA-256").digest(candidate.toByteArray())
        val hex = digest.joinToString("") { byte -> "%02x".format(byte) }
        return hex.equals(BuildConfig.PASSWORD_HASH, ignoreCase = true)
    }

    private fun fail(message: String) {
        _authError.value = message
        emit(message)
    }

    private fun hasInternet(): Boolean {
        val manager = getApplication<Application>()
            .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val network = manager.activeNetwork ?: return false
        val caps = manager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /** Turns an API failure code into an honest Kurdish message. */
    private fun explain(code: Int): String = when {
        code == ApiClient.CODE_PROTECTED ->
            "سێرڤەر بە Vercel SSO پارێزراوە — لە Vercel «Deployment Protection» بکوژێنەرەوە"
        code == ApiClient.CODE_OFFLINE && hasInternet() ->
            "سێرڤەر نەگەیشت — ناونیشانەکە یان ئینتەرنێت بپشکنە (VPN)"
        code == ApiClient.CODE_OFFLINE -> "پەیوەندی بە ئینتەرنێتەوە نییە"
        code == 401 -> "وشەی نهێنی هەڵەیە"
        else -> "هەڵەی سێرڤەر: " + code
    }

    private fun enterApp(password: String, offline: Boolean = false) {
        session.password = password
        session.loggedIn = true
        _loggedIn.value = true
        emit(
            if (offline) {
                "دۆخی ئۆفلاین — سێرڤەر نەگەیشت، داتا لەسەر مۆبایل هەڵدەگیرێت"
            } else {
                "بەخێربێیت بۆ یوسف دلیڤەری"
            },
        )
        viewModelScope.launch { runSync(announce = false) }
    }

    fun lockApp() {
        session.lock()
        _loggedIn.value = false
        stopLocationUpdates()
    }

    // ── CRUD ────────────────────────────────────────────────────────────

    fun saveCustomer(draft: CustomerDraft, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = draft.id?.let { id -> allRecords.value.firstOrNull { it.id == id } }
            val customer = Customer(
                id = draft.id ?: UUID.randomUUID().toString(),
                kind = com.yousif.delivery.data.Kinds.normalize(draft.kind),
                name = draft.name.trim(),
                primaryPhone = draft.primaryPhone.trim(),
                hasWhatsapp = draft.hasWhatsapp,
                hasViber = draft.hasViber,
                extraPhones = draft.extraPhones.filter { it.number.isNotBlank() },
                latitude = draft.latitude ?: existing?.latitude ?: 0.0,
                longitude = draft.longitude ?: existing?.longitude ?: 0.0,
                addressNote = draft.addressNote.trim(),
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING_UPSERT,
            )
            when (repository.save(customer)) {
                PushOutcome.SYNCED -> emit("لە سێرڤەر پاشەکەوتکرا ✓")
                PushOutcome.PENDING -> emit("چاوەڕوانی ئینتەرنێتە بۆ خەزنکردن")
                PushOutcome.UNAUTHORIZED -> emit("وشەی نهێنی ڕەتکرایەوە — دووبارە بچۆ ژوورەوە")
            }
            onDone()
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            goodDao.detachCustomer(customer.id)
            when (repository.delete(customer)) {
                PushOutcome.SYNCED -> emit("سڕایەوە")
                PushOutcome.PENDING -> emit("لە سێرڤەر دەسڕدرێت کاتێک ئینتەرنێت بگەڕێتەوە")
                PushOutcome.UNAUTHORIZED -> emit("وشەی نهێنی ڕەتکرایەوە")
            }
        }
    }

    // ── Goods (local only) ───────────────────────────────────────────────

    fun saveGood(item: GoodItem, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            if (item.title.isBlank()) return@launch
            val now = System.currentTimeMillis()
            val entity = item.copy(
                title = item.title.trim(),
                quantity = item.quantity.coerceAtLeast(1),
                updatedAt = now,
                createdAt = if (item.createdAt > 0) item.createdAt else now,
            ).toEntity()
            goodDao.upsert(entity)
            onDone()
        }
    }

    /** Saves a whole order sheet — several rows at once. */
    fun saveGoods(items: List<GoodItem>, onDone: () -> Unit = {}) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val entities = items
                .filter { it.title.isNotBlank() }
                .map { item ->
                    item.copy(
                        title = item.title.trim(),
                        quantity = item.quantity.coerceAtLeast(1),
                        updatedAt = now,
                        createdAt = if (item.createdAt > 0) item.createdAt else now,
                    ).toEntity()
                }
            if (entities.isNotEmpty()) {
                withContext(Dispatchers.IO) { goodDao.upsertAll(entities) }
            }
            onDone()
        }
    }

    fun deleteGood(item: GoodItem) {
        viewModelScope.launch { goodDao.delete(item.id) }
    }

    fun bumpGood(item: GoodItem, delta: Int) {
        viewModelScope.launch {
            val next = (item.quantity + delta).coerceIn(0, 9999)
            goodDao.setQuantity(item.id, next, System.currentTimeMillis())
        }
    }

    // ── Backup (export / import) ─────────────────────────────────────────

    /** Serialises every customer and shop to a pretty-printed JSON string. */
    suspend fun backupJson(): String = withContext(Dispatchers.IO) {
        val goodsSnapshot = goodDao.allOnce().map(GoodEntity::toDomain)
        com.yousif.delivery.util.BackupCodec.encode(repository.allOnce(), goodsSnapshot)
    }

    fun backupFileName(): String = com.yousif.delivery.util.BackupCodec.fileName()

    /**
     * Restores a backup file, then pushes everything to the server.
     *
     * Importing the very same file twice never duplicates anything: a record that
     * already exists under the same id is replaced, and two copies of the same
     * person (same name + phone) are folded into one and the leftover id is
     * deleted on the server too. The callback carries the exact numbers so the
     * driver can see what happened.
     */
    fun importBackup(json: String, onDone: (ImportReport) -> Unit = {}) {
        viewModelScope.launch {
            val records = withContext(Dispatchers.IO) {
                com.yousif.delivery.util.BackupCodec.decodeToCustomers(json)
            }
            if (records.isEmpty()) {
                emit("فایلەکە نەخوێندرایەوە یان هیچ تۆمارێکی تێدا نییە")
                onDone(ImportReport())
                return@launch
            }
            val goodsRows = withContext(Dispatchers.IO) {
                com.yousif.delivery.util.BackupCodec.decodeGoods(json).map { it.toEntity() }
            }
            if (goodsRows.isNotEmpty()) {
                withContext(Dispatchers.IO) { goodDao.upsertAll(goodsRows) }
            }
            _busy.value = true
            val report = repository.importRecords(records).copy(goods = goodsRows.size)
            _busy.value = false
            emit(importMessage(report))
            onDone(report)
            runSync(announce = false)
        }
    }

    /** Folds duplicate copies (same name + phone) into one, locally and on the server. */
    fun dedupeRecords(onDone: (Int) -> Unit = {}) {
        viewModelScope.launch {
            _busy.value = true
            val merged = withContext(Dispatchers.IO) { repository.mergeDuplicates() }
            _busy.value = false
            emit(
                if (merged > 0) {
                    "${TextUtilsNumber.toKurdish(merged.toString())} دووبارە یەکخرا و لە سێرڤەریش پاککرایەوە ✓"
                } else {
                    "هیچ تۆمارێکی دووبارە نەدۆزرایەوە ✓"
                },
            )
            onDone(merged)
            if (merged > 0) runSync(announce = false)
        }
    }

    private fun importMessage(report: ImportReport): String {
        val parts = mutableListOf<String>()
        if (report.added > 0) parts += "نوێ ${TextUtilsNumber.toKurdish(report.added.toString())}"
        if (report.updated > 0) parts += "نوێکرایەوە ${TextUtilsNumber.toKurdish(report.updated.toString())}"
        if (report.merged > 0) parts += "دووبارە یەکخرا ${TextUtilsNumber.toKurdish(report.merged.toString())}"
        if (report.goods > 0) parts += "کاڵا ${TextUtilsNumber.toKurdish(report.goods.toString())}"
        return if (parts.isEmpty()) {
            "هیچ گۆڕانکارییەک پێویست نەبوو — داتابەیس پێشتر نوێ بوو ✓"
        } else {
            "هاوردەکردن تەواو بوو · ${parts.joinToString(" · ")} ✓"
        }
    }

    // ── Shared / pasted Google Maps links ────────────────────────────────

    /** Resolves a share-link into coordinates off the main thread. */
    fun resolveMapLink(text: String, onResolved: (Pair<Double, Double>?) -> Unit) {
        viewModelScope.launch {
            val result = com.yousif.delivery.util.MapLinkResolver.resolve(text)
            onResolved(result)
        }
    }

    // ── Sync ────────────────────────────────────────────────────────────

    fun refresh() {
        viewModelScope.launch { runSync(announce = true) }
    }

    private suspend fun runSync(announce: Boolean) {
        _syncing.value = true
        val result = repository.syncDetailed()
        _syncing.value = false
        if (announce) {
            emit(if (result.ok) "هەموو داتاکان لەگەڵ سێرڤەر یەکسانن ✓" else explainSync(result))
        }
    }

    /** Precise reason for a failed sync — never a single catch-all message. */
    private fun explainSync(result: SyncResult): String = when {
        result.code == ApiClient.CODE_PROTECTED ->
            "سێرڤەر بە Vercel SSO پارێزراوە — Deployment Protection بکوژێنەرەوە"
        result.code == ApiClient.CODE_OFFLINE && !hasInternet() -> "پەیوەندی بە ئینتەرنێتەوە نییە"
        result.code == ApiClient.CODE_OFFLINE ->
            "وەڵام نەهاتووە (کات تێپەڕی) — «پشکنینی سێرڤەر» دابگرە"
        result.code == 200 -> "هەندێک گۆڕانکاری نەنێردرا — دووبارە هەوڵ بدە"
        else -> explain(result.code)
    }

    // ── Server settings ─────────────────────────────────────────────────

    fun updateBaseUrl(url: String) {
        val trimmed = url.trim().trimEnd('/')
        if (trimmed.isEmpty()) {
            emit("نیشانی سێرڤەر بەتاڵە")
            return
        }
        session.baseUrl = trimmed
        _baseUrl.value = session.baseUrl
        emit("نیشانی سێرڤەر پاشەکەوتکرا")
    }

    fun testConnection() {
        viewModelScope.launch {
            _busy.value = true
            val result = api.verify(_baseUrl.value, session.password.ifEmpty { "12345678rk" })
            when {
                result.ok && result.value == true -> emit("پەیوەندی سەرکەوتوو ✓")
                result.ok -> emit("سێرڤەر وەڵام دەداتەوە، بەڵام وشەی نهێنی هەڵەیە")
                else -> emit(explain(result.code))
            }
            _busy.value = false
        }
    }

    // ── Location ────────────────────────────────────────────────────────

    fun onLocationPermissionGranted() {
        startLocationUpdates()
    }

    fun startLocationUpdates() {
        if (stopLocation != null) return
        locationProvider.lastKnown()?.let { (lat, lng) -> _myLocation.value = LocationFix(lat, lng) }
        stopLocation = locationProvider.start { lat, lng ->
            _myLocation.value = LocationFix(lat, lng)
        }
    }

    fun stopLocationUpdates() {
        stopLocation?.invoke()
        stopLocation = null
    }

    /** One-shot fix for the location picker. */
    fun requestGpsFix(onFix: (Double, Double) -> Unit) {
        val cached = locationProvider.lastKnown()
        if (cached != null) {
            _myLocation.value = LocationFix(cached.first, cached.second)
            onFix(cached.first, cached.second)
            return
        }
        var stop: (() -> Unit)? = null
        var delivered = false
        stop = locationProvider.start { lat, lng ->
            _myLocation.value = LocationFix(lat, lng)
            if (!delivered) {
                delivered = true
                onFix(lat, lng)
                stop?.invoke()
            }
        }
    }

    private fun emit(message: String) {
        _messages.tryEmit(message)
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
    }
}
