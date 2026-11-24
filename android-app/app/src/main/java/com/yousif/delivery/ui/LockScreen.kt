package com.yousif.delivery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yousif.delivery.BuildConfig
import com.yousif.delivery.ui.icons.AppIcons
import com.yousif.delivery.ui.theme.LocalAppColors
import com.yousif.delivery.viewmodel.MainViewModel

@Composable
fun LockScreen(viewModel: MainViewModel) {
    val colors = LocalAppColors.current
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val diagnostics by viewModel.diagnostics.collectAsStateWithLifecycle()
    val baseUrl by viewModel.baseUrl.collectAsStateWithLifecycle()

    var password by remember { mutableStateOf("") }
    var showServerDialog by remember { mutableStateOf(false) }
    // The server address is not something a passer-by should ever read: it is
    // only reachable by long-pressing the logo.
    var advanced by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(colors.background, colors.surfaceAlt, colors.background),
                ),
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 26.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(colors.surface, CircleShape)
                    .pointerInput(Unit) {
                        detectTapGestures(onLongPress = { advanced = true })
                    },
                contentAlignment = Alignment.Center,
            ) {
                BrandBadge(size = 66.dp, icon = AppIcons.packageBox())
            }

            Spacer(Modifier.height(18.dp))
            Text(
                text = "یوسف دلیڤەری",
                color = colors.textPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "وشەی نهێنی بنووسە بۆ چوونەژوورەوە",
                color = colors.textSecondary,
                fontSize = 13.sp,
            )

            Spacer(Modifier.height(26.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    viewModel.clearAuthError()
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("وشەی نهێنی", fontSize = 13.sp, color = colors.textMuted) },
                leadingIcon = {
                    Icon(
                        AppIcons.lock(),
                        contentDescription = null,
                        tint = colors.brand,
                        modifier = Modifier.size(18.dp),
                    )
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { viewModel.unlock(password) }),
                shape = RoundedCornerShape(18.dp),
                textStyle = TextStyle(fontSize = 14.sp, color = colors.textPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedBorderColor = colors.brand,
                    unfocusedBorderColor = colors.border,
                    cursorColor = colors.brand,
                ),
            )

            Spacer(Modifier.height(16.dp))

            PillButton(
                label = "چوونەژوورەوە",
                onClick = { if (!busy) viewModel.unlock(password) },
                icon = AppIcons.check(),
                container = colors.brand,
                contentColor = colors.onBrand,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                height = 54.dp,
                fontSize = 15,
            )
            if (busy) {
                Spacer(Modifier.height(10.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = colors.brand,
                    strokeWidth = 2.dp,
                )
            }

            val error = authError
            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = error,
                    color = colors.danger,
                    fontSize = 11.5.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.dangerSoft, RoundedCornerShape(12.dp))
                        .padding(10.dp),
                )
            }

            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.Center) {
                if (advanced) {
                    TextButton(onClick = { showServerDialog = true }) {
                        Text("ڕێکخستنی سێرڤەر", color = colors.textSecondary, fontSize = 12.sp)
                    }
                    TextButton(onClick = { viewModel.diagnose() }, enabled = !busy) {
                        Text("پشکنینی سێرڤەر", color = colors.brandDeep, fontSize = 12.sp)
                    }
                }
            }
            Text(
                text = com.yousif.delivery.util.ServerPrivacy.status(baseUrl),
                color = colors.textMuted,
                fontSize = 10.sp,
            )
            Text(
                text = "وەشان " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")",
                color = colors.textMuted,
                fontSize = 9.5.sp,
            )
        }
    }

    val report = diagnostics
    if (report != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDiagnostics() },
            containerColor = colors.surface,
            title = { Text("پشکنینی سێرڤەر", color = colors.textPrimary) },
            text = {
                Column {
                    report.forEach { line ->
                        Text(text = line, color = colors.textSecondary, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissDiagnostics() }) { Text("داخستن") }
            },
        )
    }

    if (showServerDialog) {
        ServerUrlDialog(
            current = baseUrl,
            onDismiss = { showServerDialog = false },
            onSave = { url ->
                viewModel.updateBaseUrl(url)
                showServerDialog = false
            },
        )
    }
}

@Composable
fun ServerUrlDialog(
    current: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    val colors = LocalAppColors.current
    var value by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text("نیشانی سێرڤەر", color = colors.textPrimary) },
        text = {
            Column {
                Text(
                    "ئەگەر ناونیشانی سێرڤەرەکەت گۆڕا، لێرە بنووسە.",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    singleLine = true,
                    placeholder = { Text("https://…", fontSize = 12.sp, color = colors.textMuted) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colors.surface,
                        unfocusedContainerColor = colors.surface,
                        focusedBorderColor = colors.brand,
                        unfocusedBorderColor = colors.border,
                        cursorColor = colors.brand,
                    ),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(value) }) {
                Text("پاشەکەوتکردن", color = colors.brandDeep)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("پاشگەزبوونەوە", color = colors.textSecondary) }
        },
    )
}
