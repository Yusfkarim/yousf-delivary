package com.yousif.delivery.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yousif.delivery.BuildConfig
import com.yousif.delivery.ui.theme.AppDanger
import com.yousif.delivery.ui.theme.AppSurface
import com.yousif.delivery.ui.theme.AppSurfaceHigh
import com.yousif.delivery.ui.theme.AppTextPrimary
import com.yousif.delivery.ui.theme.AppTextSecondary
import com.yousif.delivery.ui.theme.MetallicGold
import com.yousif.delivery.util.TextUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    baseUrl: String,
    pendingCount: Int,
    onDismiss: () -> Unit,
    onSaveUrl: (String) -> Unit,
    onTest: () -> Unit,
    onSync: () -> Unit,
    onLock: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var url by remember { mutableStateOf(baseUrl) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppSurface,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 30.dp),
        ) {
            Text(
                text = "ڕێکخستن",
                color = AppTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )

            Spacer(Modifier.height(14.dp))

            SectionLabel("نیشانی سێرڤەر (Vercel)")
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Base URL") },
                placeholder = { Text("https://…vercel.app", fontSize = 12.sp) },
                shape = RoundedCornerShape(14.dp),
            )

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = { onSaveUrl(url) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) { Text("پاشەکەوتکردنی نیشان") }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = onTest,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) { Text("تاقیکردنەوەی پەیوەندی") }

            Spacer(Modifier.height(18.dp))
            DividerLine()
            Spacer(Modifier.height(14.dp))

            Text(
                text = if (pendingCount == 0) {
                    "هیچ گۆڕانکارییەکی چاوەڕوان نییە"
                } else {
                    "${TextUtils.toKurdish(pendingCount.toString())} گۆڕانکاری چاوەڕوانی ئینتەرنێتن"
                },
                color = if (pendingCount == 0) AppTextSecondary else MetallicGold,
                fontSize = 12.sp,
            )

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = onSync,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(Modifier.height(4.dp))
                Text("  هاوکاتکردن لەگەڵ سێرڤەر")
            }

            Spacer(Modifier.height(18.dp))
            DividerLine()
            Spacer(Modifier.height(14.dp))

            OutlinedButton(
                onClick = onLock,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = AppDanger)
                Text("  قفڵکردنی ئەپ", color = AppDanger)
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = "یوسف دلیڤەری · وەشان ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                color = AppTextSecondary.copy(alpha = 0.7f),
                fontSize = 11.sp,
            )
            Text(
                text = "ناوەختی پارێزراو بە وشەی نهێنی · داتا لە Vercel Postgres",
                color = AppSurfaceHigh,
                fontSize = 10.sp,
            )
            Spacer(Modifier.height(6.dp))
            Icon(Icons.Filled.Settings, contentDescription = null, tint = AppSurfaceHigh)
        }
    }
}
