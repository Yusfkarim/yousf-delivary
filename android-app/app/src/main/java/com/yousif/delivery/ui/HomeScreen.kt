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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yousif.delivery.data.Customer
import com.yousif.delivery.ui.icons.AppIcons
import com.yousif.delivery.ui.theme.LocalAppColors
import com.yousif.delivery.util.TextUtils
import com.yousif.delivery.util.hasLocationPermission
import com.yousif.delivery.viewmodel.MainViewModel

private enum class HomeTab { CUSTOMERS, MAP }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val colors = LocalAppColors.current
    val context = LocalContext.current

    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val visible by viewModel.visibleCustomers.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val nearestFirst by viewModel.nearestFirst.collectAsStateWithLifecycle()
    val syncing by viewModel.syncing.collectAsStateWithLifecycle()
    val pending by viewModel.pendingCount.collectAsStateWithLifecycle()
    val myLocation by viewModel.myLocation.collectAsStateWithLifecycle()
    val baseUrl by viewModel.baseUrl.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    var tab by remember { mutableStateOf(HomeTab.CUSTOMERS) }
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
        containerColor = colors.background,
        topBar = {
            HomeHeader(
                pending = pending,
                syncing = syncing,
                gpsActive = myLocation != null,
                onSync = { viewModel.refresh() },
                onBell = { showSettings = true },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (tab) {
                HomeTab.CUSTOMERS -> Column(Modifier.fillMaxSize()) {
                    SearchField(
                        query = query,
                        onQueryChange = viewModel::setQuery,
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Segmented(
                            options = listOf("نزیکترین", "نوێترین"),
                            selectedIndex = if (nearestFirst) 0 else 1,
                            modifier = Modifier.weight(1f),
                            onSelect = { index -> viewModel.setNearestFirst(index == 0) },
                        )
                        Spacer(Modifier.width(10.dp))
                        StatPill(
                            text = TextUtils.toKurdish(customers.size.toString()) + " کڕیار",
                            icon = AppIcons.user(),
                            tint = colors.onBrand,
                            soft = colors.brand,
                        )
                    }

                    SectionHeader(
                        text = "کڕیارەکان",
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )

                    PullToRefreshBox(
                        isRefreshing = syncing,
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 118.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            if (visible.isEmpty()) {
                                item {
                                    EmptyState(
                                        title = if (customers.isEmpty()) {
                                            "هیچ کڕیارێک نییە لە ئێستادا"
                                        } else {
                                            "هیچ ئەنجامێک نەدۆزرایەوە"
                                        },
                                        hint = if (customers.isEmpty()) {
                                            "بە دوگمەی «نوێ» کڕیارێک زیاد بکە — ناو، ژمارە و شوێن."
                                        } else {
                                            "گەڕانەکەت بگۆڕە یان پاکی بکەرەوە بۆ بینینی هەموو کڕیارەکان."
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

                HomeTab.MAP -> MapScreen(
                    customers = visible,
                    myLocation = myLocation,
                    distanceOf = { customer -> viewModel.distanceTo(customer) },
                    onOpenRoute = { customer -> routeTarget = customer },
                    onLocate = {
                        requestGps { _, _ -> viewModel.onLocationPermissionGranted() }
                    },
                )
            }

            HomeBottomBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                tab = tab,
                onTab = { tab = it },
                onAdd = {
                    editing = null
                    showSheet = true
                },
            )
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
            themeMode = themeMode,
            onThemeMode = { viewModel.setThemeMode(it) },
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
            containerColor = colors.surface,
            title = { Text("سڕینەوەی کڕیار", color = colors.textPrimary) },
            text = {
                Text(
                    "دڵنیایی لە سڕینەوەی «${customer.name}»؟ ئەم کارە لە سێرڤەریش دەسڕدرێتەوە.",
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCustomer(customer)
                    deleteTarget = null
                }) { Text("سڕینەوە", color = colors.danger) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("پاشگەزبوونەوە") }
            },
        )
    }
}

/* ------------------------------------------------------------------ header */

@Composable
private fun HomeHeader(
    pending: Int,
    syncing: Boolean,
    gpsActive: Boolean,
    onSync: () -> Unit,
    onBell: () -> Unit,
) {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SoftIconButton(icon = AppIcons.bell(), onClick = onBell, contentDescription = "ڕێکخستن")
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BrandBadge(size = 34.dp)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "یوسف دلیڤەری",
                    color = colors.brand,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
            }
            Spacer(Modifier.weight(1f))
            Box {
                SoftIconButton(
                    icon = AppIcons.sync(),
                    onClick = onSync,
                    contentDescription = "هاوکاتکردن",
                    tint = colors.brand,
                )
                if (pending > 0 && !syncing) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(17.dp)
                            .clip(RoundedCornerShape(50))
                            .background(colors.brand)
                            .border(2.dp, colors.background, RoundedCornerShape(50)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = TextUtils.toKurdish(pending.toString()),
                            color = colors.onBrand,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                if (syncing) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(17.dp)
                            .clip(RoundedCornerShape(50))
                            .background(colors.surface)
                            .border(2.dp, colors.background, RoundedCornerShape(50)),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(10.dp),
                            strokeWidth = 1.5.dp,
                            color = colors.brand,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(colors.surface)
                    .border(1.dp, colors.brand.copy(alpha = 0.35f), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GpsDot(active = gpsActive)
                Spacer(Modifier.width(7.dp))
                Text(
                    text = if (gpsActive) "GPS چالاکە" else "چاوەڕوانی GPS",
                    color = colors.textSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(colors.surface)
                    .border(1.dp, colors.brand.copy(alpha = 0.35f), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    AppIcons.clock(),
                    contentDescription = null,
                    tint = colors.brandDeep,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "ئەمڕۆ",
                    color = colors.brandDeep,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ search */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val colors = LocalAppColors.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        singleLine = true,
        placeholder = {
            Text("گەڕان: ناو، ژمارە، ناونیشان", fontSize = 13.sp, color = colors.textMuted)
        },
        leadingIcon = {
            Icon(
                AppIcons.search(),
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(18.dp),
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        AppIcons.close(),
                        contentDescription = "سڕینەوەی گەڕان",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        },
        shape = RoundedCornerShape(18.dp),
        textStyle = TextStyle(fontSize = 13.5.sp, color = colors.textPrimary),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = colors.surface,
            unfocusedContainerColor = colors.surface,
            focusedBorderColor = colors.brand.copy(alpha = 0.55f),
            unfocusedBorderColor = colors.border,
            cursorColor = colors.brand,
        ),
    )
}

/* --------------------------------------------------------------- bottom bar */

@Composable
private fun HomeBottomBar(
    tab: HomeTab,
    modifier: Modifier = Modifier,
    onTab: (HomeTab) -> Unit,
    onAdd: () -> Unit,
) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(14.dp, shape, clip = false, ambientColor = Color(0x22A0785A), spotColor = Color(0x22A0785A))
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.border, shape)
            .padding(start = 8.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PillButton(
            label = "نوێ",
            onClick = onAdd,
            icon = AppIcons.plus(),
            container = colors.brand,
            contentColor = colors.onBrand,
            modifier = Modifier.width(104.dp),
            height = 46.dp,
            fontSize = 13,
        )
        Spacer(Modifier.width(6.dp))
        NavItem(
            icon = AppIcons.list(),
            label = "کڕیارەکان",
            selected = tab == HomeTab.CUSTOMERS,
            modifier = Modifier.weight(1f),
        ) { onTab(HomeTab.CUSTOMERS) }
        NavItem(
            icon = AppIcons.map(),
            label = "نەخشە",
            selected = tab == HomeTab.MAP,
            modifier = Modifier.weight(1f),
        ) { onTab(HomeTab.MAP) }
    }
}

@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(50))
                .background(if (selected) colors.brandSoft else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (selected) colors.brand else colors.textMuted,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = if (selected) colors.brandDeep else colors.textMuted,
            fontSize = 9.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}
