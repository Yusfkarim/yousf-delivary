package com.yousif.delivery.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yousif.delivery.AppGraph
import com.yousif.delivery.data.Customer
import com.yousif.delivery.data.CustomerDraft
import com.yousif.delivery.data.PushOutcome
import com.yousif.delivery.data.SyncStatus
import com.yousif.delivery.data.remote.ApiClient
import com.yousif.delivery.location.LocationProvider
import com.yousif.delivery.util.GeoUtils
import com.yousif.delivery.util.PhoneUtils
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
import java.util.UUID

data class LocationFix(val latitude: Double, val longitude: Double)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val session = AppGraph.session(application)
    private val repository = AppGraph.repository(application)
    private val api = ApiClient()
    private val locationProvider = LocationProvider(application)
    private var stopLocation: (() -> Unit)? = null

    private val _loggedIn = MutableStateFlow(session.loggedIn)
    val loggedIn: StateFlow<Boolean> = _loggedIn.asStateFlow()

    private val _baseUrl = MutableStateFlow(session.baseUrl)
    val baseUrl: StateFlow<String> = _baseUrl.asStateFlow()

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

    val customers: StateFlow<List<Customer>> = repository.customers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pendingCount: StateFlow<Int> = customers
        .map { list -> list.count { it.syncStatus != SyncStatus.SYNCED } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val visibleCustomers: StateFlow<List<Customer>> =
        combine(customers, _query, _nearestFirst, _myLocation) { list, query, nearest, fix ->
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

            if (nearest && fix != null) {
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
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
            val result = api.verify(_baseUrl.value, candidate)
            if (result.ok) {
                if (result.value == true) {
                    enterApp(candidate)
                } else {
                    emit("وشەی نهێنی هەڵەیە")
                }
            } else {
                val offlineButKnown = result.code == 0 && session.password == candidate
                when {
                    offlineButKnown -> enterApp(candidate, offline = true)
                    result.code == 0 -> emit("پەیوەندی بە ئینتەرنێتەوە نییە")
                    else -> emit("وشەی نهێنی هەڵەیە")
                }
            }
            _busy.value = false
        }
    }

    private fun enterApp(password: String, offline: Boolean = false) {
        session.password = password
        session.loggedIn = true
        _loggedIn.value = true
        emit(if (offline) "دۆخی ئۆفلاین — داتای ناوخۆیی بەردەستە" else "بەخێربێیت بۆ یوسف دلیڤەری")
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
            val existing = draft.id?.let { id -> customers.value.firstOrNull { it.id == id } }
            val customer = Customer(
                id = draft.id ?: UUID.randomUUID().toString(),
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
            when (repository.delete(customer)) {
                PushOutcome.SYNCED -> emit("سڕایەوە")
                PushOutcome.PENDING -> emit("لە سێرڤەر دەسڕدرێت کاتێک ئینتەرنێت بگەڕێتەوە")
                PushOutcome.UNAUTHORIZED -> emit("وشەی نهێنی ڕەتکرایەوە")
            }
        }
    }

    // ── Sync ────────────────────────────────────────────────────────────

    fun refresh() {
        viewModelScope.launch { runSync(announce = true) }
    }

    private suspend fun runSync(announce: Boolean) {
        _syncing.value = true
        val ok = repository.syncAll()
        _syncing.value = false
        if (announce) {
            emit(
                if (ok) {
                    "هەموو داتاکان لەگەڵ سێرڤەر یەکسانن ✓"
                } else {
                    "هەندێک گۆڕانکاری چاوەڕوانی ئینتەرنێتن"
                },
            )
        }
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
            if (result.ok) {
                emit("پەیوەندی سەرکەوتوو ✓")
            } else {
                emit(
                    when (result.code) {
                        0 -> "ناتوانرێت پەیوەندی بکرێت"
                        401 -> "سێرڤەر وەڵام دەداتەوە (وشەی نهێنی پشکنین)"
                        else -> "هەڵا: ${result.code}"
                    },
                )
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
