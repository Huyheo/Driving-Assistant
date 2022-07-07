package com.tqhuy.drivingassistant

import android.annotation.SuppressLint
import android.app.Activity
import android.content.SharedPreferences
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import com.tqhuy.drivingassistant.ui.ScreenInterface

@SuppressLint("CommitPrefEdits")
class ResolutionSettingsActivity : Activity(), ScreenInterface {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var radioGroup: RadioGroup
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resolution_settings)
        sharedPreferences = getSharedPreferences("Prefs", MODE_PRIVATE)
        editor = sharedPreferences.edit()
        editor.apply()
        radioGroup = findViewById<View>(R.id.radio_group) as RadioGroup
        supportedResolutions
        radioGroup.setOnCheckedChangeListener { _, checkedId -> // checkedId is the RadioButton selected
            val rb = findViewById<View>(checkedId) as RadioButton
            val resolution = rb.text.toString()
            val resList = resolution.split(" x ").toTypedArray()
            editor.putInt("res_width", resList[0].toInt())
            editor.putInt("res_height", resList[1].toInt())
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

    private val supportedResolutions: Unit
        @SuppressLint("SetTextI18n") get() {
            val manager = getSystemService(CAMERA_SERVICE) as CameraManager
            val sharedPreferences = getSharedPreferences("Prefs", MODE_PRIVATE)
            var imgHeight: Int
            var imgWidth: Int
            try {
                val cameraId = manager.cameraIdList[0]
                val characteristics = manager.getCameraCharacteristics(cameraId)
                val map =
                    characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
                for (size in map.getOutputSizes(SurfaceTexture::class.java)) {
                    imgHeight = size.height
                    imgWidth = size.width
                    if (imgWidth <= 1920) {
                        val radioButton = RadioButton(this)
                        radioButton.id = View.generateViewId()
                        radioButton.setTextColor(getColor(R.color.colorWhite))
                        radioButton.layoutParams =
                            RadioGroup.LayoutParams(
                                RadioGroup.LayoutParams.MATCH_PARENT,
                                RadioGroup.LayoutParams.WRAP_CONTENT
                            )
                        radioButton.text = "$imgWidth x $imgHeight"
                        if (sharedPreferences.getInt(
                                "res_height",
                                0
                            ) == imgHeight && sharedPreferences.getInt("res_width", 0) == imgWidth
                        ) {
                            radioButton.isChecked = true
                        }
                        radioGroup.addView(radioButton)
                    }
                }
            } catch (error: Error) {
                Log.e(TAG, "onCreate: ", error)
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
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

    companion object {
        private const val TAG = "ResolutionSettingsAct"
    }
}
