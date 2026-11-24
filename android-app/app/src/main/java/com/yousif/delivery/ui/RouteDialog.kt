package com.yousif.delivery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yousif.delivery.data.Customer
import com.yousif.delivery.ui.icons.AppIcons
import com.yousif.delivery.ui.theme.AppSuccess
import com.yousif.delivery.ui.theme.AppSurface
import com.yousif.delivery.ui.theme.AppSurfaceHigh
import com.yousif.delivery.ui.theme.AppTextPrimary
import com.yousif.delivery.ui.theme.AppTextSecondary
import com.yousif.delivery.ui.theme.MetallicGold
import com.yousif.delivery.ui.theme.NeonCyan
import com.yousif.delivery.util.GeoUtils
import com.yousif.delivery.util.IntentUtils
import com.yousif.delivery.util.TextUtils
import com.yousif.delivery.viewmodel.LocationFix

/** Bottom-sheet style route preview: big live map on top, actions underneath. */
@Composable
fun RouteDialog(
    customer: Customer,
    myLocation: LocationFix?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    val distance = if (myLocation != null && customer.hasCoordinates) {
        GeoUtils.distanceMeters(
            myLocation.latitude,
            myLocation.longitude,
            customer.latitude,
            customer.longitude,
        )
    } else {
        null
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = AppSurface,
            ) {
                Column {
                    /* ---------- map hero ---------- */
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(330.dp),
                    ) {
                        OsmMap(
                            modifier = Modifier.fillMaxSize(),
                            center = when {
                                customer.hasCoordinates -> MapPoint(customer.latitude, customer.longitude)
                                myLocation != null -> MapPoint(myLocation.latitude, myLocation.longitude)
                                else -> null
                            },
                            zoom = 15.5,
                            me = myLocation?.let { MapPoint(it.latitude, it.longitude, "شوێنی من") },
                            customer = if (customer.hasCoordinates) {
                                MapPoint(customer.latitude, customer.longitude, customer.name)
                            } else {
                                null
                            },
                        )

                        // close
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xF2101725))
                                .border(1.dp, Color(0x33FFFFFF), CircleShape)
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                AppIcons.close(),
                                contentDescription = "داخستن",
                                tint = AppTextPrimary,
                                modifier = Modifier.size(17.dp),
                            )
                        }

                        // distance pill
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 12.dp, bottom = 12.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xF2101725))
                                .border(1.dp, MetallicGold.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                AppIcons.navigate(),
                                contentDescription = null,
                                tint = MetallicGold,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = TextUtils.distance(distance),
                                color = MetallicGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    /* ---------- details + actions ---------- */
                    Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(NeonCyan.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = TextUtils.initials(customer.name),
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                )
                            }
                            Spacer(Modifier.width(11.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = customer.name,
                                    color = AppTextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = com.yousif.delivery.util.PhoneUtils.pretty(customer.primaryPhone),
                                    color = AppTextSecondary,
                                    fontSize = 12.sp,
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(AppSurfaceHigh)
                                    .clickable {
                                        IntentUtils.copyToClipboard(
                                            context,
                                            "coordinates",
                                            TextUtils.coordinates(customer.latitude, customer.longitude),
                                        )
                                        IntentUtils.toast(context, "کۆردینات کۆپی کرا")
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    AppIcons.copy(),
                                    contentDescription = "کۆپیکردنی کۆردینات",
                                    tint = AppTextSecondary,
                                    modifier = Modifier.size(17.dp),
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(AppSurfaceHigh.copy(alpha = 0.55f))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                AppIcons.pin(),
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = customer.addressNote.ifBlank { "بێ ناونیشان" },
                                    color = AppTextPrimary.copy(alpha = 0.9f),
                                    fontSize = 12.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = TextUtils.coordinates(customer.latitude, customer.longitude),
                                    color = AppTextSecondary,
                                    fontSize = 10.5.sp,
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        Button(
                            onClick = {
                                IntentUtils.googleNavigation(context, customer.latitude, customer.longitude)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(17.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = Color(0xFF04121B),
                            ),
                        ) {
                            Icon(
                                AppIcons.navigate(),
                                contentDescription = null,
                                modifier = Modifier.size(19.dp),
                            )
                            Spacer(Modifier.width(9.dp))
                            Text(
                                "دەستپێکردنی ڕێگا",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                            )
                        }

                        Spacer(Modifier.height(9.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            SecondaryAction(
                                label = "پەیوەندی",
                                icon = AppIcons.call(),
                                tint = AppSuccess,
                                modifier = Modifier.weight(1f),
                            ) { IntentUtils.dial(context, customer.primaryPhone) }

                            SecondaryAction(
                                label = "واتسئەپ",
                                icon = AppIcons.whatsapp(),
                                tint = AppSuccess,
                                modifier = Modifier.weight(1f),
                            ) { IntentUtils.whatsapp(context, customer.primaryPhone) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SecondaryAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .background(tint.copy(alpha = 0.11f))
            .border(1.dp, tint.copy(alpha = 0.25f), shape)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(7.dp))
        Text(label, color = tint, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
    }
}
