package com.tqhuy.drivingassistant.ui

import android.util.Log
import android.widget.ImageView
import com.tqhuy.drivingassistant.R

class SignUiRunnable : Runnable {
    var signVal = 0
    private var signImageView: ImageView? = null

    constructor() {
        signVal = 0
    }

    constructor(signVal: Int, imageView: ImageView?) {
        this.signVal = signVal
        setSignImageView(imageView)
    }

    override fun run() {
        try {
            when (signVal) {
                10 -> {
                    signImageView!!.setImageResource(R.drawable.sign10)
                    Log.i(TAG, "run: -------------------- SET 10 --------------------")
                }
                20 -> {
                    signImageView!!.setImageResource(R.drawable.sign20)
                    Log.i(TAG, "run: -------------------- SET 20 --------------------")
                }
                30 -> {
                    signImageView!!.setImageResource(R.drawable.sign30)
                    Log.i(TAG, "run: -------------------- SET 30 --------------------")
                }
                40 -> {
                    signImageView!!.setImageResource(R.drawable.sign40)
                    Log.i(TAG, "run: -------------------- SET 40 --------------------")
                }
                50 -> {
                    signImageView!!.setImageResource(R.drawable.sign50)
                    Log.i(TAG, "run: -------------------- SET 50 --------------------")
                }
                60 -> {
                    signImageView!!.setImageResource(R.drawable.sign60)
                    Log.i(TAG, "run: -------------------- SET 60 --------------------")
                }
                70 -> {
                    signImageView!!.setImageResource(R.drawable.sign70)
                    Log.i(TAG, "run: -------------------- SET 70 --------------------")
                }
                80 -> {
                    signImageView!!.setImageResource(R.drawable.sign80)
                    Log.i(TAG, "run: -------------------- SET 80 --------------------")
                }
                90 -> {
                    signImageView!!.setImageResource(R.drawable.sign90)
                    Log.i(TAG, "run: -------------------- SET 90 --------------------")
                }
                100 -> {
                    signImageView!!.setImageResource(R.drawable.sign100)
                    Log.i(TAG, "run: -------------------- SET 100 --------------------")
                }
                110 -> {
                    signImageView!!.setImageResource(R.drawable.sign110)
                    Log.i(TAG, "run: -------------------- SET 110 --------------------")
                }
                120 -> {
                    signImageView!!.setImageResource(R.drawable.sign120)
                    Log.i(TAG, "run: -------------------- SET 120 --------------------")
                }
                else ->
                    Log.i(TAG, "run: -------------------- SET NONE --------------------")
            }
        } catch (e: Exception) {
            Log.e(TAG, "run: Cannot set image. ", e)
        }
    }

    fun setSignImageView(signImageView: ImageView?) {
        this.signImageView = signImageView
    }

    companion object {
        private const val TAG = "UiRunnableClass"
    }
}