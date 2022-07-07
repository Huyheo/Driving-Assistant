package com.tqhuy.drivingassistant

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.annotation.Nullable
import androidx.core.app.ActivityCompat
import kotlin.math.floor
import kotlin.math.roundToInt

class LocationService : Service() {
    private var locationManager: LocationManager? = null

    inner class LocationListener internal constructor(provider: String) :
        android.location.LocationListener {
        private var lastLocation: Location
        private var speed = 0.0
        private var distance = 0.0
        private var curTime: Long = 0
        private var prevTime: Long = 0
        override fun onLocationChanged(location: Location) {
            curTime = System.currentTimeMillis()
            /* distance : KM  ---  timeDiff : secs  ---  speed : KM/HR */
            distance = (location.distanceTo(lastLocation) / 1000).toDouble()
            var timeDiff = ((curTime - prevTime) / 1000.0f).toDouble()
            timeDiff = (timeDiff * 100.0).roundToInt() / 100.0
            Log.i(TAG, "onLocationChanged: TIMEDIFF: $timeDiff   DIST: $distance")
            speed = floor(distance / timeDiff * 3600 * 10) / 10
            lastLocation.set(location)
            prevTime = System.currentTimeMillis()
            lastLocation.set(location)
            val broadcastIntent = Intent()
            broadcastIntent.putExtra("speed", speed)
            broadcastIntent.action = "com.tqhuy.drivingassistant.UPDATE_SPEED"
            broadcastIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            sendBroadcast(broadcastIntent)
        }

        override fun onProviderDisabled(provider: String) {
            Log.e(TAG, "onProviderDisabled: $provider")
        }

        override fun onProviderEnabled(provider: String) {
            Log.e(TAG, "onProviderEnabled: $provider")
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
            Log.e(TAG, "onStatusChanged: $provider")
        }

        init {
            Log.e(TAG, "LocationListener $provider")
            lastLocation = Location(provider)
        }
    }

    private var locationListeners = arrayOf(
        LocationListener(LocationManager.GPS_PROVIDER),
        LocationListener(LocationManager.NETWORK_PROVIDER)
    )

    @Nullable
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.e(TAG, "onStartCommand")
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onCreate() {
        Log.e(TAG, "onCreate")
        initializeLocationManager()
        try {
            locationManager!!.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                LOCATION_INTERVAL.toLong(),
                LOCATION_DISTANCE.toFloat(),
                locationListeners[0]
            )
        } catch (ex: SecurityException) {
            Log.i(TAG, "fail to request location update, ignore", ex)
        } catch (ex: IllegalArgumentException) {
            Log.d(TAG, "network provider does not exist, " + ex.message)
        }
    }

    override fun onDestroy() {
        Log.e(TAG, "onDestroy")
        super.onDestroy()
        if (locationManager != null) {
            for (mLocationListener in locationListeners) {
                try {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return
                    }
                    locationManager!!.removeUpdates(mLocationListener)
                } catch (ex: Exception) {
                    Log.i(TAG, "fail to remove location listener, ignore", ex)
                }
            }
        }
    }

    private fun initializeLocationManager() {
        Log.e(TAG, "initializeLocationManager - LOCATION_INTERVAL: $LOCATION_INTERVAL LOCATION_DISTANCE: $LOCATION_DISTANCE")
        if (locationManager == null) {
            locationManager =
                applicationContext.getSystemService(LOCATION_SERVICE) as LocationManager
        }
    }

    companion object {
        private const val TAG = "LocationService"
        private const val LOCATION_INTERVAL = 1500
        private const val LOCATION_DISTANCE = 0
    }
}
