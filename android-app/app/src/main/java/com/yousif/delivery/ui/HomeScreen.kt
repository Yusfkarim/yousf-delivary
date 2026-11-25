package com.yousif.delivery.ui

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yousif.delivery.data.Customer
import com.yousif.delivery.data.Kinds
import com.yousif.delivery.data.remote.ApiClient
import com.yousif.delivery.ui.icons.AppIcons
import com.yousif.delivery.ui.theme.LocalAppColors
import com.yousif.delivery.util.ShareInbox
import com.yousif.delivery.util.TextUtils
import com.yousif.delivery.util.hasLocationPermission
import com.yousif.delivery.viewmodel.MainViewModel

private enum class HomeTab { CUSTOMERS, SHOPS, GOODS, MAP }

/**
 * A calm, explicit notice instead of an empty list that looks like lost data:
 * the phone could not read the server, so «nothing here» is not the truth.
 */
@Composable
private fun ServerNotice(
    code: Int,
    onRetry: () -> Unit,
    onFixAddress: () -> Unit,
) {
    val colors = LocalAppColors.current
    val title = when (code) {
        404 -> "ناونیشانی سێرڤەر هەڵەیە (404)"
        ApiClient.CODE_PROTECTED -> "سێرڤەر بە Vercel SSO پارێزراوە"
        ApiClient.CODE_OFFLINE -> "سێرڤەر نەگەیشت — ئینتەرنێت بپشکنە"
        401 -> "وشەی نهێنی سێرڤەر ڕەتکرایەوە"
        else -> "داتای سێرڤەر هێشتا نەهاتووە ($code)"
    }
    val hint = if (code == 404) {
        "داتاکانت لەسەر سێرڤەر سەلامەتن. هەموو ڕێگاکان تاقیکرانەوە — ئەگەر VPN چالاکە " +
            "بیکوژێنەرەوە یان بگۆڕە، پاشان «هەوڵدانەوە» دابگرە."
    } else {
        "داتاکانت لەسەر سێرڤەر سەلامەتن — دوگمە بدە بۆ هەوڵدانەوە."
    }

    GlassCard(
        padding = 14.dp,
        radius = 22.dp,
        modifier = Modifier.padding(bottom = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.brandSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    AppIcons.cloudWait(),
                    contentDescription = null,
                    tint = colors.brandDeep,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = hint,
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row {
            PillButton(
                label = "هەوڵدانەوە",
                onClick = onRetry,
                height = 42.dp,
                fontSize = 12,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            PillButton(
                label = "ڕێکخستنی ناونیشان",
                onClick = onFixAddress,
                height = 42.dp,
                fontSize = 12,
                container = colors.brandSoft,
                contentColor = colors.brandDeep,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

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
    val syncIssue by viewModel.syncIssue.collectAsStateWithLifecycle()

    var tab by remember { mutableStateOf(HomeTab.CUSTOMERS) }
    var showSheet by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Customer?>(null) }
    var routeTarget by remember { mutableStateOf<Customer?>(null) }
    var deleteTarget by remember { mutableStateOf<Customer?>(null) }
    var profileTarget by remember { mutableStateOf<Customer?>(null) }
    var lastProfile by remember { mutableStateOf<Customer?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showGoodSheet by remember { mutableStateOf(false) }
    var editingGood by remember { mutableStateOf<com.yousif.delivery.data.GoodItem?>(null) }
    var presetCustomerId by remember { mutableStateOf<String?>(null) }
    var presetCustomerName by remember { mutableStateOf<String?>(null) }
    var presetOwnerKey by remember { mutableStateOf("") }
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
                onSettings = { showSettings = true },
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
                            .padding(horizontal = 16.dp, vertical = 6.dp),
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
                            if (syncIssue != null) {
                                item {
                                    Box(Modifier.animateItem()) {
                                    ServerNotice(
                                        code = syncIssue ?: 0,
                                        onRetry = { viewModel.retrySync() },
                                        onFixAddress = { viewModel.resetServerAddress() },
                                    )
                                    }
                                }
                            }
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
                                Box(Modifier.animateItem()) {
                                CustomerCard(
                                    customer = customer,
                                    distanceMeters = viewModel.distanceTo(customer),
                                    onOpen = { profileTarget = customer },
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
                }

                HomeTab.GOODS -> GoodsScreen(
                    goods = goods,
                    people = everyone,
                    onBump = { item, delta -> viewModel.bumpGood(item, delta) },
                    onRename = { item, title -> viewModel.renameGood(item, title) },
                    onNoteChange = { items, note -> viewModel.setGoodsNote(items, note) },
                    onDelete = { item -> viewModel.deleteGood(item) },
                    onAddFor = { customerId, customerName, ownerKey ->
                        presetCustomerId = customerId
                        presetCustomerName = customerName
                        presetOwnerKey = ownerKey
                        editingGood = null
                        showGoodSheet = true
                    },
                )

                HomeTab.MAP -> MapScreen(
                    customers = customers + shops,
                    myLocation = myLocation,
                    distanceOf = { customer -> viewModel.distanceTo(customer) },
                    onOpenRoute = { customer -> routeTarget = customer },
                    onOpenCustomer = { customer -> profileTarget = customer },
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
                        presetCustomerId = null
                        presetCustomerName = null
                        presetOwnerKey = java.util.UUID.randomUUID().toString()
                        showGoodSheet = true
                    } else {
                        editing = null
                        prefill = null
                        showSheet = true
                    }
                },
            )

            // «کردنەوە» shows the person; editing stays behind the pencil.
            // The live row is used so a saved edit shows up immediately.
            val liveProfile = profileTarget?.let { target ->
                everyone.firstOrNull { row -> row.id == target.id } ?: target
            }
            LaunchedEffect(liveProfile) {
                if (liveProfile != null) lastProfile = liveProfile
            }
            AnimatedVisibility(
                visible = liveProfile != null,
                enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { height -> height / 7 },
                exit = fadeOut(tween(140)) + slideOutVertically(tween(180)) { height -> height / 9 },
            ) {
                (liveProfile ?: lastProfile)?.let { person ->
                CustomerProfile(
                    customer = person,
                    goods = goods,
                    distanceMeters = viewModel.distanceTo(person),
                    onBack = { profileTarget = null },
                    onEdit = {
                        editing = person
                        prefill = null
                        showSheet = true
                    },
                    onRoute = { routeTarget = person },
                    onOpenMap = {
                        profileTarget = null
                        tab = HomeTab.MAP
                    },
                    onDelete = {
                        profileTarget = null
                        deleteTarget = person
                    },
                )
                }
            }
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
            presetCustomerId = presetCustomerId,
            presetCustomerName = presetCustomerName,
            presetOwnerKey = presetOwnerKey,
            onDismiss = {
                showGoodSheet = false
                editingGood = null
                presetCustomerId = null
                presetCustomerName = null
                presetOwnerKey = ""
            },
            onSave = { items ->
                viewModel.saveGoods(items) {
                    showGoodSheet = false
                    editingGood = null
                    presetCustomerId = null
                    presetCustomerName = null
                    presetOwnerKey = ""
                }
            },
            onAutoSave = { items -> viewModel.saveGoods(items) },
            onSaveNewOwner = { items, name, phone ->
                viewModel.saveGoodsForOwner(items, name, phone) {
                    showGoodSheet = false
                    editingGood = null
                    presetCustomerId = null
                    presetCustomerName = null
                    presetOwnerKey = ""
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
    onSettings: () -> Unit,
) {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background)
            .statusBarsPadding()
            .padding(start = 14.dp, end = 14.dp, top = 3.dp, bottom = 2.dp),
    ) {
        // One compact glass bar — the map keeps the rest of the screen.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(47.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(22.dp),
                    clip = false,
                    ambientColor = Color(0x1FA0785A),
                    spotColor = Color(0x1FA0785A),
                )
                .clip(RoundedCornerShape(22.dp))
                .background(
                    if (colors.isDark) {
                        Brush.linearGradient(listOf(Color(0xFF2C221C), Color(0xFF1C1613)))
                    } else {
                        Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFFFF4EA)))
                    },
                )
                .border(
                    1.dp,
                    if (colors.isDark) Color(0xFF40322A) else Color(0xFFFFFFFF),
                    RoundedCornerShape(22.dp),
                )
                .padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Notification / settings — its own raised orange glass button.
            Box {
                SettingsButton(onClick = onSettings)
                if (pending > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(15.dp)
                            .clip(RoundedCornerShape(50))
                            .background(colors.brand)
                            .border(1.5.dp, Color.White, RoundedCornerShape(50)),
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
            }

            Spacer(Modifier.weight(1f))

            Row(verticalAlignment = Alignment.CenterVertically) {
                ShiningBrand(size = 26.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "یوسف دلیڤەری",
                    color = colors.brand,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.6.sp,
                    maxLines = 1,
                )
            }

            Spacer(Modifier.weight(1f))

            Box {
                SoftIconButton(
                    icon = AppIcons.sync(),
                    onClick = onSync,
                    contentDescription = "هاوکاتکردن",
                    tint = colors.brand,
                    size = 36.dp,
                )
                if (syncing) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(15.dp)
                            .clip(RoundedCornerShape(50))
                            .background(colors.surface)
                            .border(1.5.dp, Color.White, RoundedCornerShape(50)),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(9.dp),
                            strokeWidth = 1.4.dp,
                            color = colors.brand,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            MiniChip(
                text = if (gpsActive) "GPS چالاکە" else "چاوەڕوانی GPS",
                leading = { GpsDot(active = gpsActive) },
                tint = colors.textSecondary,
                bold = false,
            )
            Spacer(Modifier.weight(1f))
            MiniChip(
                text = "ئەمڕۆ",
                leading = {
                    Icon(
                        AppIcons.clock(),
                        contentDescription = null,
                        tint = colors.brandDeep,
                        modifier = Modifier.size(13.dp),
                    )
                },
                tint = colors.brandDeep,
                bold = true,
            )
        }
    }
}

/** Compact pill used for the GPS / today chips under the header bar. */
@Composable
private fun MiniChip(
    text: String,
    leading: @Composable () -> Unit,
    tint: Color,
    bold: Boolean,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(colors.surface)
            .border(1.dp, colors.brand.copy(alpha = 0.30f), RoundedCornerShape(50))
            .padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            color = tint,
            fontSize = 10.5.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

/** The bell: a raised orange-glass button that cannot be missed. */
@Composable
private fun SettingsButton(onClick: () -> Unit) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .size(36.dp)
            .shadow(9.dp, shape, clip = false, ambientColor = Color(0x45F97316), spotColor = Color(0x45F97316))
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        colors.brand.copy(alpha = 0.22f),
                        colors.brand.copy(alpha = 0.07f),
                    ),
                ),
            )
            .border(1.3.dp, colors.brand.copy(alpha = 0.55f), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = AppIcons.settings(),
            contentDescription = "ڕێکخستن",
            tint = colors.brandDeep,
            modifier = Modifier.size(21.dp),
        )
    }
}

/** Brand badge with a slow light sweep — cheap: one animation on the screen. */
@Composable
private fun ShiningBrand(size: androidx.compose.ui.unit.Dp) {
    val transition = rememberInfiniteTransition(label = "brand")
    val sweep by transition.animateFloat(
        initialValue = -0.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sweep",
    )
    Box {
        BrandBadge(size = size)
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(size / 3))
                .drawWithContent {
                    drawContent()
                    val w = this.size.width.toFloat()
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.35f),
                                Color.Transparent,
                            ),
                            startX = sweep * w,
                            endX = sweep * w + w * 0.5f,
                        ),
                    )
                },
        )
    }
}

/* ------------------------------------------------------------------ search */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val colors = LocalAppColors.current
    GlassInput(
        value = query,
        onValueChange = onQueryChange,
        placeholder = "گەڕان: ناو، ژمارە، ناونیشان",
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        leading = AppIcons.search(),
        minHeight = 48.dp,
        textSize = 13.sp,
        trailing = {
            if (query.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(50))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(50))
                        .clickable { onQueryChange("") },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        AppIcons.close(),
                        contentDescription = "سڕینەوەی گەڕان",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
        },
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
            .shadow(24.dp, shape, clip = false, ambientColor = Color(0x3A8A5C33), spotColor = Color(0x3A8A5C33))
            .shadow(10.dp, shape, clip = false, ambientColor = Color(0x1FA0785A), spotColor = Color(0x1FA0785A))
            .clip(shape)
            .background(
                if (colors.isDark) {
                    Brush.verticalGradient(listOf(Color(0xFF3B2E26), Color(0xFF2A211B), Color(0xFF1B1512)))
                } else {
                    Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFFFF8F1), Color(0xFFFCEBDB)))
                },
            )
            .border(1.2.dp, if (colors.isDark) Color(0xFF4A3A31) else Color(0xFFFFFFFF), shape)
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
    val highlight by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(220),
        label = "navHighlight",
    )
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
                .graphicsLayer {
                    val scale = 0.92f + 0.08f * highlight
                    scaleX = scale
                    scaleY = scale
                }
                .shadow(
                    (10.dp * highlight),
                    RoundedCornerShape(50),
                    clip = false,
                    ambientColor = colors.brand.copy(alpha = 0.35f * highlight),
                    spotColor = colors.brand.copy(alpha = 0.35f * highlight),
                )
                .clip(RoundedCornerShape(50))
                .background(
                    if (highlight <= 0.01f) {
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                    } else {
                        Brush.verticalGradient(
                            listOf(
                                lerp(colors.brandSoft, Color.White, 0.35f * highlight),
                                colors.brandSoft,
                            ),
                        )
                    },
                )
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.55f * highlight),
                    RoundedCornerShape(50),
                ),
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
