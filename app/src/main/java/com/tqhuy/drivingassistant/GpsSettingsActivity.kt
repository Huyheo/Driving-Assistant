package com.tqhuy.drivingassistant

import android.annotation.SuppressLint
import android.app.Activity
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.widget.Switch
import com.tqhuy.drivingassistant.ui.ScreenInterface

@SuppressLint("CommitPrefEdits")
class GpsSettingsActivity : Activity(), ScreenInterface {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var gpsSwitch: Switch
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gps_settings)
        sharedPreferences = getSharedPreferences("Prefs", MODE_PRIVATE)
        editor = sharedPreferences.edit()
        gpsSwitch = findViewById<View>(R.id.gps_switch) as Switch
        val status = sharedPreferences.getBoolean("gps_enabled", false)
        gpsSwitch.isChecked = status
        gpsSwitch.setOnCheckedChangeListener { _, _ ->
            if (status) {
                editor.putBoolean("gps_enabled", false)
            } else {
                editor.putBoolean("gps_enabled", true)
            }
            editor.apply()
        }
        val dm = DisplayMetrics()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val display = display
            display?.getRealMetrics(dm)
        } else {
            @Suppress("DEPRECATION")
            val display = windowManager.defaultDisplay
            @Suppress("DEPRECATION")
            display.getMetrics(dm)
        }
        val width = dm.widthPixels
        val height = dm.heightPixels
        window.setLayout((width * 0.95).toInt(), (height * .9).toInt())
    }

    override fun onResume() {
        super.onResume()
        setFullscreen()
    }

    fun closeActivity(v: View?) {
        finish()
    }

    override fun setFullscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }
}