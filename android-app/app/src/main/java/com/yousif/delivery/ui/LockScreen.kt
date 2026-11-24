package com.yousif.delivery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yousif.delivery.ui.theme.AppBackground
import com.yousif.delivery.ui.theme.AppSurface
import com.yousif.delivery.ui.theme.AppTextPrimary
import com.yousif.delivery.ui.theme.AppTextSecondary
import com.yousif.delivery.ui.theme.NeonCyan
import com.yousif.delivery.viewmodel.MainViewModel

@Composable
fun LockScreen(viewModel: MainViewModel) {
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val baseUrl by viewModel.baseUrl.collectAsStateWithLifecycle()

    var password by remember { mutableStateOf("") }
    var showServerDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(AppBackground, Color(0xFF0D1524), AppBackground),
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
                    .size(88.dp)
                    .background(NeonCyan.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Place,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(44.dp),
                )
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = "یوسف دلیڤەری",
                color = AppTextPrimary,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "وشەی نهێنی بنووسە بۆ چوونەژوورەوە",
                color = AppTextSecondary,
                fontSize = 13.sp,
            )

            Spacer(Modifier.height(28.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("وشەی نهێنی") },
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { viewModel.unlock(password) }),
                shape = RoundedCornerShape(16.dp),
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { viewModel.unlock(password) },
                enabled = !busy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFF04202A),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("چوونەژوورەوە", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            Spacer(Modifier.height(10.dp))

            TextButton(onClick = { showServerDialog = true }) {
                Text("ڕێکخستنی سێرڤەر", color = AppTextSecondary, fontSize = 12.sp)
            }
            Text(
                text = baseUrl,
                color = AppTextSecondary.copy(alpha = 0.55f),
                fontSize = 10.sp,
            )
        }
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
    var value by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        title = { Text("نیشانی سێرڤەر", color = AppTextPrimary) },
        text = {
            Column {
                Text(
                    "ئەگەر ناونیشانی سێرڤەرەکەت گۆڕا، لێرە بنووسە.",
                    color = AppTextSecondary,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    singleLine = true,
                    label = { Text("https://…") },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(value) }) { Text("پاشەکەوتکردن") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("پاشگەزبوونەوە") }
        },
    )
}
