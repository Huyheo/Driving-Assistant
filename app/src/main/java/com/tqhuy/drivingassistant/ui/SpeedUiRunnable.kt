package com.tqhuy.drivingassistant.ui

import android.util.Log
import android.widget.TextView

class SpeedUiRunnable : Runnable {
    var speedTextView: TextView? = null
    var speedVal = 0.0

    constructor() {
        speedVal = 0.0
    }

    constructor(textView: TextView?) {
        speedTextView = textView
    }

    override fun run() {
        try {
            speedTextView!!.text = String.format("%s km/hr", speedVal)
        } catch (e: Exception) {
            Log.e(TAG, "run: ", e)
        }
    }

    companion object {
        private const val TAG = "SpeedUiRunnable"
    }
}