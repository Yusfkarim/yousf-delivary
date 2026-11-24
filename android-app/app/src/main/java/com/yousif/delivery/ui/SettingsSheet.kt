package com.yousif.delivery.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.yousif.delivery.BuildConfig
import com.yousif.delivery.data.ImportReport
import com.yousif.delivery.ui.icons.AppIcons
import com.yousif.delivery.ui.theme.LocalAppColors
import com.yousif.delivery.util.TextUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * «ڕێکخستن» — one sheet with everything the driver can tune.
 *
 * Each group has its own big glass medallion, a bold title and a milky card, so
 * the sheet reads like the goods screen instead of a list of switches. The
 * database group exports the whole database to a file on the phone (or straight
 * to Drive / Telegram) and imports it back without ever creating a duplicate.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    baseUrl: String,
    pendingCount: Int,
    themeMode: Int,
    onDismiss: () -> Unit,
    onSaveUrl: (String) -> Unit,
    onTest: () -> Unit,
    onSync: () -> Unit,
    onExport: suspend () -> String,
    onExportName: () -> String,
    onImport: (String, (ImportReport) -> Unit) -> Unit,
    onDedupe: ((Int) -> Unit) -> Unit,
    onLock: () -> Unit,
    onThemeMode: (Int) -> Unit,
    customerCount: Int = 0,
    shopCount: Int = 0,
    goodCount: Int = 0,
) {
    val colors = LocalAppColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var url by remember { mutableStateOf(baseUrl) }
    var revealUrl by remember { mutableStateOf(false) }
    var report by remember { mutableStateOf<ImportReport?>(null) }
    var dedupeResult by remember { mutableStateOf<Int?>(null) }
    var working by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    /** Writes the file the user picked in the system «Save to…» dialog. */
    val saveFile = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            working = true
            val json = onExport()
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(json.toByteArray())
                    } ?: error("no stream")
                }.isSuccess
            }
            working = false
            Toast.makeText(
                context,
                if (ok) "داتابەیس هەناردە کرا بۆ مۆبایل ✓" else "هەناردەکردن سەرکەوتوو نەبوو",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    /** Writes the file to the app cache and hands it to Drive / Telegram / WhatsApp. */
    fun shareBackup() {
        scope.launch {
            working = true
            val json = onExport()
            val name = onExportName()
            val file = withContext(Dispatchers.IO) {
                runCatching {
                    val folder = File(context.cacheDir, "exports").apply { mkdirs() }
                    File(folder, name).apply { writeText(json) }
                }.getOrNull()
            }
            working = false
            if (file == null) {
                Toast.makeText(context, "دروستکردنی فایل سەرکەوتوو نەبوو", Toast.LENGTH_SHORT).show()
                return@launch
            }
            runCatching {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.backup", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "یوسف دلیڤەری — پاشەکەوتی داتابەیس")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "ناردنی فایلی پاشەکەوت"))
            }.onFailure {
                Toast.makeText(context, "هیچ ئەپی ناردن نەدۆزرایەوە", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val openFile = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            working = true
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
            }
            working = false
            if (text.isNullOrBlank()) {
                Toast.makeText(context, "فایلەکە نەخوێندرایەوە", Toast.LENGTH_SHORT).show()
                return@launch
            }
            working = true
            onImport(text) { result ->
                working = false
                report = result
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 34.dp),
        ) {
            /* ------------------------------------------------------- hero */
            GlassCard(padding = 16.dp, radius = 26.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BrandBadge(size = 52.dp, icon = AppIcons.settings())
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "ڕێکخستن",
                            color = colors.textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "ڕووکار، داتابەیس، سێرڤەر و ئاسایش",
                            color = colors.textSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    StatPill(
                        text = "${TextUtils.toKurdish(customerCount.toString())} کڕیار",
                        icon = AppIcons.user(),
                        tint = colors.brandDeep,
                        soft = colors.brandSoft,
                    )
                    StatPill(
                        text = "${TextUtils.toKurdish(shopCount.toString())} فرۆشگا",
                        icon = AppIcons.store(),
                        tint = colors.brandDeep,
                        soft = colors.brandSoft,
                    )
                    StatPill(
                        text = "${TextUtils.toKurdish(goodCount.toString())} کاڵا",
                        icon = AppIcons.packageBox(),
                        tint = colors.brandDeep,
                        soft = colors.brandSoft,
                    )
                }
            }

            /* ------------------------------------------------------- theme */
            SheetSection(
                icon = AppIcons.sun(),
                title = "ڕووکار",
                subtitle = "ڕۆژ، شەو یان بەپێی سیستەم",
            ) {
                Segmented(
                    options = listOf("سیستەم", "ڕۆژ", "شەو"),
                    selectedIndex = themeMode,
                    modifier = Modifier.fillMaxWidth(),
                    onSelect = onThemeMode,
                )
            }

            /* ---------------------------------------------------- database */
            SheetSection(
                icon = AppIcons.database(),
                title = "داتابەیس",
                subtitle = "ئێکسپۆرت و ئیمپۆرت — کڕیار، فرۆشگا و کاڵا",
            ) {
                Text(
                    text = "هەموو داتابەیسەکە لە فایلێکی JSON دەنێردرێتە مۆبایلەکەت. دەتوانی " +
                        "لە فایلی مۆبایل هەڵیبژێریت یان ڕاستەوخۆ بۆ Google Drive، تێلێگرام " +
                        "یان واتسئەپ بنێریت و لە مۆبایلێکی تر بیگەڕێنیتەوە.",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 19.sp,
                )
                Spacer(Modifier.height(12.dp))
                PillButton(
                    label = if (working) "دروستکردن…" else "ئێکسپۆرت بۆ مۆبایل",
                    onClick = { saveFile.launch(onExportName()) },
                    icon = AppIcons.download(),
                    container = colors.brand,
                    contentColor = colors.onBrand,
                    modifier = Modifier.fillMaxWidth(),
                    height = 48.dp,
                    fontSize = 13,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillButton(
                        label = "ناردن بۆ Drive / تێلێگرام",
                        onClick = { shareBackup() },
                        icon = AppIcons.share(),
                        container = colors.brandSoft,
                        contentColor = colors.brandDeep,
                        modifier = Modifier.weight(1f),
                        height = 46.dp,
                        fontSize = 12,
                    )
                }
                Spacer(Modifier.height(14.dp))
                DividerLine()
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "ئیمپۆرت — ئەگەر هەمان فایل جارێکی تر بهێنیت، هیچ شتێک دووبارە " +
                        "نابێت: تۆمارەکان دەگۆڕدرێن و دووبارەکان خۆکار یەکدەخرێن و لە " +
                        "سێرڤەریش پاکدەکرێنەوە.",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 19.sp,
                )
                Spacer(Modifier.height(12.dp))
                PillButton(
                    label = if (working) "چاوەڕوان بە…" else "ئیمپۆرت لە فایل",
                    onClick = { openFile.launch(arrayOf("application/json", "text/plain", "*/*")) },
                    icon = AppIcons.cloudDone(),
                    container = colors.surfaceAlt,
                    contentColor = colors.brandDeep,
                    modifier = Modifier.fillMaxWidth(),
                    height = 48.dp,
                    fontSize = 13,
                )
            }

            /* ------------------------------------------------------- sync */
            SheetSection(
                icon = AppIcons.sync(),
                title = "هاوکاتکردن",
                subtitle = "سێرڤەر، دووبارەکان و گۆڕانکارییە چاوەڕوانەکان",
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        AppIcons.cloudWait(),
                        contentDescription = null,
                        tint = if (pendingCount == 0) colors.success else colors.gold,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(9.dp))
                    Text(
                        text = if (pendingCount == 0) {
                            "هیچ گۆڕانکارییەکی چاوەڕوان نییە"
                        } else {
                            "${TextUtils.toKurdish(pendingCount.toString())} گۆڕانکاری چاوەڕوانی ئینتەرنێتن"
                        },
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(12.dp))
                PillButton(
                    label = "هاوکاتکردن لەگەڵ سێرڤەر",
                    onClick = onSync,
                    icon = AppIcons.sync(),
                    container = colors.brand,
                    contentColor = colors.onBrand,
                    modifier = Modifier.fillMaxWidth(),
                    height = 46.dp,
                    fontSize = 13,
                )
                Spacer(Modifier.height(8.dp))
                PillButton(
                    label = "ڕێکخستنی دووبارەکان",
                    onClick = {
                        working = true
                        onDedupe { merged ->
                            working = false
                            dedupeResult = merged
                        }
                    },
                    icon = AppIcons.checkCircle(),
                    container = colors.brandSoft,
                    contentColor = colors.brandDeep,
                    modifier = Modifier.fillMaxWidth(),
                    height = 46.dp,
                    fontSize = 13,
                )
            }

            /* ----------------------------------------------------- server */
            SheetSection(
                icon = AppIcons.cloudDone(),
                title = "سێرڤەر",
                subtitle = "ناونیشانی API و پەیوەندی",
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surfaceSunken)
                        .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        AppIcons.lock(),
                        contentDescription = null,
                        tint = colors.brand,
                        modifier = Modifier.size(17.dp),
                    )
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = com.yousif.delivery.util.ServerPrivacy.status(baseUrl),
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "ناونیشان و وشەی نهێنی بە نهێنی دەمێننەوە",
                            color = colors.textMuted,
                            fontSize = 10.5.sp,
                        )
                    }
                    TextButton(onClick = { revealUrl = !revealUrl }) {
                        Text(
                            if (revealUrl) "داخستن" else "گۆڕین",
                            color = colors.brandDeep,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                if (revealUrl) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("https://…", fontSize = 12.sp, color = colors.textMuted) },
                        shape = RoundedCornerShape(16.dp),
                        textStyle = TextStyle(fontSize = 13.sp, color = colors.textPrimary),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.surface,
                            unfocusedContainerColor = colors.surface,
                            focusedBorderColor = colors.brand,
                            unfocusedBorderColor = colors.border,
                            cursorColor = colors.brand,
                        ),
                    )
                }

                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillButton(
                        label = "پاشەکەوتکردن",
                        onClick = { onSaveUrl(url) },
                        icon = AppIcons.check(),
                        container = colors.surfaceAlt,
                        contentColor = colors.brandDeep,
                        modifier = Modifier.weight(1f),
                        height = 46.dp,
                        fontSize = 12,
                    )
                    PillButton(
                        label = "تاقیکردنەوە",
                        onClick = onTest,
                        icon = AppIcons.sync(),
                        container = colors.surfaceAlt,
                        contentColor = colors.brandDeep,
                        modifier = Modifier.weight(1f),
                        height = 46.dp,
                        fontSize = 12,
                    )
                }
            }

            /* ---------------------------------------------------- security */
            SheetSection(
                icon = AppIcons.lock(),
                title = "ئاسایش",
                subtitle = "قفڵکردنی ئەپ بە وشەی نهێنی",
            ) {
                PillButton(
                    label = "قفڵکردنی ئەپ",
                    onClick = onLock,
                    icon = AppIcons.lock(),
                    container = colors.dangerSoft,
                    contentColor = colors.danger,
                    modifier = Modifier.fillMaxWidth(),
                    height = 46.dp,
                    fontSize = 13,
                )
            }

            Spacer(Modifier.height(22.dp))
            Text(
                text = "یوسف دلیڤەری · وەشان ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                color = colors.textMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "داتای کڕیار و فرۆشگا لە سێرڤەری تایبەت · کاڵاکان لە مۆبایل",
                color = colors.textMuted,
                fontSize = 10.sp,
            )
        }
    }

    report?.let { result ->
        AlertDialog(
            onDismissRequest = { report = null },
            containerColor = colors.surface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "ئیمپۆرت تەواو بوو",
                    color = colors.textPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            },
            text = {
                Column {
                    ReportLine("تۆمارە نوێیەکان", result.added, colors.brandDeep)
                    ReportLine("نوێکرایەوە (شوێنی کۆنەکە)", result.updated, colors.successDeep)
                    ReportLine("دووبارە یەکخرا و پاککرایەوە", result.merged, colors.gold)
                    ReportLine("کاڵا هاوردەکرا", result.goods, colors.brandDeep)
                    if (result.untouched) {
                        Text(
                            text = "داتابەیس پێشتر هەمان داتای هەبوو — هیچ شتێک دووبارە نەبوو.",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { report = null }) {
                    Text("باشە", color = colors.brandDeep, fontWeight = FontWeight.Bold)
                }
            },
        )
    }

    dedupeResult?.let { merged ->
        AlertDialog(
            onDismissRequest = { dedupeResult = null },
            containerColor = colors.surface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    text = "ڕێکخستنی دووبارەکان",
                    color = colors.textPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            },
            text = {
                Text(
                    text = if (merged > 0) {
                        "${TextUtils.toKurdish(merged.toString())} تۆماری دووبارە یەکخرا و " +
                            "لە سێرڤەریش پاککرایەوە. ئێستا هەر کەسێک تەنها جارێک هەیە."
                    } else {
                        "هیچ تۆمارێکی دووبارە نەدۆزرایەوە — داتابەیس ڕێک و پێکییە."
                    },
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = { dedupeResult = null }) {
                    Text("باشە", color = colors.brandDeep, fontWeight = FontWeight.Bold)
                }
            },
        )
    }
}

@Composable
private fun ReportLine(label: String, value: Int, color: Color) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = colors.textSecondary,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = TextUtils.toKurdish(value.toString()),
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

/** Big glass medallion + bold title, then a milky card with the group's controls. */
@Composable
private fun SheetSection(
    icon: ImageVector,
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 9.dp, start = 2.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(
                    Brush.linearGradient(
                        if (colors.isDark) {
                            listOf(Color(0xFF3A2C22), Color(0xFF261D17))
                        } else {
                            listOf(Color(0xFFFFFFFF), Color(0xFFFFEBDD))
                        },
                    ),
                )
                .border(
                    1.dp,
                    if (colors.isDark) Color(0xFF4A3A30) else Color(0xFFFFFFFF),
                    RoundedCornerShape(15.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.brand,
                modifier = Modifier.size(21.dp),
            )
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.textPrimary,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = subtitle,
                color = colors.textMuted,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
    GlassCard(padding = 14.dp, radius = 24.dp) { content() }
}
