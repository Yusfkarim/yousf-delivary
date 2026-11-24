package com.yousif.delivery.ui

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yousif.delivery.data.Customer
import com.yousif.delivery.ui.theme.AppBackground
import com.yousif.delivery.ui.theme.AppSuccess
import com.yousif.delivery.ui.theme.AppTextPrimary
import com.yousif.delivery.ui.theme.AppTextSecondary
import com.yousif.delivery.ui.theme.MetallicGold
import com.yousif.delivery.ui.theme.NeonCyan
import com.yousif.delivery.util.TextUtils
import com.yousif.delivery.util.hasLocationPermission
import com.yousif.delivery.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val context = LocalContext.current

    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val visible by viewModel.visibleCustomers.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val nearestFirst by viewModel.nearestFirst.collectAsStateWithLifecycle()
    val syncing by viewModel.syncing.collectAsStateWithLifecycle()
    val pending by viewModel.pendingCount.collectAsStateWithLifecycle()
    val myLocation by viewModel.myLocation.collectAsStateWithLifecycle()
    val baseUrl by viewModel.baseUrl.collectAsStateWithLifecycle()

    var showSheet by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Customer?>(null) }
    var routeTarget by remember { mutableStateOf<Customer?>(null) }
    var deleteTarget by remember { mutableStateOf<Customer?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var pendingGpsCallback by remember { mutableStateOf<((Double, Double) -> Unit)?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants.values.any { it }) {
            viewModel.onLocationPermissionGranted()
            pendingGpsCallback?.let { viewModel.requestGpsFix(it) }
        } else {
            Toast.makeText(context, "بۆ نیشاندانی دووری، مۆڵەتی شوێن پێویستە", Toast.LENGTH_SHORT).show()
        }
        pendingGpsCallback = null
    }

    LaunchedEffect(Unit) {
        if (hasLocationPermission(context)) {
            viewModel.onLocationPermissionGranted()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    fun requestGps(onFix: (Double, Double) -> Unit) {
        if (hasLocationPermission(context)) {
            viewModel.requestGpsFix(onFix)
        } else {
            pendingGpsCallback = onFix
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            HomeTopBar(
                pending = pending,
                syncing = syncing,
                gpsActive = myLocation != null,
                onSync = { viewModel.refresh() },
                onSettings = { showSettings = true },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editing = null
                    showSheet = true
                },
                containerColor = NeonCyan,
                contentColor = Color(0xFF04202A),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "زیادکردنی کڕیار")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                singleLine = true,
                placeholder = { Text("گەڕان: ناو، ژمارە، ناونیشان", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setQuery("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "سڕینەوەی گەڕان")
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = nearestFirst,
                    onClick = { viewModel.setNearestFirst(true) },
                    label = { Text("نزیکترین (GPS)", fontSize = 12.sp) },
                )
                FilterChip(
                    selected = !nearestFirst,
                    onClick = { viewModel.setNearestFirst(false) },
                    label = { Text("نوێترین", fontSize = 12.sp) },
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = TextUtils.toKurdish(customers.size.toString()),
                    color = AppTextSecondary,
                    fontSize = 12.sp,
                )
            }

            PullToRefreshBox(
                isRefreshing = syncing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (visible.isEmpty()) {
                        item {
                            EmptyState(
                                title = "هێشتا کڕیارێک نییە",
                                hint = "بە دوگمەی + کڕیارێکی نوێ زیاد بکە — ناو، ژمارە و شوێن.",
                            )
                        }
                    }
                    items(items = visible, key = { it.id }) { customer ->
                        CustomerCard(
                            customer = customer,
                            distanceMeters = viewModel.distanceTo(customer),
                            onOpenRoute = { routeTarget = customer },
                            onEdit = {
                                editing = customer
                                showSheet = true
                            },
                            onDelete = { deleteTarget = customer },
                        )
                    }
                }
            }
        }
    }

    if (showSheet) {
        CustomerSheet(
            initial = editing,
            myLocation = myLocation,
            onRequestGps = { onFix -> requestGps(onFix) },
            onDismiss = { showSheet = false },
            onSave = { draft ->
                viewModel.saveCustomer(draft) { showSheet = false }
            },
        )
    }

    routeTarget?.let { customer ->
        RouteDialog(
            customer = customer,
            myLocation = myLocation,
            onDismiss = { routeTarget = null },
        )
    }

    if (showSettings) {
        SettingsSheet(
            baseUrl = baseUrl,
            pendingCount = pending,
            onDismiss = { showSettings = false },
            onSaveUrl = { viewModel.updateBaseUrl(it) },
            onTest = { viewModel.testConnection() },
            onSync = { viewModel.refresh() },
            onLock = {
                showSettings = false
                viewModel.lockApp()
            },
        )
    }

    deleteTarget?.let { customer ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = com.yousif.delivery.ui.theme.AppSurface,
            title = { Text("سڕینەوەی کڕیار", color = AppTextPrimary) },
            text = {
                Text(
                    "دڵنیایی لە سڕینەوەی «${customer.name}»؟ ئەم کارە لە سێرڤەریش دەسڕدرێتەوە.",
                    color = AppTextSecondary,
                    fontSize = 13.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCustomer(customer)
                    deleteTarget = null
                }) { Text("سڕینەوە", color = com.yousif.delivery.ui.theme.AppDanger) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("پاشگەزبوونەوە") }
            },
        )
    }
}

@Composable
private fun HomeTopBar(
    pending: Int,
    syncing: Boolean,
    gpsActive: Boolean,
    onSync: () -> Unit,
    onSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "یوسف دلیڤەری",
                color = AppTextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                GpsDot(active = gpsActive)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (gpsActive) "GPS چالاکە" else "چاوەڕوانی GPS",
                    color = AppTextSecondary,
                    fontSize = 11.sp,
                )
            }
        }

        IconButton(onClick = onSync) {
            if (syncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = NeonCyan,
                )
            } else {
                Icon(
                    Icons.Filled.Sync,
                    contentDescription = "هاوکاتکردن لەگەڵ سێرڤەر",
                    tint = if (pending == 0) AppSuccess else MetallicGold,
                )
            }
        }
        IconButton(onClick = onSettings) {
            Icon(
                Icons.Filled.Settings,
                contentDescription = "ڕێکخستن",
                tint = AppTextSecondary,
            )
        }
    }
}
