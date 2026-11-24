package com.yousif.delivery.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

/**
 * Hardware GPS through FusedLocationProviderClient with a platform
 * LocationManager fallback (works on devices without Google Play Services).
 *
 * Callers must hold ACCESS_FINE_LOCATION before calling [start].
 */
class LocationProvider(private val context: Context) {

    private val fusedClient: FusedLocationProviderClient? = try {
        LocationServices.getFusedLocationProviderClient(context)
    } catch (error: Throwable) {
        null
    }

    private val locationManager: LocationManager? =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    /** Instant fix from the OS cache, if any. */
    @SuppressLint("MissingPermission")
    fun lastKnown(): Pair<Double, Double>? {
        val manager = locationManager ?: return null
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        )
        for (provider in providers) {
            val location: Location? = try {
                if (manager.isProviderEnabled(provider)) manager.getLastKnownLocation(provider) else null
            } catch (error: SecurityException) {
                return null
            } catch (error: IllegalArgumentException) {
                null
            }
            if (location != null) return location.latitude to location.longitude
        }
        return null
    }

    /** Starts live updates; the returned lambda stops them. */
    @SuppressLint("MissingPermission")
    fun start(onUpdate: (Double, Double) -> Unit): () -> Unit {
        var stopped = false

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                if (!stopped) onUpdate(location.latitude, location.longitude)
            }
        }

        val fused = fusedClient
        if (fused != null) {
            try {
                val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000L)
                    .setMinUpdateIntervalMillis(2_000L)
                    .setMinUpdateDistanceMeters(2f)
                    .build()
                fused.requestLocationUpdates(request, callback, Looper.getMainLooper())
                return {
                    stopped = true
                    try {
                        fused.removeLocationUpdates(callback)
                    } catch (error: Exception) {
                        // ignore
                    }
                }
            } catch (error: SecurityException) {
                return { stopped = true }
            } catch (error: Exception) {
                // fall through to the platform provider
            }
        }

        val manager = locationManager
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (!stopped) onUpdate(location.latitude, location.longitude)
            }

            override fun onProviderEnabled(provider: String) {}

            override fun onProviderDisabled(provider: String) {}
        }

        if (manager != null) {
            try {
                if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    manager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, 5_000L, 3f, listener, Looper.getMainLooper(),
                    )
                }
                try {
                    manager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER, 8_000L, 10f, listener, Looper.getMainLooper(),
                    )
                } catch (error: Exception) {
                    // network provider not available
                }
            } catch (error: SecurityException) {
                // permission revoked
            } catch (error: Exception) {
                // provider missing
            }
        }

        return {
            stopped = true
            try {
                manager?.removeUpdates(listener)
            } catch (error: Exception) {
                // ignore
            }
        }
    }
}
