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
import com.yousif.delivery.data.Kinds
import com.yousif.delivery.ui.icons.AppIcons
import com.yousif.delivery.ui.theme.LocalAppColors
import com.yousif.delivery.util.ShareInbox
import com.yousif.delivery.util.TextUtils
import com.yousif.delivery.util.hasLocationPermission
import com.yousif.delivery.viewmodel.MainViewModel

private enum class HomeTab { CUSTOMERS, SHOPS, GOODS, MAP }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val colors = LocalAppColors.current
    val context = LocalContext.current

    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val shops by viewModel.shops.collectAsStateWithLifecycle()
    val visibleCustomers by viewModel.visibleCustomers.collectAsStateWithLifecycle()
    val visibleShops by viewModel.visibleShops.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val nearestFirst by viewModel.nearestFirst.collectAsStateWithLifecycle()
    val syncing by viewModel.syncing.collectAsStateWithLifecycle()
    val pending by viewModel.pendingCount.collectAsStateWithLifecycle()
    val myLocation by viewModel.myLocation.collectAsStateWithLifecycle()
    val baseUrl by viewModel.baseUrl.collectAsStateWithLifecycle()
    val goods by viewModel.goods.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    var tab by remember { mutableStateOf(HomeTab.CUSTOMERS) }
    var showSheet by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Customer?>(null) }
    var routeTarget by remember { mutableStateOf<Customer?>(null) }
    var deleteTarget by remember { mutableStateOf<Customer?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showGoodSheet by remember { mutableStateOf(false) }
    var editingGood by remember { mutableStateOf<com.yousif.delivery.data.GoodItem?>(null) }
    var prefill by remember { mutableStateOf<MapPoint?>(null) }
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

    // A Google Maps link shared into the app opens a ready-to-save customer.
    val sharedLink by ShareInbox.link.collectAsStateWithLifecycle()
    LaunchedEffect(sharedLink) {
        val link = sharedLink ?: return@LaunchedEffect
        ShareInbox.clear()
        viewModel.resolveMapLink(link) { coordinates ->
            if (coordinates != null) {
                prefill = MapPoint(coordinates.first, coordinates.second)
                editing = null
                tab = HomeTab.CUSTOMERS
                showSheet = true
            }
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

    val everyone = remember(customers, shops) { customers + shops }

    val isShops = tab == HomeTab.SHOPS
    val activeList = if (isShops) shops else customers
    val activeVisible = if (isShops) visibleShops else visibleCustomers

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
                HomeTab.CUSTOMERS, HomeTab.SHOPS -> Column(Modifier.fillMaxSize()) {
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
                            text = TextUtils.toKurdish(activeList.size.toString()) +
                                if (isShops) " فرۆشگا" else " کڕیار",
                            icon = if (isShops) AppIcons.store() else AppIcons.user(),
                            tint = colors.onBrand,
                            soft = colors.brand,
                        )
                    }

                    SectionHeader(
                        text = if (isShops) "فرۆشگاکان" else "کڕیارەکان",
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )

                    PullToRefreshBox(
                        isRefreshing = syncing,
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 132.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            if (activeVisible.isEmpty()) {
                                item {
                                    EmptyState(
                                        title = when {
                                            activeList.isEmpty() && isShops -> "هیچ فرۆشگایەک نییە لە ئێستادا"
                                            activeList.isEmpty() -> "هیچ کڕیارێک نییە لە ئێستادا"
                                            else -> "هیچ ئەنجامێک نەدۆزرایەوە"
                                        },
                                        hint = when {
                                            activeList.isEmpty() && isShops ->
                                                "بە دوگمەی «نوێ» فرۆشگایەک زیاد بکە — ناو، ژمارە و شوێن."
                                            activeList.isEmpty() ->
                                                "بە دوگمەی «نوێ» کڕیارێک زیاد بکە — ناو، ژمارە و شوێن."
                                            else ->
                                                "گەڕانەکەت بگۆڕە یان پاکی بکەرەوە بۆ بینینی هەموو تۆمارەکان."
                                        },
                                    )
                                }
                            }
                            items(items = activeVisible, key = { it.id }) { customer ->
                                CustomerCard(
                                    customer = customer,
                                    distanceMeters = viewModel.distanceTo(customer),
                                    onOpenRoute = { routeTarget = customer },
                                    onEdit = {
                                        editing = customer
                                        prefill = null
                                        showSheet = true
                                    },
                                    onDelete = { deleteTarget = customer },
                                )
                            }
                        }
                    }
                }

                HomeTab.GOODS -> GoodsScreen(
                    goods = goods,
                    people = everyone,
                    onBump = { item, delta -> viewModel.bumpGood(item, delta) },
                    onEdit = { item ->
                        editingGood = item
                        showGoodSheet = true
                    },
                    onDelete = { item -> viewModel.deleteGood(item) },
                    onAdd = {
                        editingGood = null
                        showGoodSheet = true
                    },
                )

                HomeTab.MAP -> MapScreen(
                    customers = customers + shops,
                    myLocation = myLocation,
                    distanceOf = { customer -> viewModel.distanceTo(customer) },
                    onOpenRoute = { customer -> routeTarget = customer },
                    onOpenCustomer = { customer ->
                        editing = customer
                        prefill = null
                        showSheet = true
                    },
                    onLocate = {
                        requestGps { _, _ -> viewModel.onLocationPermissionGranted() }
                    },
                )
            }

            HomeBottomBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 14.dp, vertical = 16.dp),
                tab = tab,
                onTab = { tab = it },
                onAdd = {
                    if (tab == HomeTab.GOODS) {
                        editingGood = null
                        showGoodSheet = true
                    } else {
                        editing = null
                        prefill = null
                        showSheet = true
                    }
                },
            )
        }
    }

    if (showSheet) {
        CustomerSheet(
            initial = editing,
            kind = if (isShops) Kinds.SHOP else Kinds.CUSTOMER,
            prefill = prefill,
            myLocation = myLocation,
            onRequestGps = { onFix -> requestGps(onFix) },
            onDismiss = {
                showSheet = false
                prefill = null
            },
            onSave = { draft ->
                viewModel.saveCustomer(draft) {
                    showSheet = false
                    prefill = null
                }
            },
        )
    }

    if (showGoodSheet) {
        GoodSheet(
            initial = editingGood,
            customers = customers,
            onDismiss = {
                showGoodSheet = false
                editingGood = null
            },
            onSave = { items ->
                viewModel.saveGoods(items) {
                    showGoodSheet = false
                    editingGood = null
                }
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
            onExport = { viewModel.backupJson() },
            onExportName = { viewModel.backupFileName() },
            onImport = { json, done -> viewModel.importBackup(json, done) },
            onDedupe = { done -> viewModel.dedupeRecords(done) },
            customerCount = customers.size,
            shopCount = shops.size,
            goodCount = goods.size,
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
            title = {
                Text(
                    if (customer.isShop) "سڕینەوەی فرۆشگا" else "سڕینەوەی کڕیار",
                    color = colors.textPrimary,
                )
            },
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
                BrandBadge(size = 36.dp)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "یوسف دلیڤەری",
                    color = colors.brand,
                    fontSize = 12.sp,
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
                            .size(18.dp)
                            .clip(RoundedCornerShape(50))
                            .background(colors.brand)
                            .border(2.dp, colors.background, RoundedCornerShape(50)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = TextUtils.toKurdish(pending.toString()),
                            color = colors.onBrand,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                if (syncing) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(18.dp)
                            .clip(RoundedCornerShape(50))
                            .background(colors.surface)
                            .border(2.dp, colors.background, RoundedCornerShape(50)),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(11.dp),
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
                    fontSize = 12.sp,
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
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "ئەمڕۆ",
                    color = colors.brandDeep,
                    fontSize = 12.sp,
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
            Text("گەڕان: ناو، ژمارە، ناونیشان", fontSize = 13.5.sp, color = colors.textMuted)
        },
        leadingIcon = {
            Icon(
                AppIcons.search(),
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(20.dp),
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        AppIcons.close(),
                        contentDescription = "سڕینەوەی گەڕان",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        },
        shape = RoundedCornerShape(18.dp),
        textStyle = TextStyle(fontSize = 14.sp, color = colors.textPrimary),
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
            modifier = Modifier.width(74.dp),
            height = 48.dp,
            fontSize = 13,
        )
        Spacer(Modifier.width(4.dp))
        NavItem(
            icon = AppIcons.user(),
            label = "کڕیارەکان",
            selected = tab == HomeTab.CUSTOMERS,
            modifier = Modifier.weight(1f),
        ) { onTab(HomeTab.CUSTOMERS) }
        NavItem(
            icon = AppIcons.store(),
            label = "فرۆشگا",
            selected = tab == HomeTab.SHOPS,
            modifier = Modifier.weight(1f),
        ) { onTab(HomeTab.SHOPS) }
        NavItem(
            icon = AppIcons.packageBox(),
            label = "کاڵا",
            selected = tab == HomeTab.GOODS,
            modifier = Modifier.weight(1f),
        ) { onTab(HomeTab.GOODS) }
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
                .size(38.dp)
                .clip(RoundedCornerShape(50))
                .background(if (selected) colors.brandSoft else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (selected) colors.brand else colors.textMuted,
                modifier = Modifier.size(23.dp),
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            color = if (selected) colors.brandDeep else colors.textMuted,
            fontSize = 10.5.sp,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}
