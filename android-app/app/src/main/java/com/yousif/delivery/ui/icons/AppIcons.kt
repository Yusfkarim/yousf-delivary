package com.yousif.delivery.ui.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.yousif.delivery.R

/**
 * Hand-drawn 24dp line icons (1.6–1.8dp stroke, round caps) so the whole app
 * shares one visual language instead of mixing stock Material glyphs.
 * Each icon is a plain VectorDrawable, so it scales crisply on every screen
 * density and tints with [androidx.compose.material3.Icon]'s `tint`.
 */
object AppIcons {
    @Composable fun call(): ImageVector = ImageVector.vectorResource(R.drawable.ic_call)
    @Composable fun whatsapp(): ImageVector = ImageVector.vectorResource(R.drawable.ic_whatsapp)
    @Composable fun viber(): ImageVector = ImageVector.vectorResource(R.drawable.ic_viber)
    @Composable fun route(): ImageVector = ImageVector.vectorResource(R.drawable.ic_route)
    @Composable fun navigate(): ImageVector = ImageVector.vectorResource(R.drawable.ic_navigate)
    @Composable fun share(): ImageVector = ImageVector.vectorResource(R.drawable.ic_share)
    @Composable fun edit(): ImageVector = ImageVector.vectorResource(R.drawable.ic_edit)
    @Composable fun delete(): ImageVector = ImageVector.vectorResource(R.drawable.ic_delete)
    @Composable fun copy(): ImageVector = ImageVector.vectorResource(R.drawable.ic_copy)
    @Composable fun cloudDone(): ImageVector = ImageVector.vectorResource(R.drawable.ic_cloud_done)
    @Composable fun cloudWait(): ImageVector = ImageVector.vectorResource(R.drawable.ic_cloud_wait)
    @Composable fun locate(): ImageVector = ImageVector.vectorResource(R.drawable.ic_locate)
    @Composable fun pin(): ImageVector = ImageVector.vectorResource(R.drawable.ic_pin)
    @Composable fun search(): ImageVector = ImageVector.vectorResource(R.drawable.ic_search)
    @Composable fun plus(): ImageVector = ImageVector.vectorResource(R.drawable.ic_plus)
    @Composable fun minus(): ImageVector = ImageVector.vectorResource(R.drawable.ic_minus)
    @Composable fun close(): ImageVector = ImageVector.vectorResource(R.drawable.ic_close)
    @Composable fun check(): ImageVector = ImageVector.vectorResource(R.drawable.ic_check)
    @Composable fun chevronDown(): ImageVector = ImageVector.vectorResource(R.drawable.ic_chevron_down)
    @Composable fun settings(): ImageVector = ImageVector.vectorResource(R.drawable.ic_settings)
    @Composable fun sync(): ImageVector = ImageVector.vectorResource(R.drawable.ic_sync)
    @Composable fun lock(): ImageVector = ImageVector.vectorResource(R.drawable.ic_lock)
    @Composable fun map(): ImageVector = ImageVector.vectorResource(R.drawable.ic_map)
    @Composable fun more(): ImageVector = ImageVector.vectorResource(R.drawable.ic_more)
    @Composable fun user(): ImageVector = ImageVector.vectorResource(R.drawable.ic_user)
    @Composable fun list(): ImageVector = ImageVector.vectorResource(R.drawable.ic_list)
    @Composable fun bell(): ImageVector = ImageVector.vectorResource(R.drawable.ic_bell)
    @Composable fun clock(): ImageVector = ImageVector.vectorResource(R.drawable.ic_clock)
    @Composable fun store(): ImageVector = ImageVector.vectorResource(R.drawable.ic_store)
    @Composable fun wallet(): ImageVector = ImageVector.vectorResource(R.drawable.ic_wallet)
    @Composable fun checkCircle(): ImageVector = ImageVector.vectorResource(R.drawable.ic_check_circle)
    @Composable fun chevronEnd(): ImageVector = ImageVector.vectorResource(R.drawable.ic_chevron_end)

    @Composable fun chevronStart(): ImageVector = ImageVector.vectorResource(R.drawable.ic_chevron_start)
    @Composable fun moon(): ImageVector = ImageVector.vectorResource(R.drawable.ic_moon)
    @Composable fun sun(): ImageVector = ImageVector.vectorResource(R.drawable.ic_sun)
    @Composable fun packageBox(): ImageVector = ImageVector.vectorResource(R.drawable.ic_package)

    @Composable fun fit(): ImageVector = ImageVector.vectorResource(R.drawable.ic_fit)

    @Composable fun download(): ImageVector = ImageVector.vectorResource(R.drawable.ic_download)

    @Composable fun database(): ImageVector = ImageVector.vectorResource(R.drawable.ic_database)
}
