package com.yousif.delivery.ui

import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yousif.delivery.ui.icons.AppIcons
import com.yousif.delivery.data.Customer
import com.yousif.delivery.data.CustomerDraft
import com.yousif.delivery.data.ExtraPhone
import com.yousif.delivery.ui.theme.AppDanger
import com.yousif.delivery.ui.theme.AppSurface
import com.yousif.delivery.ui.theme.AppTextPrimary
import com.yousif.delivery.ui.theme.AppTextSecondary
import com.yousif.delivery.ui.theme.NeonCyan
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
        containerColor = AppSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = if (initial == null) "زیادکردنی کڕیاری نوێ" else "دەستکاری کڕیار",
                color = AppTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("ناوی کڕیار") },
                shape = RoundedCornerShape(14.dp),
            )
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = primaryPhone,
                onValueChange = { primaryPhone = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("ژمارەی سەرەکی") },
                placeholder = { Text("07xx xxx xxxx", fontSize = 12.sp) },
                shape = RoundedCornerShape(14.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = hasWhatsapp,
                    onClick = { hasWhatsapp = !hasWhatsapp },
                    label = { Text("واتسئەپ", fontSize = 12.sp) },
                )
                FilterChip(
                    selected = hasViber,
                    onClick = { hasViber = !hasViber },
                    label = { Text("ڤایبەر", fontSize = 12.sp) },
                )
            }

            Spacer(Modifier.height(6.dp))

            extras.forEachIndexed { index, editor ->
                ExtraPhoneRow(
                    editor = editor,
                    onRemove = { extras.removeAt(index) },
                )
            }

            TextButton(onClick = { extras.add(ExtraPhoneEditor()) }) {
                Icon(
                    AppIcons.plus(),
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text("زیادکردنی ژمارەی تر", color = NeonCyan, fontSize = 12.sp)
            }

            SectionLabel("شوێنی کڕیار")

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onRequestGps { lat, lng -> applyCoordinates(lat, lng) } },
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(AppIcons.pin(), contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("شوێنی ئێستام (GPS)", fontSize = 12.sp)
                }
                if (latitude != null && longitude != null) {
                    TextButton(onClick = {
                        IntentUtils.copyToClipboard(
                            context,
                            "coordinates",
                            TextUtils.coordinates(latitude ?: 0.0, longitude ?: 0.0),
                        )
                        IntentUtils.toast(context, "کۆردینات کۆپی کرا")
                    }) {
                        Icon(
                            AppIcons.copy(),
                            contentDescription = null,
                            tint = AppTextSecondary,
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(TextUtils.coordinates(latitude ?: 0.0, longitude ?: 0.0), fontSize = 11.sp)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(18.dp)),
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
                color = AppTextSecondary,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 6.dp),
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = pasteField,
                onValueChange = { pasteField = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("لینکی گووگڵ ماپ یان کۆردینات") },
                placeholder = { Text("37.1234, 45.1234", fontSize = 12.sp) },
                shape = RoundedCornerShape(14.dp),
                trailingIcon = {
                    TextButton(onClick = { parsePaste() }) { Text("جێبەجێکردن", fontSize = 11.sp) }
                },
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = addressNote,
                onValueChange = { addressNote = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("ناونیشان / تێبینی کورت") },
                shape = RoundedCornerShape(14.dp),
            )

            error?.let { message ->
                Text(
                    text = message,
                    color = AppDanger,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Spacer(Modifier.height(14.dp))

            Button(
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("پاشەکەوتکردن", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ExtraPhoneRow(editor: ExtraPhoneEditor, onRemove: () -> Unit) {
    Column(Modifier.padding(top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = editor.number,
                onValueChange = { editor.number = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("ژمارەی تر", fontSize = 12.sp) },
                shape = RoundedCornerShape(14.dp),
            )
            IconButton(onClick = onRemove) {
                Icon(AppIcons.delete(), contentDescription = "سڕینەوە", tint = AppDanger)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = editor.hasWhatsapp,
                onClick = { editor.hasWhatsapp = !editor.hasWhatsapp },
                label = { Text("واتسئەپ", fontSize = 11.sp) },
            )
            FilterChip(
                selected = editor.hasViber,
                onClick = { editor.hasViber = !editor.hasViber },
                label = { Text("ڤایبەر", fontSize = 11.sp) },
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
