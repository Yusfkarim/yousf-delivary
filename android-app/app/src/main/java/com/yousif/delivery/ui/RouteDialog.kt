package com.yousif.delivery.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yousif.delivery.data.Customer
import com.yousif.delivery.ui.theme.AppSurface
import com.yousif.delivery.ui.theme.AppTextPrimary
import com.yousif.delivery.ui.theme.AppTextSecondary
import com.yousif.delivery.ui.theme.MetallicGold
import com.yousif.delivery.util.GeoUtils
import com.yousif.delivery.util.IntentUtils
import com.yousif.delivery.util.TextUtils
import com.yousif.delivery.viewmodel.LocationFix

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
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            shape = RoundedCornerShape(24.dp),
            color = AppSurface,
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = customer.name,
                            color = AppTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        )
                        Text(
                            text = TextUtils.distance(distance),
                            color = MetallicGold,
                            fontSize = 12.sp,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "داخستن", tint = AppTextSecondary)
                    }
                }

                Spacer(Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(18.dp)),
                ) {
                    OsmMap(
                        modifier = Modifier.fillMaxSize(),
                        center = if (customer.hasCoordinates) {
                            MapPoint(customer.latitude, customer.longitude)
                        } else if (myLocation != null) {
                            MapPoint(myLocation.latitude, myLocation.longitude)
                        } else {
                            null
                        },
                        zoom = 15.5,
                        me = myLocation?.let { MapPoint(it.latitude, it.longitude, "شوێنی من") },
                        customer = if (customer.hasCoordinates) {
                            MapPoint(customer.latitude, customer.longitude, customer.name)
                        } else {
                            null
                        },
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (customer.addressNote.isBlank()) {
                            TextUtils.coordinates(customer.latitude, customer.longitude)
                        } else {
                            customer.addressNote
                        },
                        color = AppTextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = {
                        IntentUtils.copyToClipboard(
                            context,
                            "coordinates",
                            TextUtils.coordinates(customer.latitude, customer.longitude),
                        )
                        IntentUtils.toast(context, "کۆردینات کۆپی کرا")
                    }) {
                        Icon(
                            Icons.Filled.ContentCopy,
                            contentDescription = "کۆپیکردنی کۆردینات",
                            tint = AppTextSecondary,
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Button(
                    onClick = {
                        IntentUtils.googleNavigation(context, customer.latitude, customer.longitude)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("دەستپێکردنی ڕێگا (Google Maps)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(Modifier.height(6.dp))

                OutlinedButton(
                    onClick = {
                        IntentUtils.dial(context, customer.primaryPhone)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("پەیوەندی بە کڕیار")
                }
            }
        }
    }
}
