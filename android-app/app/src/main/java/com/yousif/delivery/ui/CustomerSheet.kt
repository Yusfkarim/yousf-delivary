package com.yousif.delivery.ui

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yousif.delivery.data.Customer
import com.yousif.delivery.data.CustomerDraft
import com.yousif.delivery.data.ExtraPhone
import com.yousif.delivery.ui.icons.AppIcons
import com.yousif.delivery.ui.theme.LocalAppColors
import com.yousif.delivery.util.IntentUtils
import com.yousif.delivery.util.MapLinkParser
import com.yousif.delivery.util.TextUtils
import com.yousif.delivery.viewmodel.LocationFix

/** Mutable row state for the "extra numbers" editor. */
class ExtraPhoneEditor(
    number: String = "",
    hasWhatsapp: Boolean = false,
    hasViber: Boolean = false,
) {
    var number by mutableStateOf(number)
    var hasWhatsapp by mutableStateOf(hasWhatsapp)
    var hasViber by mutableStateOf(hasViber)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerSheet(
    initial: Customer?,
    myLocation: LocationFix?,
    onRequestGps: ((Double, Double) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onSave: (CustomerDraft) -> Unit,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf(initial?.name ?: "") }
    var primaryPhone by remember { mutableStateOf(initial?.primaryPhone ?: "") }
    var hasWhatsapp by remember { mutableStateOf(initial?.hasWhatsapp ?: false) }
    var hasViber by remember { mutableStateOf(initial?.hasViber ?: false) }
    var addressNote by remember { mutableStateOf(initial?.addressNote ?: "") }
    var latitude by remember { mutableStateOf(initial?.latitude?.takeIf { it != 0.0 }) }
    var longitude by remember { mutableStateOf(initial?.longitude?.takeIf { it != 0.0 }) }
    var pasteField by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val extras = remember {
        mutableStateListOf<ExtraPhoneEditor>().apply {
            initial?.extraPhones?.forEach { add(ExtraPhoneEditor(it.number, it.hasWhatsapp, it.hasViber)) }
        }
    }

    val initialCenter = remember {
        val lat = initial?.latitude?.takeIf { it != 0.0 }
        val lng = initial?.longitude?.takeIf { it != 0.0 }
        when {
            lat != null && lng != null -> MapPoint(lat, lng)
            myLocation != null -> MapPoint(myLocation.latitude, myLocation.longitude)
            else -> null
        }
    }

    fun applyCoordinates(lat: Double, lng: Double) {
        latitude = lat
        longitude = lng
        error = null
    }

    fun parsePaste() {
        val fromField = MapLinkParser.parse(pasteField)
        val fromClipboard = readClipboardCoordinates(context)
        val parsed = fromField ?: fromClipboard
        if (parsed != null) {
            applyCoordinates(parsed.first, parsed.second)
            pasteField = TextUtils.coordinates(parsed.first, parsed.second)
        } else {
            error = "نەتوانرا شوێن لە دەقەکە بدۆزرێتەوە"
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
                BrandBadge(size = 40.dp, icon = if (initial == null) AppIcons.plus() else AppIcons.edit())
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (initial == null) "زیادکردنی کڕیاری نوێ" else "دەستکاری کڕیار",
                        color = colors.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "ناو، ژمارە و شوێن پێویستن",
                        color = colors.textSecondary,
                        fontSize = 11.5.sp,
                    )
                }
            }

            SectionHeader(text = "زانیاری سەرەکی")

            AppCard(padding = 14.dp) {
                SheetField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "ناوی کڕیار",
                    icon = AppIcons.user(),
                )
                Spacer(Modifier.height(10.dp))
                SheetField(
                    value = primaryPhone,
                    onValueChange = { primaryPhone = it },
                    placeholder = "ژمارەی سەرەکی (07xx xxx xxxx)",
                    icon = AppIcons.call(),
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TogglePill(
                        label = "واتسئەپ",
                        selected = hasWhatsapp,
                        onClick = { hasWhatsapp = !hasWhatsapp },
                        modifier = Modifier.weight(1f),
                    )
                    TogglePill(
                        label = "ڤایبەر",
                        selected = hasViber,
                        onClick = { hasViber = !hasViber },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            extras.forEachIndexed { index, editor ->
                ExtraPhoneRow(editor = editor, onRemove = { extras.removeAt(index) })
            }

            TextButton(onClick = { extras.add(ExtraPhoneEditor()) }) {
                Icon(
                    AppIcons.plus(),
                    contentDescription = null,
                    tint = colors.brandDeep,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text("زیادکردنی ژمارەی تر", color = colors.brandDeep, fontSize = 12.sp)
            }

            SectionHeader(text = "شوێنی کڕیار")

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(215.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, colors.border, RoundedCornerShape(20.dp)),
            ) {
                OsmMap(
                    modifier = Modifier.fillMaxSize(),
                    center = initialCenter,
                    zoom = 15.0,
                    me = myLocation?.let { MapPoint(it.latitude, it.longitude, "شوێنی من") },
                    customer = if (latitude != null && longitude != null) {
                        MapPoint(latitude ?: 0.0, longitude ?: 0.0, "شوێنی کڕیار")
                    } else {
                        null
                    },
                    onTap = { lat, lng -> applyCoordinates(lat, lng) },
                )
            }

            Text(
                text = "لە نەخشەکە دەست بنێ بۆ دیاریکردنی شوێن",
                color = colors.textMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 6.dp, bottom = 10.dp),
            )

            AppCard(padding = 14.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillButton(
                        label = "شوێنی ئێستام",
                        onClick = { onRequestGps { lat, lng -> applyCoordinates(lat, lng) } },
                        icon = AppIcons.locate(),
                        container = colors.brandSoft,
                        contentColor = colors.brandDeep,
                        modifier = Modifier.weight(1f),
                    )
                    if (latitude != null && longitude != null) {
                        PillButton(
                            label = "کۆپی",
                            onClick = {
                                IntentUtils.copyToClipboard(
                                    context,
                                    "coordinates",
                                    TextUtils.coordinates(latitude ?: 0.0, longitude ?: 0.0),
                                )
                                IntentUtils.toast(context, "کۆردینات کۆپی کرا")
                            },
                            icon = AppIcons.copy(),
                            container = colors.surfaceAlt,
                            contentColor = colors.textSecondary,
                            modifier = Modifier.width(110.dp),
                        )
                    }
                }
                if (latitude != null && longitude != null) {
                    Text(
                        text = TextUtils.coordinates(latitude ?: 0.0, longitude ?: 0.0),
                        color = colors.textMuted,
                        fontSize = 10.5.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                SheetField(
                    value = pasteField,
                    onValueChange = { pasteField = it },
                    placeholder = "لینکی گووگڵ ماپ یان کۆردینات",
                    icon = AppIcons.map(),
                    trailing = {
                        TextButton(onClick = { parsePaste() }) {
                            Text("جێبەجێکردن", fontSize = 11.sp, color = colors.brandDeep)
                        }
                    },
                )
                Spacer(Modifier.height(10.dp))
                SheetField(
                    value = addressNote,
                    onValueChange = { addressNote = it },
                    placeholder = "ناونیشان / تێبینی کورت",
                    icon = AppIcons.pin(),
                )
            }

            error?.let { message ->
                Text(
                    text = message,
                    color = colors.danger,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .background(colors.dangerSoft, RoundedCornerShape(12.dp))
                        .padding(10.dp),
                )
            }

            Spacer(Modifier.height(16.dp))

            PillButton(
                label = if (initial == null) "زیادکردنی کڕیار" else "پاشەکەوتکردنی گۆڕانکاری",
                onClick = {
                    when {
                        name.isBlank() -> error = "ناوی کڕیار پێویستە"
                        primaryPhone.isBlank() -> error = "ژمارەی سەرەکی پێویستە"
                        latitude == null || longitude == null -> error = "شوێن دیاری بکە (GPS یان نەخشە)"
                        else -> {
                            error = null
                            onSave(
                                CustomerDraft(
                                    id = initial?.id,
                                    name = name,
                                    primaryPhone = primaryPhone,
                                    hasWhatsapp = hasWhatsapp,
                                    hasViber = hasViber,
                                    extraPhones = extras
                                        .filter { it.number.isNotBlank() }
                                        .map { ExtraPhone(it.number.trim(), it.hasWhatsapp, it.hasViber) },
                                    latitude = latitude,
                                    longitude = longitude,
                                    addressNote = addressNote,
                                ),
                            )
                        }
                    }
                },
                icon = AppIcons.check(),
                container = colors.brand,
                contentColor = colors.onBrand,
                modifier = Modifier.fillMaxWidth(),
                height = 54.dp,
                fontSize = 14,
            )
        }
    }
}

@Composable
private fun SheetField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = LocalAppColors.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text(placeholder, fontSize = 12.5.sp, color = colors.textMuted) },
        leadingIcon = {
            Icon(icon, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(17.dp))
        },
        trailingIcon = trailing,
        shape = RoundedCornerShape(16.dp),
        textStyle = TextStyle(fontSize = 13.5.sp, color = colors.textPrimary, fontWeight = FontWeight.Medium),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = colors.surfaceSunken,
            unfocusedContainerColor = colors.surfaceSunken,
            focusedBorderColor = colors.brand.copy(alpha = 0.6f),
            unfocusedBorderColor = colors.border,
            cursorColor = colors.brand,
        ),
    )
}

@Composable
private fun TogglePill(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .height(42.dp)
            .clip(shape)
            .background(if (selected) colors.brandSoft else colors.surfaceSunken)
            .border(1.dp, if (selected) colors.brand.copy(alpha = 0.4f) else colors.border, shape)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (label == "واتسئەپ") AppIcons.whatsapp() else AppIcons.viber(),
            contentDescription = null,
            tint = if (selected) colors.brandDeep else colors.textMuted,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            color = if (selected) colors.brandDeep else colors.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
        if (selected) {
            Spacer(Modifier.width(5.dp))
            Icon(
                AppIcons.check(),
                contentDescription = null,
                tint = colors.brandDeep,
                modifier = Modifier.size(13.dp),
            )
        }
    }
}

@Composable
private fun ExtraPhoneRow(editor: ExtraPhoneEditor, onRemove: () -> Unit) {
    val colors = LocalAppColors.current
    Column(Modifier.padding(top = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                SheetField(
                    value = editor.number,
                    onValueChange = { editor.number = it },
                    placeholder = "ژمارەی تر",
                    icon = AppIcons.call(),
                )
            }
            IconButton(onClick = onRemove) {
                Icon(AppIcons.delete(), contentDescription = "سڕینەوە", tint = colors.danger)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TogglePill(
                label = "واتسئەپ",
                selected = editor.hasWhatsapp,
                onClick = { editor.hasWhatsapp = !editor.hasWhatsapp },
                modifier = Modifier.weight(1f),
            )
            TogglePill(
                label = "ڤایبەر",
                selected = editor.hasViber,
                onClick = { editor.hasViber = !editor.hasViber },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun readClipboardCoordinates(context: Context): Pair<Double, Double>? {
    return try {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val text = manager?.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)
        if (text.isNullOrBlank()) null else MapLinkParser.parse(text.toString())
    } catch (error: Exception) {
        null
    }
}
