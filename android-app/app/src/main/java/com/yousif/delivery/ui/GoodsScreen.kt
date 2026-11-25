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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yousif.delivery.data.Customer
import com.yousif.delivery.data.GoodItem
import com.yousif.delivery.data.Kinds
import com.yousif.delivery.ui.icons.AppIcons
import com.yousif.delivery.ui.theme.LocalAppColors
import com.yousif.delivery.util.TextUtils
import kotlinx.coroutines.delay
import java.util.UUID
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush

/**
 * «کاڵای کڕیار» — a clean **menu of owners**.
 *
 * The screen only lists the people who have goods (their full name, whether they
 * are a customer or a shop, and how many goods). Tapping one opens that person's
 * own page with every good inside — two customers can never be mixed up on the
 * same screen any more.
 *
 * The list is a list of **cells** («خانەی کاڵا»). Every cell keeps its own goods:
 * a new cell that is left without a person is a *new* «بێ کەس» — it never joins
 * the one before it. Goods are only written inside a cell, never at the list
 * level, and a good is renamed in place (the pencil) instead of opening a page.
 */
@Composable
fun GoodsScreen(
    goods: List<GoodItem>,
    people: List<Customer>,
    onBump: (GoodItem, Int) -> Unit,
    onRename: (GoodItem, String) -> Unit,
    onNoteChange: (List<GoodItem>, String) -> Unit,
    onAssignOwner: (List<GoodItem>, String, String, String?) -> Unit,
    onDelete: (GoodItem) -> Unit,
    onAddFor: (String?, String?, String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    var deleteTarget by remember { mutableStateOf<GoodItem?>(null) }
    var groupTarget by remember { mutableStateOf<GoodGroup?>(null) }
    var openKey by remember { mutableStateOf<String?>(null) }
    /** A cell that was just created and has no good in it yet. */
    var freshKey by remember { mutableStateOf<String?>(null) }

    val groups = remember(goods, people) {
        val byId = people.associateBy { it.id }
        goods.groupBy { item -> item.customerId ?: item.ownerKey.ifBlank { NOBODY_KEY } }
            .map { (key, items) ->
                val person = items.firstNotNullOfOrNull { item -> item.customerId?.let { byId[it] } }
                GoodGroup(
                    key = key,
                    customerId = person?.id
                        ?: items.firstNotNullOfOrNull { item -> item.customerId },
                    name = person?.name
                        ?: items.firstNotNullOfOrNull { item ->
                            item.customerName?.takeIf { name -> name.isNotBlank() }
                        }
                        ?: "بێ کەس",
                    kind = person?.kind,
                    items = items,
                )
            }
    }

    val open = groups.firstOrNull { group -> group.key == openKey }
        ?: openKey?.takeIf { key -> key == freshKey }
            ?.let { key ->
                GoodGroup(
                    key = key,
                    customerId = null,
                    name = "بێ کەس",
                    kind = null,
                    items = emptyList(),
                )
            }

    if (open == null) {
        OwnerMenu(
            groups = groups,
            totalGoods = goods.size,
            onOpen = { group -> openKey = group.key },
            onDeleteGroup = { group -> groupTarget = group },
            onAdd = {
                /* A new cell, never a merge into the «بێ کەس» of before. */
                val key = UUID.randomUUID().toString()
                freshKey = key
                openKey = key
            },
            modifier = modifier,
        )
    } else {
        OwnerDetail(
            group = open,
            onBack = {
                if (open.key == freshKey) freshKey = null
                openKey = null
            },
            people = people,
            onBump = onBump,
            onRename = onRename,
            onNoteChange = onNoteChange,
            onAssignOwner = onAssignOwner,
            onDelete = { item -> deleteTarget = item },
            onAdd = {
                /* Inside the cell: same person, or the very same «بێ کەس» cell. */
                val personId = open.customerId
                if (personId != null) {
                    onAddFor(personId, open.name, "")
                } else {
                    onAddFor(null, null, if (open.key == NOBODY_KEY) "" else open.key)
                }
            },
            modifier = modifier,
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = colors.surface,
            shape = RoundedCornerShape(26.dp),
            title = {
                Text(
                    text = "سڕینەوەی کاڵا",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.ExtraBold,
                )
            },
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

    groupTarget?.let { group ->
        AlertDialog(
            onDismissRequest = { groupTarget = null },
            containerColor = colors.surface,
            shape = RoundedCornerShape(26.dp),
            title = {
                Text(
                    text = "سڕینەوەی کاڵاکان",
                    color = colors.textPrimary,
                    fontWeight = FontWeight.ExtraBold,
                )
            },
            text = {
                Text(
                    text = "هەموو کاڵاکانی «${group.name}» بسرێتەوە؟ " +
                        "(${TextUtils.toKurdish(group.items.size.toString())} جۆر) " +
                        "ئەمە تەنها کاڵاکان دەسڕێتەوە — زانیاری کەسەکە لە سێرڤەر دەمێنێتەوە.",
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    group.items.forEach { item -> onDelete(item) }
                    groupTarget = null
                    openKey = null
                }) { Text("سڕینەوە", color = colors.danger, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { groupTarget = null }) { Text("پاشگەزبوونەوە") }
            },
        )
    }
}

/* ------------------------------------------------------------- menu of owners */

@Composable
private fun OwnerMenu(
    groups: List<GoodGroup>,
    totalGoods: Int,
    onOpen: (GoodGroup) -> Unit,
    onDeleteGroup: (GoodGroup) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
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
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "بۆ بینینی کاڵاکانی هەر کەسێک، ناوەکەی بکەرەوە",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                )
            }
            GlassChip(
                text = TextUtils.toKurdish(totalGoods.toString()) + " کاڵا",
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
                DashedAddButton(
                    label = "زیادکردنی خانەی کاڵا",
                    onClick = onAdd,
                )
            }

            if (groups.isEmpty()) {
                item {
                    GlassEmptyState(
                        icon = AppIcons.packageBox(),
                        title = "هێشتا هیچ کاڵایەک نییە",
                        hint = "بە دوگمەی «زیادکردنی خانەی کاڵا» خانەیەکی نوێ دروست دەبێت — " +
                            "پاشان لەناو خۆی کاڵاکان بنووسە و ئەگەر ویستت کەسێک هەڵبژێرە. " +
                            "هەر خانەیەک کاڵاکانی خۆی دەبێت، تێکەڵ نابن.",
                    )
                }
            }

            items(items = groups, key = { group -> group.key }) { group ->
                Box(Modifier.animateItem()) {
                    OwnerCard(
                        group = group,
                        onClick = { onOpen(group) },
                        onDelete = { onDeleteGroup(group) },
                    )
                }
            }
        }
    }
}

/** One line of the menu: the whole name, the kind and how much he carries. */
@Composable
private fun OwnerCard(group: GoodGroup, onClick: () -> Unit, onDelete: () -> Unit) {
    val colors = LocalAppColors.current
    val nobody = group.customerId == null
    val isShop = group.kind == Kinds.SHOP
    val pieces = group.items.sumOf { item -> item.total }

    GlassCard(padding = 14.dp, radius = 24.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(if (nobody) colors.surfaceAlt else colors.brandSoft)
                    .border(
                        1.dp,
                        if (nobody) colors.border else colors.brand.copy(alpha = 0.22f),
                        RoundedCornerShape(15.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (nobody) {
                    Icon(
                        AppIcons.user(),
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(21.dp),
                    )
                } else {
                    Text(
                        text = TextUtils.initials(group.name),
                        color = colors.brandDeep,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    color = colors.textPrimary,
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                )
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!nobody) {
                        GlassChip(
                            text = if (isShop) "فرۆشگا" else "کڕیار",
                            icon = if (isShop) AppIcons.store() else AppIcons.user(),
                            tint = colors.brandDeep,
                            container = colors.brandSoft,
                        )
                        Spacer(Modifier.width(7.dp))
                    }
                    Text(
                        text = "${TextUtils.toKurdish(group.items.size.toString())} جۆر · " +
                            "${TextUtils.toKurdish(pieces.toString())} دانە",
                        color = colors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(colors.danger.copy(alpha = if (colors.isDark) 0.18f else 0.08f))
                    .border(1.dp, colors.danger.copy(alpha = 0.20f), RoundedCornerShape(11.dp))
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    AppIcons.delete(),
                    contentDescription = "سڕینەوەی کاڵاکان",
                    tint = colors.danger,
                    modifier = Modifier.size(17.dp),
                )
            }
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(50))
                    .background(colors.surfaceSunken),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    AppIcons.chevronStart(),
                    contentDescription = "کردنەوە",
                    tint = colors.brandDeep,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}

/* ------------------------------------------------------------- one owner page */

@Composable
private fun OwnerDetail(
    group: GoodGroup,
    people: List<Customer>,
    onBack: () -> Unit,
    onBump: (GoodItem, Int) -> Unit,
    onRename: (GoodItem, String) -> Unit,
    onNoteChange: (List<GoodItem>, String) -> Unit,
    onAssignOwner: (List<GoodItem>, String, String, String?) -> Unit,
    onDelete: (GoodItem) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val nobody = group.customerId == null
    val isShop = group.kind == Kinds.SHOP

    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SoftIconButton(
                icon = AppIcons.chevronEnd(),
                onClick = onBack,
                contentDescription = "گەڕانەوە",
                size = 42.dp,
            )
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    color = colors.textPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = TextUtils.toKurdish(group.items.size.toString()) + " جۆری کاڵا",
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (!nobody) {
                GlassChip(
                    text = if (isShop) "فرۆشگا" else "کڕیار",
                    icon = if (isShop) AppIcons.store() else AppIcons.user(),
                    tint = colors.brandDeep,
                    container = colors.brandSoft,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                OwnerBar(
                    group = group,
                    people = people,
                    onAssign = { phone, name ->
                        onAssignOwner(group.items, phone, name, group.customerId)
                    },
                )
            }

            if (group.items.isEmpty()) {
                item {
                    GlassEmptyState(
                        icon = AppIcons.packageBox(),
                        title = "ئەم خانەیە بەتاڵە",
                        hint = "کاڵاکانی ئەم خانەیە لە خۆی زیاد بکە — «زیادکردنی کاڵا بۆ ئەم کەسە» دابگرە، " +
                            "پاشان کەسێک هەڵبژێرە یان بێ کەس بەجێی بهێڵە.",
                    )
                }
            } else {
                item {
                    GlassCard(padding = 12.dp, radius = 24.dp) {
                        group.items.forEachIndexed { index, item ->
                            if (index > 0) DividerLine()
                            GoodLine(
                                index = index,
                                item = item,
                                onRename = onRename,
                                onBump = onBump,
                                onDelete = onDelete,
                            )
                        }
                    }
                }
            }

            /* The note belongs to the whole cell — it is shown outside the goods,
               never printed again under every single good. */
            if (group.items.isNotEmpty()) {
                val notes = group.items
                    .map { item -> item.note.trim() }
                    .filter { text -> text.isNotEmpty() }
                    .distinct()
                item {
                    NoteCard(
                        text = notes.joinToString(" · "),
                        onSave = { text -> onNoteChange(group.items, text) },
                    )
                }
            }

            item {
                DashedAddButton(
                    label = "زیادکردنی کاڵا بۆ ئەم کەسە",
                    onClick = onAdd,
                )
            }
        }
    }
}

/**
 * «کەس» of the cell — the number cell that sits **above the goods**.
 *
 * Type the number and press the tick: the person who owns that number is found
 * and the whole cell moves to him; if the number is new, that person is created
 * once, with the goods attached — never a second copy, never a split.
 */
@Composable
private fun OwnerBar(
    group: GoodGroup,
    people: List<Customer>,
    onAssign: (phone: String, name: String) -> Unit,
) {
    val colors = LocalAppColors.current
    val focus = LocalFocusManager.current
    val person = remember(group.customerId, people) {
        people.firstOrNull { it.id == group.customerId }
    }
    val savedPhone = person?.primaryPhone.orEmpty()
    var phone by remember(group.key) { mutableStateOf(savedPhone) }
    var name by remember(group.key) { mutableStateOf("") }

    LaunchedEffect(savedPhone) {
        if (phone.isBlank() && savedPhone.isNotBlank()) phone = savedPhone
    }

    val found = matchedPerson(phone, people)
    val isNew = phone.filter(Char::isDigit).length >= 7 && found == null

    GlassCard(padding = 12.dp, radius = 22.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(if (person == null) colors.surfaceAlt else colors.brandSoft)
                    .border(
                        1.dp,
                        if (person == null) colors.border else colors.brand.copy(alpha = 0.25f),
                        RoundedCornerShape(13.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (person == null) {
                    Icon(
                        AppIcons.user(),
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(17.dp),
                    )
                } else {
                    Text(
                        text = TextUtils.initials(person.name),
                        color = colors.brandDeep,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "کەس",
                    color = colors.textMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    text = found?.name ?: person?.name ?: "بێ کەس",
                    color = colors.textPrimary,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.height(9.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassInput(
                value = phone,
                onValueChange = { phone = it },
                placeholder = "ژمارەی کەس…",
                leading = AppIcons.call(),
                minHeight = 44.dp,
                keyboardType = KeyboardType.Phone,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(7.dp))
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .shadow(
                        8.dp,
                        RoundedCornerShape(14.dp),
                        clip = false,
                        ambientColor = colors.brand.copy(alpha = 0.45f),
                        spotColor = colors.brand.copy(alpha = 0.45f),
                    )
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(lerp(colors.brand, Color.White, 0.22f), colors.brand),
                        ),
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .clickable {
                        if (phone.filter(Char::isDigit).length >= 7) {
                            onAssign(phone.trim(), name.trim())
                            focus.clearFocus()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    AppIcons.check(),
                    contentDescription = "دانانی کەس",
                    tint = colors.onBrand,
                    modifier = Modifier.size(19.dp),
                )
            }
        }

        if (isNew) {
            Spacer(Modifier.height(8.dp))
            GlassInput(
                value = name,
                onValueChange = { name = it },
                placeholder = "ناوی ئەم کەسی نوێ…",
                leading = AppIcons.user(),
                minHeight = 44.dp,
            )
        }

        Spacer(Modifier.height(7.dp))
        Text(
            text = when {
                found != null -> "ژمارەکە هی «" + found.name + "»ە — کاڵاکان دەچنە لای ئەو"
                isNew -> "کەسێکی نوێ بەم ژمارەیە دروست دەبێت و کاڵاکان بۆی دەخەزنرێن"
                person != null -> "ژمارەیەکی نوێ بنووسە و ✓ دابگرە بۆ نوێکردنەوەی کەسەکە"
                else -> "ژمارە بنووسە و ✓ دابگرە — کەسەکە دەدۆزرێتەوە یان دروست دەبێت"
            },
            color = colors.textSecondary,
            fontSize = 11.sp,
        )
    }
}

/**
 * Number, the name and the quantity stepper of one good.
 *
 * The name is edited **here**, in the row itself: the pencil opens a small field,
 * the tick saves it. Tapping the row never opens another page.
 */
@Composable
private fun GoodLine(
    index: Int,
    item: GoodItem,
    onRename: (GoodItem, String) -> Unit,
    onBump: (GoodItem, Int) -> Unit,
    onDelete: (GoodItem) -> Unit,
) {
    val colors = LocalAppColors.current
    val focus = LocalFocusManager.current
    var editing by remember(item.id) { mutableStateOf(false) }
    var draft by remember(item.id) { mutableStateOf(item.title) }

    fun commit() {
        val clean = draft.trim()
        if (clean.isNotEmpty() && clean != item.title) onRename(item, clean)
        draft = item.title
        editing = false
        focus.clearFocus()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(50))
                .background(colors.brand),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = TextUtils.toKurdish((index + 1).toString()),
                color = colors.onBrand,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Spacer(Modifier.width(9.dp))

        if (editing) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(13.dp))
                    .background(colors.surfaceSunken)
                    .border(1.dp, colors.brand.copy(alpha = 0.5f), RoundedCornerShape(13.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                    ),
                    cursorBrush = SolidColor(colors.brand),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { commit() }),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (draft.isEmpty()) {
                                Text(
                                    text = "ناوی کاڵا بنووسە…",
                                    color = colors.textMuted,
                                    fontSize = 12.5.sp,
                                )
                            }
                            inner()
                        }
                    },
                )
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(50))
                        .background(colors.brand)
                        .clickable { commit() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        AppIcons.check(),
                        contentDescription = "پاشەکەوتکردنی ناو",
                        tint = colors.onBrand,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable {
                        draft = item.title
                        editing = false
                        focus.clearFocus()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    AppIcons.close(),
                    contentDescription = "پاشگەزبوونەوە",
                    tint = colors.textMuted,
                    modifier = Modifier.size(15.dp),
                )
            }
        } else {
            Text(
                text = item.title,
                color = colors.textPrimary,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(5.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(50))
                    .background(colors.brand.copy(alpha = if (colors.isDark) 0.22f else 0.10f))
                    .clickable {
                        draft = item.title
                        editing = true
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    AppIcons.edit(),
                    contentDescription = "دەستکاری ناو",
                    tint = colors.brandDeep,
                    modifier = Modifier.size(14.dp),
                )
            }
            Spacer(Modifier.width(6.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(colors.surfaceSunken)
                    .border(1.dp, colors.border, RoundedCornerShape(50))
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StepCircle(AppIcons.minus()) { onBump(item, -1) }
                Box(Modifier.width(36.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = TextUtils.toKurdish(item.total.toString()),
                        color = colors.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                StepCircle(AppIcons.plus()) { onBump(item, 1) }
            }
            Spacer(Modifier.width(2.dp))
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable { onDelete(item) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    AppIcons.delete(),
                    contentDescription = "سڕینەوە",
                    tint = colors.textMuted,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/**
 * «تێبینی» of a cell — drawn once, **outside** the goods card.
 *
 * Tapping it opens a small field in place: the note is written and saved here,
 * exactly like a good's name. Nothing opens a page of its own.
 */
@Composable
private fun NoteCard(text: String, onSave: (String) -> Unit) {
    val colors = LocalAppColors.current
    val focus = LocalFocusManager.current
    var editing by remember(text) { mutableStateOf(false) }
    var draft by remember(text) { mutableStateOf(text) }

    fun commit() {
        if (draft.trim() != text) onSave(draft.trim())
        editing = false
        focus.clearFocus()
    }

    GlassCard(
        padding = 13.dp,
        radius = 22.dp,
        onClick = {
            if (!editing) {
                draft = text
                editing = true
            }
        },
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
                    AppIcons.edit(),
                    contentDescription = null,
                    tint = colors.brandDeep,
                    modifier = Modifier.size(15.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            if (editing) {
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(
                        fontSize = 12.5.sp,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    cursorBrush = SolidColor(colors.brand),
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { commit() }),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (draft.isEmpty()) {
                                Text(
                                    text = "تێبینی بنووسە…",
                                    color = colors.textMuted,
                                    fontSize = 12.sp,
                                )
                            }
                            inner()
                        }
                    },
                )
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(50))
                        .background(colors.brand)
                        .clickable { commit() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        AppIcons.check(),
                        contentDescription = "پاشەکەوتکردنی تێبینی",
                        tint = colors.onBrand,
                        modifier = Modifier.size(14.dp),
                    )
                }
            } else {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "تێبینی",
                        color = colors.brandDeep,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = text.ifBlank { "دەستبدە بۆ نووسینی تێبینی…" },
                        color = if (text.isBlank()) colors.textMuted else colors.textSecondary,
                        fontSize = 12.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** The saved person whose number ends with the digits typed (last 9). */
private fun matchedPerson(raw: String, people: List<Customer>): Customer? {
    val digits = raw.filter(Char::isDigit).takeLast(9)
    if (digits.length < 7) return null
    return people.firstOrNull { person ->
        person.allPhones.any { number ->
            number.filter(Char::isDigit).takeLast(9) == digits
        }
    }
}

/**
 * «داواکاریی نوێ» — one sheet for a whole order: pick the person (or «بێ کەس»),
 * write as many goods rows as you need, and save them all at once.
 *
 * Editing an existing row opens the same sheet with that one row filled in.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoodSheet(
    initial: GoodItem?,
    customers: List<Customer>,
    onDismiss: () -> Unit,
    onCommit: (
        items: List<GoodItem>,
        customerId: String?,
        customerName: String,
        phone: String,
        typedName: String,
    ) -> Unit,
    presetCustomerId: String? = null,
    presetCustomerName: String? = null,
    presetOwnerKey: String = "",
    onAutoSave: (List<GoodItem>) -> Unit = {},
) {
    val colors = LocalAppColors.current
    val focus = LocalFocusManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var rows by remember {
        mutableStateOf(
            listOf(
                GoodDraft(
                    id = initial?.id ?: UUID.randomUUID().toString(),
                    title = initial?.title.orEmpty(),
                    quantity = (initial?.quantity ?: 1).coerceAtLeast(1),
                    createdAt = initial?.createdAt ?: 0L,
                ),
            ),
        )
    }
    var note by remember { mutableStateOf(initial?.note.orEmpty()) }
    var query by remember { mutableStateOf("") }
    var customerId by remember { mutableStateOf(initial?.customerId ?: presetCustomerId) }
    var customerName by remember { mutableStateOf(initial?.customerName ?: presetCustomerName) }
    /* Which «خانەی کاڵا» the goods belong to while they have no person. */
    var ownerKey by remember { mutableStateOf(initial?.ownerKey ?: presetOwnerKey) }
    var error by remember { mutableStateOf<String?>(null) }
    var ownerPhone by remember {
        mutableStateOf(
            (presetCustomerId ?: initial?.customerId)
                ?.let { id -> customers.firstOrNull { person -> person.id == id }?.primaryPhone }
                .orEmpty(),
        )
    }
    var ownerTypedName by remember { mutableStateOf("") }
    /* «بێ کەس» is a choice, never a default: nothing is highlighted until the
       driver taps it (or he came from a «بێ کەس» cell). */
    var nobodyChosen by remember {
        mutableStateOf(
            (initial?.customerId ?: presetCustomerId) == null && presetOwnerKey.isNotBlank(),
        )
    }
    val typedDigits = ownerPhone.filter(Char::isDigit)
    val phoneMatch: Customer? = if (typedDigits.length >= 7) matchedPerson(ownerPhone, customers) else null
    val phoneIsNew = typedDigits.length >= 7 && phoneMatch == null

    fun update(index: Int, change: (GoodDraft) -> GoodDraft) {
        val copy = rows.toMutableList()
        if (index in copy.indices) copy[index] = change(copy[index])
        rows = copy
    }

    /* Everything typed here is written straight away: swiping the sheet away or
       closing the app can never take a good with it. */
    fun buildItems(): List<GoodItem> {
        val now = System.currentTimeMillis()
        return rows
            .filter { draft -> draft.title.isNotBlank() }
            .map { draft ->
                GoodItem(
                    id = draft.id,
                    title = draft.title.trim(),
                    quantity = draft.quantity.coerceAtLeast(1),
                    note = note.trim(),
                    customerId = customerId,
                    customerName = customerName,
                    ownerKey = if (customerId == null) ownerKey else "",
                    createdAt = if (draft.createdAt > 0) draft.createdAt else now,
                    updatedAt = now,
                )
            }
    }

    LaunchedEffect(rows.map { draft -> draft.title to draft.quantity }, note, customerId) {
        val items = buildItems()
        if (items.isEmpty()) return@LaunchedEffect
        delay(500)
        onAutoSave(items)
    }

    val digits = query.filter(Char::isDigit)
    val people = remember(query, customers) {
        if (query.isBlank()) {
            customers.take(30)
        } else {
            customers
                .filter { person ->
                    person.name.contains(query.trim(), ignoreCase = true) ||
                        (digits.isNotEmpty() && person.allPhones.any { number ->
                            number.filter(Char::isDigit).contains(digits)
                        })
                }
                .take(30)
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            val items = buildItems()
            if (items.isNotEmpty()) {
                /* Closing the sheet never invents a new person, but goods that
                   already have a person (picked or found by number) are moved to
                   him at once — they can never stay behind in «بێ کەس». */
                if (customerId != null || phoneMatch != null) {
                    onCommit(
                        items,
                        customerId,
                        customerName.orEmpty(),
                        ownerPhone.trim(),
                        ownerTypedName.trim(),
                    )
                } else {
                    onAutoSave(items)
                }
            }
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = colors.surface,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 22.dp),
        ) {
            /* -------------------------------------------------------- header */
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (initial == null) "داواکاریی نوێ" else "دەستکاری کاڵا",
                        color = colors.textPrimary,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "کڕیار هەڵبژێرە و کاڵاکان خێرا بنووسە",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(10.dp, RoundedCornerShape(16.dp), clip = false, ambientColor = Color(0x33F97316), spotColor = Color(0x33F97316))
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.verticalGradient(listOf(Color(0xFFFFFFFF), colors.brandSoft)),
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        AppIcons.packageBox(),
                        contentDescription = null,
                        tint = colors.brandDeep,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            /* ------------------------------------------------------------- کەس
               Number first — the saved name comes up by itself. Nothing is
               picked automatically: «بێ کەس» is a choice, not a default. */
            Text(
                text = "کەس",
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(8.dp))
            GlassInput(
                value = ownerPhone,
                onValueChange = { typed ->
                    ownerPhone = typed
                    val found = matchedPerson(typed, customers)
                    if (found != null) {
                        customerId = found.id
                        customerName = found.name
                        ownerKey = ""
                        nobodyChosen = false
                    }
                },
                placeholder = "ژمارەی کەس — بۆ دۆزینەوە یان کەسێکی نوێ…",
                leading = AppIcons.call(),
                minHeight = 46.dp,
                keyboardType = KeyboardType.Phone,
            )

            /* What the app understood from that number — said plainly. */
            val pickedName: String? = customerName
            val statusText: String? = when {
                phoneMatch != null ->
                    "دۆزرایەوە: «" + phoneMatch.name + "» — کاڵاکان بۆ ئەم کەسە دەخەزنرێن"
                phoneIsNew && nobodyChosen ->
                    "کەسێکی نوێ بەم ژمارەیە دروست دەبێت"
                phoneIsNew ->
                    "کەسێکی نوێ بەم ژمارەیە دروست دەبێت — ناوەکەی بنووسە"
                nobodyChosen ->
                    "بێ کەس — کاڵاکان بە بێ ناو دەخەزنرێن"
                pickedName != null ->
                    "دەبێت بۆ «" + pickedName + "» — دەتوانیت بگۆڕیت"
                else ->
                    "کەسێک هەڵبژێرە، ژمارەیەک بنووسە، یان «بێ کەس» دابگرە"
            }
            statusText?.let { text ->
                Spacer(Modifier.height(7.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(13.dp))
                        .background(colors.brandSoft.copy(alpha = 0.7f))
                        .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(13.dp))
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (phoneMatch != null || pickedName != null) AppIcons.check() else AppIcons.user(),
                        contentDescription = null,
                        tint = colors.brandDeep,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = text,
                        color = colors.brandDeep,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            if (phoneIsNew || (customerId == null && ownerPhone.isNotBlank() && phoneMatch == null)) {
                Spacer(Modifier.height(8.dp))
                GlassInput(
                    value = ownerTypedName,
                    onValueChange = { ownerTypedName = it },
                    placeholder = "ناوی کەسی نوێ…",
                    leading = AppIcons.user(),
                    minHeight = 46.dp,
                )
            }

            Spacer(Modifier.height(8.dp))
            GlassInput(
                value = query,
                onValueChange = { query = it },
                placeholder = "گەڕان بە ناو…",
                leading = AppIcons.search(),
                minHeight = 42.dp,
                textSize = 12.5.sp,
            )

            Spacer(Modifier.height(9.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    SelectChip(
                        label = "بێ کەس",
                        selected = customerId == null && nobodyChosen,
                        onClick = {
                            customerId = null
                            customerName = null
                            nobodyChosen = true
                            ownerTypedName = ""
                            /* A cell of its own from now on — never merged. */
                            if (ownerKey.isBlank()) ownerKey = UUID.randomUUID().toString()
                        },
                    )
                }
                items(items = people, key = { it.id }) { person ->
                    SelectChip(
                        label = person.name,
                        selected = customerId == person.id,
                        onClick = {
                            customerId = person.id
                            customerName = person.name
                            nobodyChosen = false
                            ownerKey = ""
                            ownerPhone = person.primaryPhone
                            ownerTypedName = ""
                        },
                    )
                }
            }

            /* ---------------------------------------------------------- goods */
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "کاڵاکان",
                    color = colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(colors.brandSoft)
                        .padding(horizontal = 11.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = TextUtils.toKurdish(rows.size.toString()) + " کاڵا",
                        color = colors.brandDeep,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }

            Spacer(Modifier.height(9.dp))

            rows.forEachIndexed { index, row ->
                if (index > 0) Spacer(Modifier.height(9.dp))
                GoodRow(
                    index = index,
                    title = row.title,
                    quantity = row.quantity,
                    onTitle = { value ->
                        update(index) { it.copy(title = value) }
                        error = null
                    },
                    onQuantity = { delta ->
                        update(index) {
                            it.copy(quantity = (it.quantity + delta).coerceIn(1, 9999))
                        }
                    },
                    onRemove = {
                        if (rows.size > 1) {
                            rows = rows.toMutableList().also { it.removeAt(index) }
                        } else {
                            update(index) { it.copy(title = "") }
                        }
                    },
                )
            }

            Spacer(Modifier.height(10.dp))
            DashedAddButton(
                label = "زیادکردنی خانەی کاڵا",
                onClick = {
                    rows = rows + GoodDraft(
                        id = UUID.randomUUID().toString(),
                        title = "",
                        quantity = 1,
                        createdAt = 0L,
                    )
                },
            )

            /* ----------------------------------------------------------- note */
            Spacer(Modifier.height(18.dp))
            Text(
                text = "تێبینی",
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
                placeholder = { Text("ئارەزوومەندانە…", fontSize = 13.sp, color = colors.textMuted) },
                shape = RoundedCornerShape(18.dp),
                textStyle = TextStyle(fontSize = 13.5.sp, color = colors.textPrimary),
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colors.surfaceSunken,
                    unfocusedContainerColor = colors.surfaceSunken,
                    focusedBorderColor = colors.brand.copy(alpha = 0.55f),
                    unfocusedBorderColor = colors.border,
                    cursorColor = colors.brand,
                ),
            )

            error?.let { message ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = message,
                    color = colors.danger,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            /* --------------------------------------------------------- save */
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton(
                    label = "پاشەکەوتکردن",
                    onClick = {
                        focus.clearFocus()
                        val items = buildItems()
                        if (items.isEmpty()) {
                            error = "لانیکەم ناوێکی کاڵا بنووسە"
                        } else {
                            /* One call decides the owner: matched number, the
                               picked person, a new one, or «بێ کەس». */
                            onCommit(
                                items,
                                customerId,
                                customerName.orEmpty(),
                                ownerPhone.trim(),
                                ownerTypedName.trim(),
                            )
                        }
                    },
                    icon = AppIcons.check(),
                    container = colors.brand,
                    contentColor = colors.onBrand,
                    modifier = Modifier.weight(1.35f),
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

/** One line of the order: number, the name you type, the quantity and a remove cross. */
@Composable
private fun GoodRow(
    index: Int,
    title: String,
    quantity: Int,
    onTitle: (String) -> Unit,
    onQuantity: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceSunken)
            .border(1.dp, colors.border, shape)
            .padding(horizontal = 9.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(50))
                .background(colors.brand),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = TextUtils.toKurdish((index + 1).toString()),
                color = colors.onBrand,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Spacer(Modifier.width(9.dp))
        BasicTextField(
            value = title,
            onValueChange = onTitle,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 14.sp,
                color = colors.textPrimary,
                fontWeight = FontWeight.SemiBold,
            ),
            cursorBrush = SolidColor(colors.brand),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (title.isEmpty()) {
                        Text(
                            text = "ناوی کاڵا بنووسە…",
                            color = colors.textMuted,
                            fontSize = 13.sp,
                        )
                    }
                    inner()
                }
            },
        )
        Spacer(Modifier.width(8.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(50))
                .padding(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepCircle(AppIcons.minus()) { onQuantity(-1) }
            Box(Modifier.width(34.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = TextUtils.toKurdish(quantity.toString()),
                    color = colors.textPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            StepCircle(AppIcons.plus()) { onQuantity(1) }
        }
        Spacer(Modifier.width(2.dp))
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                AppIcons.close(),
                contentDescription = "سڕینەوەی خانە",
                tint = colors.textMuted,
                modifier = Modifier.size(15.dp),
            )
        }
    }
}

@Composable
private fun StepCircle(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier
            .size(30.dp)
            .shadow(5.dp, RoundedCornerShape(50), clip = false, ambientColor = colors.brand.copy(alpha = 0.4f), spotColor = colors.brand.copy(alpha = 0.4f))
            .clip(RoundedCornerShape(50))
            .background(
                Brush.verticalGradient(
                    listOf(lerp(colors.brandSoft, Color.White, 0.6f), colors.brandSoft),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(50))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = colors.brandDeep,
            modifier = Modifier.size(15.dp),
        )
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
            .shadow(
                if (selected) 9.dp else 4.dp,
                shape,
                clip = false,
                ambientColor = if (selected) colors.brand.copy(alpha = 0.5f) else Color(0x1A9A6A3E),
                spotColor = if (selected) colors.brand.copy(alpha = 0.5f) else Color(0x1A9A6A3E),
            )
            .clip(shape)
            .background(
                if (selected) {
                    Brush.verticalGradient(
                        listOf(
                            lerp(colors.brand, Color.White, 0.26f),
                            colors.brand,
                            lerp(colors.brand, Color.Black, 0.16f),
                        ),
                    )
                } else {
                    Brush.verticalGradient(listOf(Color(0xFFFFFFFF), colors.surfaceSunken))
                },
            )
            .border(1.dp, Color.White.copy(alpha = 0.45f), shape)
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

/** The orange dashed «+ زیادکردنی خانەی کاڵا» bar of the reference design. */
@Composable
private fun DashedAddButton(
    label: String,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .drawBehind {
                drawRoundRect(
                    color = colors.brand.copy(alpha = 0.55f),
                    style = Stroke(
                        width = 1.6.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f),
                    ),
                    cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
                )
            }
            .background(colors.brand.copy(alpha = 0.06f))
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            AppIcons.plus(),
            contentDescription = null,
            tint = colors.brand,
            modifier = Modifier.size(17.dp),
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = label,
            color = colors.brandDeep,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

/** One editable line of the order sheet. */
private data class GoodDraft(
    val id: String,
    val title: String,
    val quantity: Int,
    val createdAt: Long,
)

/** Goods grouped under one owner. */
private class GoodGroup(
    val key: String,
    val customerId: String?,
    val name: String,
    val kind: String?,
    val items: List<GoodItem>,
)

private const val NOBODY_KEY = "-"
