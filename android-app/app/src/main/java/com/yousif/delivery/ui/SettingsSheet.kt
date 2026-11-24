package com.yousif.delivery.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yousif.delivery.BuildConfig
import com.yousif.delivery.ui.icons.AppIcons
import com.yousif.delivery.ui.theme.LocalAppColors
import com.yousif.delivery.util.TextUtils

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
    onLock: () -> Unit,
    onThemeMode: (Int) -> Unit,
) {
    val colors = LocalAppColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var url by remember { mutableStateOf(baseUrl) }

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

            SectionHeader(text = "نیشانی سێرڤەر (Vercel)")

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("https://…vercel.app", fontSize = 12.sp, color = colors.textMuted) },
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
