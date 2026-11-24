package com.yousif.delivery.ui

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yousif.delivery.data.Customer
import com.yousif.delivery.ui.icons.AppIcons
import com.yousif.delivery.ui.theme.AppBackground
import com.yousif.delivery.ui.theme.AppDanger
import com.yousif.delivery.ui.theme.AppSurface
import com.yousif.delivery.ui.theme.AppSurfaceHigh
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
                contentColor = Color(0xFF04121B),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.size(60.dp),
            ) {
                Icon(
                    AppIcons.plus(),
                    contentDescription = "زیادکردنی کڕیار",
                    modifier = Modifier.size(26.dp),
                )
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
                placeholder = {
                    Text("گەڕان: ناو، ژمارە، ناونیشان", fontSize = 13.sp, color = AppTextSecondary.copy(alpha = 0.7f))
                },
                leadingIcon = {
                    Icon(
                        AppIcons.search(),
                        contentDescription = null,
                        tint = AppTextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setQuery("") }) {
                            Icon(
                                AppIcons.close(),
                                contentDescription = "سڕینەوەی گەڕان",
                                tint = AppTextSecondary,
                                modifier = Modifier.size(17.dp),
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(18.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.5.sp, color = AppTextPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = AppSurface,
                    unfocusedContainerColor = AppSurface,
                    focusedBorderColor = NeonCyan.copy(alpha = 0.45f),
                    unfocusedBorderColor = Color(0x1F94A3B8),
                    cursorColor = NeonCyan,
                ),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SortPill(
                    label = "نزیکترین (GPS)",
                    icon = AppIcons.locate(),
                    selected = nearestFirst,
                    onClick = { viewModel.setNearestFirst(true) },
                    modifier = Modifier.weight(1f),
                )
                SortPill(
                    label = "نوێترین",
                    icon = AppIcons.sync(),
                    selected = !nearestFirst,
                    onClick = { viewModel.setNearestFirst(false) },
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = TextUtils.toKurdish(customers.size.toString()),
                    color = AppTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            PullToRefreshBox(
                isRefreshing = syncing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (visible.isEmpty()) {
                        item {
                            EmptyState(
                                title = if (customers.isEmpty()) "هێشتا کڕیارێک نییە" else "هیچ ئەنجامێک نەدۆزرایەوە",
                                hint = if (customers.isEmpty()) {
                                    "بە دوگمەی + کڕیارێکی نوێ زیاد بکە — ناو، ژمارە و شوێن."
                                } else {
                                    "گەڕانەکەت بگۆڕە یان بیکەرەوە بۆ بینینی هەموو کڕیارەکان."
                                },
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
            containerColor = AppSurface,
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
                }) { Text("سڕینەوە", color = AppDanger) }
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
            .padding(start = 18.dp, end = 12.dp, top = 18.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(
                    Brush.linearGradient(
                        listOf(NeonCyan, Color(0xFF0EA5E9)),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                AppIcons.pin(),
                contentDescription = null,
                tint = Color(0xFF04121B),
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = "یوسف دلیڤەری",
                color = AppTextPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.size(2.dp))
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

        Box {
            TopBarIconButton(onClick = onSync) {
                if (syncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = NeonCyan,
                    )
                } else {
                    Icon(
                        AppIcons.sync(),
                        contentDescription = "هاوکاتکردن لەگەڵ سێرڤەر",
                        tint = if (pending == 0) AppTextSecondary else MetallicGold,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            if (pending > 0 && !syncing) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(15.dp)
                        .clip(CircleShape)
                        .background(MetallicGold),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = TextUtils.toKurdish(pending.toString()),
                        color = Color(0xFF1A1206),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        TopBarIconButton(onClick = onSettings) {
            Icon(
                AppIcons.settings(),
                contentDescription = "ڕێکخستن",
                tint = AppTextSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun TopBarIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(shape)
            .background(AppSurfaceHigh.copy(alpha = 0.6f))
            .border(1.dp, Color(0x1494A3B8), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}
