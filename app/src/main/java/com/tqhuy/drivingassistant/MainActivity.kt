package com.tqhuy.drivingassistant

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tqhuy.drivingassistant.ui.ScreenInterface
import com.tqhuy.drivingassistant.ui.SignUiRunnable
import com.tqhuy.drivingassistant.ui.SpeedUiRunnable
import org.opencv.android.*
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.util.*
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), CvCameraViewListener2, ScreenInterface,
    OnInitListener {
    var javaCameraView: JavaCameraView? = null
    private lateinit var textRecognizer: TextRecognizer
    private lateinit var signImageView: ImageView
    private lateinit var speedTextView: TextView
    private lateinit var fabSettings: FloatingActionButton
    private lateinit var fabResolutions: FloatingActionButton
    private lateinit var fabGps: FloatingActionButton
    private lateinit var fabSound: FloatingActionButton
    private lateinit var fabOpen: Animation
    private lateinit var fabClose: Animation
    private lateinit var fabRotateCw: Animation
    private lateinit var fabRotateAntiCw: Animation
    private var isOpen = false
    private lateinit var mRgba: Mat
    private lateinit var mGray: Mat
    private lateinit var circles: Mat
    private lateinit var mRed: Mat
    private lateinit var mGreen: Mat
    private lateinit var mBlue: Mat
    private lateinit var mHueHsv: Mat
    private lateinit var mSatHsv: Mat
    private lateinit var mValHsv: Mat
    private lateinit var mHueHls: Mat
    private lateinit var mSatHls: Mat
    private lateinit var mLightHls: Mat
    private lateinit var hsv: Mat
    private lateinit var hls: Mat
    private lateinit var rgba: Mat
    private lateinit var gray: Mat
    private lateinit var mNew: Mat
    private lateinit var mask: Mat
    private lateinit var mEdges: Mat
    private lateinit var laneZoneMat: Mat
    private lateinit var signRegion: Rect
    private lateinit var laneZone: MatOfPoint
    private var darkGreen = Scalar(0.0, 125.0, 0.0)
    private var bm: Bitmap? = null
    private var newSignFlag = false
    private var imgWidth = 0
    private var imgHeight = 0
    private var rows = 0
    private var cols = 0
    private var left = 0
    private var width = 0
    private var top = 0.0
    private var middleX = 0.0
    private var bottomY = 0.0
    private var vehicleCenterX1 = 0.0
    private var vehicleCenterY1 = 0.0
    private var vehicleCenterX2 = 0.0
    private var vehicleCenterY2 = 0.0
    private var laneCenterX = 0.0
    private var laneCenterY = 0.0
    private var signUiRunnable: SignUiRunnable = SignUiRunnable()
    private var speedUiRunnable: SpeedUiRunnable = SpeedUiRunnable()
    private lateinit var ttsSpeed: TextToSpeech
    lateinit var ttsLane: TextToSpeech
    var speedingCount = 0
    var toneGen1 = ToneGenerator(AudioManager.STREAM_MUSIC, 75)
    var toneGen2 = ToneGenerator(AudioManager.STREAM_MUSIC, 75)
    private lateinit var audioManager: AudioManager
    private lateinit var timer: CountDownTimer
    private var isTimerRunning = false
    private var mLoaderCallback: BaseLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                SUCCESS -> {
                    javaCameraView!!.enableView()
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
            super.onManagerConnected(status)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        fabSettings = findViewById(R.id.fab_settings)
        fabResolutions = findViewById(R.id.fab_resolution)
        fabGps = findViewById(R.id.fab_gps)
        fabSound = findViewById(R.id.fab_sound)
        permissions
        setUpCameraServices()
        val filter = IntentFilter("com.tqhuy.drivingassistant.UPDATE_SPEED")
        this.registerReceiver(LocationBroadcastReceiver(), filter)
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        ttsSpeed = TextToSpeech(this, this)
        ttsLane = TextToSpeech(this, this)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        javaCameraView = findViewById(R.id.java_camera_view)
        javaCameraView!!.visibility = SurfaceView.VISIBLE
        javaCameraView!!.setCvCameraViewListener(this)
        javaCameraView!!.setMaxFrameSize(imgWidth, imgHeight)
        speedTextView = findViewById(R.id.speed_text_view)
        signImageView = findViewById(R.id.sign_image_view)
        setViewClickListeners()
        fabOpen = AnimationUtils.loadAnimation(this, R.anim.fab_open)
        fabClose = AnimationUtils.loadAnimation(this, R.anim.fab_close)
        fabRotateCw = AnimationUtils.loadAnimation(this, R.anim.rotate_clockwise)
        fabRotateAntiCw = AnimationUtils.loadAnimation(this, R.anim.rotate_anticlockwise)
        signUiRunnable.setSignImageView(signImageView)
        speedUiRunnable.speedTextView = speedTextView
        val sharedPreferences: SharedPreferences = getSharedPreferences("Prefs", MODE_PRIVATE)
        signUiRunnable.signVal = sharedPreferences.getInt("last_speed", 0)
        Log.i(TAG, "onCreate: ---------------------------------------------" + sharedPreferences.getInt("last_speed", 0))
        signUiRunnable.run()

        // Timer to alert user of lane departure
        timer = object : CountDownTimer(30000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (millisUntilFinished < 27500) {
                    toneGen2.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 250)
                    ttsLane.speak(
                        "Lane departure detected",
                        TextToSpeech.QUEUE_ADD,
                        null,
                        "Lane Departure Detected"
                    )
                }
            }

            override fun onFinish() {
                Log.i(TAG, "onFinish: ---------- TIMER DONE ----------")
            }
        }
        displayMetrics = DisplayMetrics()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val display = display
            display?.getRealMetrics(displayMetrics)
        } else {
            @Suppress("DEPRECATION")
            val display = windowManager.defaultDisplay
            @Suppress("DEPRECATION")
            display.getMetrics(displayMetrics)
        }
    }

    override fun onCameraViewStarted(w: Int, h: Int) {
        rows = h
        cols = w
        left = rows / 8
        width = cols - left
        top = rows / 2.5
        middleX = (w / 2).toDouble()
        bottomY = h * .95
        vehicleCenterX1 = middleX
        vehicleCenterX2 = middleX
        vehicleCenterY1 = bottomY - rows / 7
        vehicleCenterY2 = bottomY - rows / 20
        laneCenterX = 0.0
        laneCenterY = (bottomY - rows / 7 + bottomY - rows / 20) / 2
        initializeAllMats()
    }

    override fun onCameraViewStopped() {
        releaseAllMats()
    }

    private val ksize = Size(5.0, 5.0)
    private val sigma = 3.0
    private val blurPt = Point(3.0, 3.0)

    /******************************************************************************************
     * mRed, mGreen, mBlue, m-_hsv, m-_hls :  Mats of respective channels of ROI
     * mCombined : combined mat of canny edges and mask for yellow and white
     * hsv, hls, rgb : color space mats of ROI
     */
    override fun onCameraFrame(inputFrame: CvCameraViewFrame): Mat {
        mRgba = inputFrame.rgba()
        mGray = inputFrame.gray()
        Imgproc.blur(mGray, mGray, ksize, blurPt)
        Imgproc.GaussianBlur(mRgba, mRgba, ksize, sigma)
        val lines = Mat()
        /* rgbaInnerWindow & mIntermediateMat = ROI Mats */
        val rgbaInnerWindow: Mat = mRgba.submat(
            top.toInt(), rows, left, width
        )
        rgbaInnerWindow.copyTo(rgba)
        Imgproc.cvtColor(rgbaInnerWindow, gray, Imgproc.COLOR_RGB2GRAY)
        Imgproc.cvtColor(rgbaInnerWindow, hsv, Imgproc.COLOR_RGB2HSV)
        Imgproc.cvtColor(rgbaInnerWindow, hls, Imgproc.COLOR_RGB2HLS)
        splitRGBChannels(rgba, hsv, hls)
        applyThreshold()
        Imgproc.erode(mask, mask, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0)))
        Imgproc.dilate(
            mask,
            mask,
            Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
        )
        Imgproc.Canny(mask, mEdges, 50.0, 150.0)
        Imgproc.resize(mEdges, mNew, Size(imgWidth.toDouble(), imgHeight.toDouble()))
        Imgproc.HoughCircles(
            mGray,
            circles,
            Imgproc.CV_HOUGH_GRADIENT,
            2.0,
            1000.0,
            175.0,
            120.0,
            25,
            125
        )
        if (circles.cols() > 0) {
            for (x in 0 until min(circles.cols(), 5)) {
                val circleVec = circles[0, x] ?: break
                val center = Point(
                    circleVec[0],
                    circleVec[1]
                )
                val radius = circleVec[2].toInt()
                val `val` = radius * 2 + 20
                // defines the ROI
                signRegion = Rect(
                    (center.x - radius - 10).toInt(),
                    (center.y - radius - 10).toInt(), `val`, `val`
                )
                if (!newSignFlag) {
                    analyzeObject(inputFrame.rgba(), signRegion, radius)
                }
            }
        }
        circles.release()
        Imgproc.HoughLinesP(mEdges, lines, 1.0, Math.PI / 180, 50, 25.0, 85.0)
        if (lines.rows() > 0) {
            getAverageSlopes(lines)
        }
        rgbaInnerWindow.release()
        Imgproc.line(
            mRgba,
            Point(vehicleCenterX1, vehicleCenterY1),
            Point(vehicleCenterX2, vehicleCenterY2),
            darkGreen,
            2,
            8
        )
        Imgproc.rectangle(
            mRgba, Point(left.toDouble(), top), Point(
                (cols - left).toDouble(), bottomY
            ), darkGreen, 2
        )
        return mRgba
    }

    inner class LocationBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val vehicleSpeed = Objects.requireNonNull(intent.extras)?.getDouble("speed")
            Log.e("BroadcastReceiver", "onReceive: $vehicleSpeed")
            if (vehicleSpeed != null) {
                speedUiRunnable.speedVal = vehicleSpeed
            }
            runOnUiThread(speedUiRunnable)
            if (vehicleSpeed != null) {
                if (vehicleSpeed > signUiRunnable.signVal && signUiRunnable.signVal > 0) {
                    speedingCount += 1
                    if (speedingCount >= 5) {
                        try {
                            toneGen1.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 200)
                        } catch (e: Exception) {
                            toneGen1.release()
                            toneGen1 = ToneGenerator(AudioManager.STREAM_MUSIC, 75)
                            Log.e("BroadcastReceiver", "onReceive: ", e)
                        }
                    }
                } else {
                    speedingCount = 0
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (javaCameraView != null) javaCameraView!!.disableView()
        val editor = getSharedPreferences("Prefs", MODE_PRIVATE).edit()
        Log.i(TAG, "onPause: Latest detected speed limit: " + signUiRunnable.signVal)
        editor.putInt("last_speed", signUiRunnable.signVal)
        editor.apply()
        timer.cancel()
        // stop updates to save battery
        stopService(Intent(this, LocationService::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
        if (javaCameraView != null) javaCameraView!!.disableView()
        stopService(Intent(this, LocationService::class.java))
        ttsLane.shutdown()
        ttsSpeed.shutdown()
    }

    @SuppressLint("SetTextI18n")
    override fun onResume() {
        val sharedPreferences = getSharedPreferences("Prefs", MODE_PRIVATE)
        super.onResume()
        if (OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV initialize success")
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        } else {
            Log.d(TAG, "OpenCV initialize failed")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback)
        }
        imgWidth = sharedPreferences.getInt("res_width", 1920)
        imgHeight = sharedPreferences.getInt("res_height", 1080)
        javaCameraView!!.setMaxFrameSize(imgWidth, imgHeight)
        javaCameraView!!.disableView()
        javaCameraView!!.enableView()
        setFullscreen()
        // restart location updates when back in focus
        val locationServiceIntent = Intent(this, LocationService::class.java)
        if (sharedPreferences.getBoolean("gps_enabled", true)) {
            startService(locationServiceIntent)
            speedTextView.text = "0.0 km/hr"
        } else {
            stopService(locationServiceIntent)
            speedTextView.text = "GPS Disabled"
        }
    }

    private fun splitRGBChannels(rgb_split: Mat?, hsv_split: Mat?, hls_split: Mat?) {
        val rgbChannels: List<Mat> = ArrayList()
        val hsvChannels: List<Mat> = ArrayList()
        val hlsChannels: List<Mat> = ArrayList()
        Core.split(rgb_split, rgbChannels)
        Core.split(hsv_split, hsvChannels)
        Core.split(hls_split, hlsChannels)

        rgbChannels[0].copyTo(mRed)
        rgbChannels[1].copyTo(mGreen)
        rgbChannels[2].copyTo(mBlue)

        hsvChannels[0].copyTo(mHueHsv)
        hsvChannels[1].copyTo(mSatHsv)
        hsvChannels[2].copyTo(mValHsv)

        hlsChannels[0].copyTo(mHueHls)
        hlsChannels[1].copyTo(mSatHls)
        hlsChannels[2].copyTo(mLightHls)

        for (i in rgbChannels.indices) {
            rgbChannels[i].release()
        }
        for (i in hsvChannels.indices) {
            hsvChannels[i].release()
        }
        for (i in hlsChannels.indices) {
            hlsChannels[i].release()
        }
    }

    private fun applyThreshold() {
        val lowerThreshold = Scalar(210.0)
        val higherThreshold = Scalar(255.0)

        Core.inRange(mRed, lowerThreshold, higherThreshold, mRed)
        Core.inRange(mValHsv, lowerThreshold, higherThreshold, mValHsv)

        Core.bitwise_and(mRed, mValHsv, mask)
    }

    private var curSpeedVal = 50
    private var signValue = ""
    private var isRunning = false
    private fun analyzeObject(img: Mat?, roi: Rect?, radius: Int) {
        val runnable = Runnable {
            isRunning = true
            val copy: Mat
            try {
                copy = Mat(img, roi)
                // Creates a bitmap with size of detected circle and stores the Mat into it
                bm = Bitmap.createBitmap(
                    abs(radius * 2 + 20),
                    abs(radius * 2 + 20),
                    Bitmap.Config.ARGB_8888
                )
                Utils.matToBitmap(copy, bm)
            } catch (e: Exception) {
                bm = null
            }
            if (bm != null) {
                val image = InputImage.fromBitmap(bm!!, 0)
                textRecognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        for (block in visionText.textBlocks) {
                            if (signValue != block.text) {
                                signValue = block.text
                                setUISign(signValue)
                            }
                        }
                    }
            }
            isRunning = false
        }
        if (!isRunning) {
            val textDetectionThread = Thread(runnable)
            textDetectionThread.run()
        }
    }

    private fun setUISign(`val`: String) {
        curSpeedVal = signUiRunnable.signVal
        when {
            `val`.contains("10") -> {
                signUiRunnable.signVal = 10
            }
            `val`.contains("20") -> {
                signUiRunnable.signVal = 20
            }
            `val`.contains("30") -> {
                signUiRunnable.signVal = 30
            }
            `val`.contains("40") -> {
                signUiRunnable.signVal = 40
            }
            `val`.contains("50") -> {
                signUiRunnable.signVal = 50
            }
            `val`.contains("60") -> {
                signUiRunnable.signVal = 60
            }
            `val`.contains("70") -> {
                signUiRunnable.signVal = 70
            }
            `val`.contains("80") -> {
                signUiRunnable.signVal = 80
            }
            `val`.contains("90") -> {
                signUiRunnable.signVal = 90
            }
            `val`.contains("100") -> {
                signUiRunnable.signVal = 100
            }
            `val`.contains("110") -> {
                signUiRunnable.signVal = 110
            }
            `val`.contains("120") -> {
                signUiRunnable.signVal = 120
            }
        }
        Log.i(
            TAG,
            "setUISign:" + curSpeedVal + " -------------------------------" + signUiRunnable.signVal
        )
        if (curSpeedVal != signUiRunnable.signVal) {
            ttsSpeed.speak(
                signUiRunnable.signVal.toString() + " kilometers per hour",
                TextToSpeech.QUEUE_FLUSH,
                null,
                "Speed Detected"
            )
        }
        runOnUiThread(signUiRunnable)
    }

    private fun getAverageSlopes(lines: Mat) {
        val leftSlopes: MutableList<Double> = ArrayList()
        val rightSlopes: MutableList<Double> = ArrayList()
        val leftYIntercept: MutableList<Double> = ArrayList()
        val rightYIntercept: MutableList<Double> = ArrayList()

        // Threshold zone for detected lanes, lines must be within this zone
        val zoneX1 = cols - left * 2.5
        val zoneX2 = left * 2.5
        Imgproc.line(
            mRgba,
            Point(zoneX1, top),
            Point(zoneX1, top + 5),
            Scalar(0.0, 155.0, 0.0),
            2,
            8
        )
        Imgproc.line(
            mRgba,
            Point(zoneX2, top),
            Point(zoneX2, top + 5),
            Scalar(0.0, 155.0, 0.0),
            2,
            8
        )
        for (i in 0 until lines.rows()) {
            val points = lines[i, 0]
            var x1: Double
            var y1: Double
            var x2: Double
            var y2: Double
            try {
                x1 = points[0]
                y1 = points[1]
                x2 = points[2]
                y2 = points[3]
                val p1 = Point(x1, y1)
                val p2 = Point(x2, y2)
                val slope = (p2.y - p1.y) / (p2.x - p1.x)
                var yIntercept: Double
                if (slope > 0.375 && slope < 2.6) { // Right lane
                    if (p1.x + left < zoneX1) {
                        rightSlopes.add(slope)
                        yIntercept = p1.y - p1.x * slope
                        rightYIntercept.add(yIntercept)
                    }
                } else if (slope > -2.6 && slope < -0.375) { // Left lane
                    if (p2.x + left > zoneX2) {
                        leftSlopes.add(slope)
                        yIntercept = p1.y - p1.x * slope
                        leftYIntercept.add(yIntercept)
                    }
                }
            } catch (e: Error) {
                Log.e(TAG, "onCameraFrame: ", e)
            }
        }
        var avgLeftSlope = 0.0
        var avgRightSlope = 0.0
        var avgLeftYIntercept = 0.0
        var avgRightYIntercept = 0.0
        for (i in rightSlopes.indices) {
            avgRightSlope += rightSlopes[i]
            avgRightYIntercept += rightYIntercept[i]
        }
        avgRightSlope /= rightSlopes.size.toDouble()
        avgRightYIntercept /= rightYIntercept.size.toDouble()
        for (i in leftSlopes.indices) {
            avgLeftSlope += leftSlopes[i]
            avgLeftYIntercept += leftYIntercept[i]
        }
        avgLeftSlope /= leftSlopes.size.toDouble()
        avgLeftYIntercept /= leftYIntercept.size.toDouble()

        // x = (y-b)/m
        // y = xm + b
        val newLeftTopX = -avgLeftYIntercept / avgLeftSlope + left
        val newRightTopX = (0 - avgRightYIntercept) / avgRightSlope + left
        val rightLanePt = Point(
            (imgHeight - avgRightYIntercept) / avgRightSlope,
            imgHeight.toDouble()
        )
        val leftLanePt = Point(0.0, -left * avgLeftSlope + avgLeftYIntercept)
        val topLeftPt = Point(newLeftTopX, 0 + top)
        val topRightPt = Point(newRightTopX, 0 + top)
        val bottomLeftPt =
            Point((-500 + left).toDouble(), -500 * avgLeftSlope + avgLeftYIntercept + top)
        val bottomRightPt = Point(rightLanePt.x + left, rightLanePt.y + top)
        if (rightSlopes.size != 0 && leftSlopes.size != 0) {
            val laneCenterX1 = (laneCenterY - top - avgLeftYIntercept) / avgLeftSlope + left
            val laneCenterX2 = (laneCenterY - top - avgRightYIntercept) / avgRightSlope + left
            laneCenterX = (laneCenterX1 + laneCenterX2) / 2
            laneZone = MatOfPoint(topLeftPt, topRightPt, bottomRightPt, bottomLeftPt)
            laneZoneMat.setTo(Scalar(0.0, 0.0, 0.0))
            Imgproc.fillConvexPoly(laneZoneMat, laneZone, Scalar(255.0, 240.0, 160.0))
            Core.addWeighted(laneZoneMat, .5, mRgba, 1.0, 0.0, mRgba)
            laneZone.release()
            val distanceFromCenter =
                sqrt((laneCenterX - vehicleCenterX1) * (laneCenterX - vehicleCenterX1) + (laneCenterY - laneCenterY) * (laneCenterY - laneCenterY))

            // If lane departure is detected, add an orange layer over output
            if (distanceFromCenter > 70) {
                if (!isTimerRunning) {
                    timer.start()
                    isTimerRunning = true
                    Log.i(TAG, "---------- LaneDrift Start: Timer STARTED ----------")
                }
                Core.add(mRgba, Scalar(255.0, 128.0, 0.0), mRgba)
            } else if (isTimerRunning) {
                timer.cancel()
                isTimerRunning = false
                Log.i(TAG, "---------- LaneDeparture Stop: Timer STOPPED ----------")
                if (ttsLane.isSpeaking) {
                    ttsLane.stop()
                }
            }
            Imgproc.line(
                mRgba,
                Point(vehicleCenterX1, laneCenterY),
                Point(laneCenterX, laneCenterY),
                darkGreen,
                2,
                8
            )
            Imgproc.circle(mRgba, Point(laneCenterX, laneCenterY), 4, Scalar(0.0, 0.0, 255.0), 7)
        } else if (isTimerRunning) {
            timer.cancel()
            isTimerRunning = false
            if (ttsLane.isSpeaking) {
                ttsLane.stop()
            }
            Log.i(TAG, "---------- TIMER CANCELED: No lanes detected ----------")
        }
        if (leftSlopes.size != 0) {
            Imgproc.line(mRgba, topLeftPt, bottomLeftPt, Scalar(225.0, 0.0, 0.0), 8)
        }
        if (rightSlopes.size != 0) {
            Imgproc.line(mRgba, bottomRightPt, topRightPt, Scalar(0.0, 0.0, 225.0), 8)
        }
    }

    override fun onInit(status: Int) {
        ttsSpeed.language = Locale.ENGLISH
        ttsLane.language = Locale.ENGLISH
        ttsLane.setSpeechRate(0.9f)
    }

    private fun setUpCameraServices() {
        val sharedPreferences = getSharedPreferences("Prefs", MODE_PRIVATE)
        var firstLaunch = false
        val editor = sharedPreferences.edit()
        try {
            firstLaunch = sharedPreferences.getBoolean("first_launch", true)
            Log.i(TAG, "setUpCameraServices: $firstLaunch")
        } catch (e: Exception) {
            Log.e(TAG, "setUpCameraServices: ", e)
        }
        if (firstLaunch) {
            editor.putBoolean("gps_enabled", true)
            editor.putBoolean("sound_enabled", true)
            val manager = getSystemService(CAMERA_SERVICE) as CameraManager
            try {
                val cameraId = manager.cameraIdList[0]
                val characteristics = manager.getCameraCharacteristics(cameraId)
                val map =
                    characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
                for (size in map.getOutputSizes(SurfaceTexture::class.java)) {
                    val ratio = size.width.toFloat() / size.height.toFloat()
                    if (ratio >= 1.3 && size.width < 900) {
                        imgHeight = size.height
                        imgWidth = size.width
                        break
                    }
                }
                editor.putInt("res_height", imgHeight)
                editor.putInt("res_width", imgWidth)
                Log.i(
                    TAG,
                    "setUpCameraServices: $sharedPreferences"
                )
            } catch (error: Error) {
                Log.e(TAG, "onCreate: ", error)
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
            editor.putBoolean("first_launch", false)
            editor.apply()
        } else {
            imgHeight = sharedPreferences.getInt("res_height", 1080)
            imgWidth = sharedPreferences.getInt("res_width", 1920)
        }
        if (sharedPreferences.getBoolean("sound_enabled", true)) {
            fabSound.setImageResource(R.drawable.volume_on_white_24dp)
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_UNMUTE,
                0
            )
        } else {
            fabSound.setImageResource(R.drawable.volume_off_white_24dp)
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_MUTE,
                0
            )
        }
    }

    private fun setViewClickListeners() {
        fabSettings.setOnClickListener {
            runOnUiThread {
                isOpen = if (isOpen) {
                    fabResolutions.startAnimation(fabClose)
                    fabGps.startAnimation(fabClose)
                    fabSound.startAnimation(fabClose)
                    fabSound.isClickable = false
                    fabSettings.startAnimation(fabRotateAntiCw)
                    fabResolutions.isClickable = false
                    fabGps.isClickable = false
                    false
                } else {
                    fabResolutions.startAnimation(fabOpen)
                    fabGps.startAnimation(fabOpen)
                    fabSound.startAnimation(fabOpen)
                    fabSound.isClickable = true
                    fabSettings.startAnimation(fabRotateCw)
                    fabResolutions.isClickable = true
                    fabGps.isClickable = true
                    true
                }
            }
        }
        fabResolutions.setOnClickListener {
            val intent = Intent(applicationContext, ResolutionSettingsActivity::class.java)
            startActivity(intent)
            Toast.makeText(applicationContext, "Resolution Settings", Toast.LENGTH_SHORT).show()
        }
        fabGps.setOnClickListener {
            val intent = Intent(applicationContext, GpsSettingsActivity::class.java)
            startActivity(intent)
            Toast.makeText(applicationContext, "GPS Settings", Toast.LENGTH_SHORT).show()
        }
        fabSound.setOnClickListener {
            val sharedPreferences = getSharedPreferences("Prefs", MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            if (sharedPreferences.getBoolean("sound_enabled", true)) {
                fabSound.setImageResource(R.drawable.volume_off_white_24dp)
                Toast.makeText(applicationContext, "Alerts/warnings disabled", Toast.LENGTH_SHORT)
                    .show()
                editor.putBoolean("sound_enabled", false)
                try {
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_MUTE,
                        0
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "onClick: ", e)
                }
            } else {
                fabSound.setImageResource(R.drawable.volume_on_white_24dp)
                Toast.makeText(applicationContext, "Alerts/warnings enabled", Toast.LENGTH_SHORT)
                    .show()
                editor.putBoolean("sound_enabled", true)
                try {
                    audioManager.adjustStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_UNMUTE,
                        0
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "onClick: ", e)
                }
            }
            editor.apply()
        }
    }

    private fun initializeAllMats() {
        mRgba = Mat()
        mGray = Mat()
        circles = Mat()
        mRed = Mat()
        mGreen = Mat()
        mBlue = Mat()
        mHueHls = Mat()
        mLightHls = Mat()
        mSatHls = Mat()
        mHueHsv = Mat()
        mSatHsv = Mat()
        mValHsv = Mat()
        hsv = Mat()
        hls = Mat()
        gray = Mat()
        rgba = Mat()
        mNew = Mat()
        mask = Mat()
        mEdges = Mat()
        laneZoneMat = Mat(rows, cols, CvType.CV_8UC4)
    }

    private fun releaseAllMats() {
        mRgba.release()
        mGray.release()
        circles.release()
        mRed.release()
        mGreen.release()
        mBlue.release()
        mHueHls.release()
        mLightHls.release()
        mSatHls.release()
        mHueHsv.release()
        mSatHsv.release()
        mValHsv.release()
        hsv.release()
        hls.release()
        gray.release()
        rgba.release()
        mNew.release()
        mask.release()
        mEdges.release()
        laneZoneMat.release()
    }

    private val permissions: Unit
        get() {
            if (!hasPermissions(this, *PERMISSIONS)) {
                Toast.makeText(
                    applicationContext,
                    "Camera permission is needed or \nthis application will not work.",
                    Toast.LENGTH_LONG
                ).show()
                ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL)
            }
        }


    private var displayMetrics: DisplayMetrics? = null
    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)
        val disHeight = displayMetrics!!.heightPixels.toFloat()
        val disWidth = displayMetrics!!.widthPixels.toFloat()
        val x = event.x
        val y = event.y
        val z = imgHeight / disHeight
        val scaledY = y * z
        if (scaledY > imgHeight * 0.25 && scaledY < imgHeight * 0.75 && x > 125 && x < disWidth - 125) top =
            scaledY.toDouble()
        return true
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
        private const val TAG = "MainActivity"
        /* Checks if all the needed permissions are enabled and asks user if not */
        var PERMISSION_ALL = 1
        var PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        fun hasPermissions(context: Context?, vararg permissions: String): Boolean {
            if (context != null) {
                for (permission in permissions) {
                    if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                        return false
                    }
                }
            }
            return true
        }
    }
}
