package com.yousif.delivery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yousif.delivery.data.Customer
import com.yousif.delivery.data.GoodItem
import com.yousif.delivery.ui.icons.AppIcons
import com.yousif.delivery.ui.theme.LocalAppColors
import com.yousif.delivery.util.TextUtils
import java.util.UUID

/**
 * «کاڵای کڕیار» — a creamy, glassy checklist of everything the driver has to
 * carry: one card per item, a quantity stepper, and an optional owner picked from
 * the phonebook (it can also stay empty).
 */
@Composable
fun GoodsScreen(
    goods: List<GoodItem>,
    onBump: (GoodItem, Int) -> Unit,
    onEdit: (GoodItem) -> Unit,
    onDelete: (GoodItem) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    var deleteTarget by remember { mutableStateOf<GoodItem?>(null) }

    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "کاڵای کڕیار",
                    color = colors.textPrimary,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "هەر کاڵایەک کە دەبێت بگەیەنیت — بە ژمارە و بە کەسەکەی",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                )
            }
            GlassChip(
                text = TextUtils.toKurdish(goods.size.toString()) + " کاڵا",
                icon = AppIcons.packageBox(),
            )
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillButton(
                        label = "زیادکردنی کاڵا",
                        onClick = onAdd,
                        icon = AppIcons.plus(),
                        container = colors.brand,
                        contentColor = colors.onBrand,
                        modifier = Modifier.weight(1f),
                        height = 50.dp,
                        fontSize = 14,
                    )
                }
            }

            if (goods.isEmpty()) {
                item {
                    GlassEmptyState(
                        icon = AppIcons.packageBox(),
                        title = "هێشتا هیچ کاڵایەک نییە",
                        hint = "بە دوگمەی «زیادکردنی کاڵا» دەست پێبکە — ناوێک بنووسە، ژمارە دیاری بکە،\n" +
                            "و ئەگەر ویستت کەسێک لە لیستی کڕیارەکان هەڵبژێرە.",
                    )
                }
            }

            items(items = goods, key = { it.id }) { item ->
                GoodCard(
                    item = item,
                    onBump = { delta -> onBump(item, delta) },
                    onEdit = { onEdit(item) },
                    onDelete = { deleteTarget = item },
                )
            }
        }
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = colors.surface,
            shape = RoundedCornerShape(26.dp),
            title = { Text("سڕینەوەی کاڵا", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "دڵنیایی لە سڕینەوەی «${target.title}»؟",
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(target)
                    deleteTarget = null
                }) { Text("سڕینەوە", color = colors.danger, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("پاشگەزبوونەوە") }
            },
        )
    }
}

@Composable
private fun GoodCard(
    item: GoodItem,
    onBump: (Int) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalAppColors.current

    GlassCard(padding = 14.dp, onClick = onEdit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(colors.brandSoft)
                    .border(1.dp, colors.brand.copy(alpha = 0.22f), RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    AppIcons.packageBox(),
                    contentDescription = null,
                    tint = colors.brandDeep,
                    modifier = Modifier.size(21.dp),
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = colors.textPrimary,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.note.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.note,
                        color = colors.textMuted,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    AppIcons.delete(),
                    contentDescription = "سڕینەوە",
                    tint = colors.textMuted,
                    modifier = Modifier.size(17.dp),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassStepButton(icon = AppIcons.minus(), onClick = { onBump(-1) })
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = TextUtils.toKurdish(item.total.toString()),
                    color = colors.textPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            GlassStepButton(icon = AppIcons.plus(), onClick = { onBump(1) })

            Spacer(Modifier.weight(1f))

            if (item.hasCustomer) {
                GlassChip(
                    text = item.customerName.orEmpty(),
                    icon = AppIcons.user(),
                )
            } else {
                GlassChip(
                    text = "بێ کەس",
                    icon = AppIcons.user(),
                    tint = colors.textMuted,
                    container = if (colors.isDark) Color(0xFF2A211C) else Color(0xFFFFF8F2),
                )
            }
        }
    }
}

/** Add / edit one item. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoodSheet(
    initial: GoodItem?,
    customers: List<Customer>,
    onDismiss: () -> Unit,
    onSave: (GoodItem) -> Unit,
) {
    val colors = LocalAppColors.current
    val focus = LocalFocusManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var quantity by remember { mutableStateOf(initial?.quantity?.coerceAtLeast(1) ?: 1) }
    var note by remember { mutableStateOf(initial?.note.orEmpty()) }
    var customerId by remember { mutableStateOf(initial?.customerId) }
    var customerName by remember { mutableStateOf(initial?.customerName) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, bottom = 22.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.brandSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        AppIcons.packageBox(),
                        contentDescription = null,
                        tint = colors.brandDeep,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(11.dp))
                Column {
                    Text(
                        text = if (initial == null) "کاڵای نوێ" else "دەستکاری کاڵا",
                        color = colors.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "ناو، ژمارە و ئەگەر ویستت کەسەکە",
                        color = colors.textSecondary,
                        fontSize = 11.5.sp,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text("کاڵا", color = colors.textPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    error = null
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("بۆ نموونە: ٥ کارتۆن ئاو", fontSize = 13.sp, color = colors.textMuted)
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                textStyle = TextStyle(fontSize = 14.5.sp, color = colors.textPrimary, fontWeight = FontWeight.Medium),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.surfaceSunken,
                    unfocusedContainerColor = colors.surfaceSunken,
                    focusedBorderColor = colors.brand.copy(alpha = 0.65f),
                    unfocusedBorderColor = colors.border,
                    cursorColor = colors.brand,
                ),
            )

            Spacer(Modifier.height(16.dp))

            Text("ژمارە", color = colors.textPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.surfaceSunken)
                    .border(1.dp, colors.border, RoundedCornerShape(18.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "بڕی کاڵا",
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f),
                )
                GlassStepButton(icon = AppIcons.minus(), onClick = { quantity = (quantity - 1).coerceAtLeast(1) })
                Box(Modifier.width(50.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = TextUtils.toKurdish(quantity.toString()),
                        color = colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                GlassStepButton(icon = AppIcons.plus(), onClick = { quantity = (quantity + 1).coerceAtMost(9999) })
            }

            Spacer(Modifier.height(16.dp))

            Text("کەس", color = colors.textPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    SelectChip(
                        label = "بێ کەس",
                        selected = customerId == null,
                        onClick = {
                            customerId = null
                            customerName = null
                        },
                    )
                }
                items(items = customers, key = { it.id }) { person ->
                    SelectChip(
                        label = person.name,
                        selected = customerId == person.id,
                        onClick = {
                            customerId = person.id
                            customerName = person.name
                        },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text("تێبینی", color = colors.textPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp),
                placeholder = { Text("ئارەزوومەندانە…", fontSize = 13.sp, color = colors.textMuted) },
                shape = RoundedCornerShape(16.dp),
                textStyle = TextStyle(fontSize = 13.5.sp, color = colors.textPrimary),
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.surfaceSunken,
                    unfocusedContainerColor = colors.surfaceSunken,
                    focusedBorderColor = colors.brand.copy(alpha = 0.65f),
                    unfocusedBorderColor = colors.border,
                    cursorColor = colors.brand,
                ),
            )

            error?.let { message ->
                Spacer(Modifier.height(8.dp))
                Text(message, color = colors.danger, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(18.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton(
                    label = if (initial == null) "زیادکردن" else "پاشەکەوتکردن",
                    onClick = {
                        focus.clearFocus()
                        if (title.isBlank()) error = "ناوی کاڵا پێویستە" else onSave(
                            GoodItem(
                                id = initial?.id ?: UUID.randomUUID().toString(),
                                title = title.trim(),
                                quantity = quantity,
                                note = note.trim(),
                                customerId = customerId,
                                customerName = customerName,
                                createdAt = initial?.createdAt ?: System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis(),
                            ),
                        )
                    },
                    icon = AppIcons.check(),
                    container = colors.brand,
                    contentColor = colors.onBrand,
                    modifier = Modifier.weight(1f),
                    height = 52.dp,
                    fontSize = 14,
                )
                PillButton(
                    label = "پاشگەزبوونەوە",
                    onClick = onDismiss,
                    container = colors.surfaceAlt,
                    contentColor = colors.textSecondary,
                    modifier = Modifier.weight(1f),
                    height = 52.dp,
                    fontSize = 13,
                )
            }
        }
    }
}

@Composable
private fun SelectChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) colors.brand else colors.surfaceSunken)
            .border(1.dp, if (selected) colors.brand else colors.border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            Icon(
                AppIcons.check(),
                contentDescription = null,
                tint = colors.onBrand,
                modifier = Modifier.size(13.dp),
            )
            Spacer(Modifier.width(5.dp))
        }
        Text(
            text = label,
            color = if (selected) colors.onBrand else colors.textSecondary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
