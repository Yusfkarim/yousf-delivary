package com.yousif.delivery.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yousif.delivery.BuildConfig
import com.yousif.delivery.ui.icons.AppIcons
import com.yousif.delivery.ui.theme.LocalAppColors
import com.yousif.delivery.util.TextUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    onImport: (String, (Int) -> Unit) -> Unit,
    onLock: () -> Unit,
    onThemeMode: (Int) -> Unit,
) {
    val colors = LocalAppColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var url by remember { mutableStateOf(baseUrl) }
    var revealUrl by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val saveFile = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val json = onExport()
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(json.toByteArray())
                    } ?: error("no stream")
                }.isSuccess
            }
            Toast.makeText(
                context,
                if (ok) "داتاکە هەناردە کرا ✓" else "هەناردەکردن سەرکەوتوو نەبوو",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    val openFile = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
            }
            if (text.isNullOrBlank()) {
                Toast.makeText(context, "فایلەکە نەخوێندرایەوە", Toast.LENGTH_SHORT).show()
                return@launch
            }
            onImport(text) { count ->
                Toast.makeText(
                    context,
                    if (count > 0) "$count تۆمار هاوردە کرا ✓" else "هیچ تۆمارێک هاوردە نەکرا",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 30.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandBadge(size = 40.dp, icon = AppIcons.settings())
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "ڕێکخستن",
                        color = colors.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "ڕووکار، سێرڤەر و هاوکاتکردن",
                        color = colors.textSecondary,
                        fontSize = 11.5.sp,
                    )
                }
            }

            SectionHeader(text = "ڕووکار")

            Segmented(
                options = listOf("سیستەم", "ڕۆژ", "شەو"),
                selectedIndex = themeMode,
                modifier = Modifier.fillMaxWidth(),
                onSelect = onThemeMode,
            )

            SectionHeader(text = "سێرڤەر")

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
                    Text(if (revealUrl) "داخستن" else "گۆڕین", color = colors.brandDeep, fontSize = 11.5.sp)
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

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton(
                    label = "پاشەکەوتکردن",
                    onClick = { onSaveUrl(url) },
                    icon = AppIcons.check(),
                    container = colors.brand,
                    contentColor = colors.onBrand,
                    modifier = Modifier.weight(1f),
                )
                PillButton(
                    label = "تاقیکردنەوە",
                    onClick = onTest,
                    icon = AppIcons.sync(),
                    container = colors.surfaceAlt,
                    contentColor = colors.brandDeep,
                    modifier = Modifier.weight(1f),
                )
            }

            SectionHeader(text = "هاوکاتکردن")

            AppCard(padding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        AppIcons.cloudWait(),
                        contentDescription = null,
                        tint = if (pendingCount == 0) colors.success else colors.gold,
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
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(12.dp))
                PillButton(
                    label = "هاوکاتکردن لەگەڵ سێرڤەر",
                    onClick = onSync,
                    icon = AppIcons.sync(),
                    container = colors.brandSoft,
                    contentColor = colors.brandDeep,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SectionHeader(text = "پاڵپشت (Export / Import)")

            AppCard(padding = 14.dp) {
                Text(
                    text = "هەموو کڕیار و فرۆشگاکان لە فایلێکی JSON پاشەکەوت دەکرێن — دەتوانی لە Google Drive یان تێلێگرام هەڵیبگریت و لە مۆبایلێکی تر بیگەڕێنیتەوە.",
                    color = colors.textSecondary,
                    fontSize = 12.5.sp,
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillButton(
                        label = "هەناردەکردن",
                        onClick = { saveFile.launch(onExportName()) },
                        icon = AppIcons.share(),
                        container = colors.brand,
                        contentColor = colors.onBrand,
                        modifier = Modifier.weight(1f),
                        height = 48.dp,
                        fontSize = 13,
                    )
                    PillButton(
                        label = "هاوردەکردن",
                        onClick = { openFile.launch(arrayOf("application/json", "text/plain", "*/*")) },
                        icon = AppIcons.cloudDone(),
                        container = colors.brandSoft,
                        contentColor = colors.brandDeep,
                        modifier = Modifier.weight(1f),
                        height = 48.dp,
                        fontSize = 13,
                    )
                }
            }

            SectionHeader(text = "ئاسایش")

            PillButton(
                label = "قفڵکردنی ئەپ",
                onClick = onLock,
                icon = AppIcons.lock(),
                container = colors.dangerSoft,
                contentColor = colors.danger,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(18.dp))

            Text(
                text = "یوسف دلیڤەری · وەشان ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                color = colors.textMuted,
                fontSize = 11.sp,
            )
            Text(
                text = "ناوەختی پارێزراو بە وشەی نهێنی · داتا لە Supabase",
                color = colors.textMuted,
                fontSize = 10.sp,
            )
        }
    }
}
