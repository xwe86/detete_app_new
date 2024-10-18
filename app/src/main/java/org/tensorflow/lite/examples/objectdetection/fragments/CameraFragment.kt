/*
 * Copyright 2022 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tensorflow.lite.examples.objectdetection.fragments

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.android.example.cameraxbasic.utils.ANIMATION_FAST_MILLIS
import com.android.example.cameraxbasic.utils.ANIMATION_SLOW_MILLIS
import org.tensorflow.lite.examples.objectdetection.ObjectDetectorHelper
import org.tensorflow.lite.examples.objectdetection.R
import org.tensorflow.lite.examples.objectdetection.databinding.ActivityMainBinding
import org.tensorflow.lite.examples.objectdetection.databinding.FragmentCameraBinding
import org.tensorflow.lite.task.vision.detector.Detection
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class CameraFragment : Fragment(), ObjectDetectorHelper.DetectorListener {

    private val TAG = "ObjectDetection"
    private lateinit var windowManager: WindowManager
    private var _fragmentCameraBinding: FragmentCameraBinding? = null
    private val animatorSet: AnimatorSet = AnimatorSet()
    private val fragmentCameraBinding
        get() = _fragmentCameraBinding!!
    private lateinit var objectDetectorHelper: ObjectDetectorHelper
    private lateinit var bitmapBuffer: Bitmap
    private var displayId: Int = -1
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null

    /** Blocking camera operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService


    private var lastRecordTime = 0L // 上次记录的时间戳
    private val handler = Handler(Looper.getMainLooper())
    private var tipColseTime = 3000L
    private var tipLeftTime = 3000L
    private var tipOKTime = 3000L

    override fun onResume() {
        super.onResume()
        // Make sure that all permissions are still present, since the
        // user could have removed them while the app was in paused state.
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            Navigation.findNavController(requireActivity(), R.id.fragment_container)
                .navigate(CameraFragmentDirections.actionCameraToPermissions())
        }
    }

    override fun onDestroyView() {
        _fragmentCameraBinding = null
        super.onDestroyView()

        // Shut down our background executor
        cameraExecutor.shutdown()
    }



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false)
        windowManager = requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager


//        playRight()
        // 显示"请靠近"文案
        handler.postDelayed({
            // 在这里更新UI显示"请靠近"文案
            showTipsText("请靠近")
        }, tipColseTime)

        // 显示"向左移动相机"文案
        handler.postDelayed({
            playLeft()
            // 在这里更新UI显示"向左移动相机"文案
            showTipsText("向左移动相机")
        }, tipColseTime +tipLeftTime)

        // 显示"识别成功"文案
        handler.postDelayed({
            playLeftStop()
            // 在这里更新UI显示"识别成功"文案
            showTipsText("识别成功")
        }, tipColseTime +tipLeftTime +tipOKTime)
        return fragmentCameraBinding.root
    }

    private fun playLeft() {
//        val animatorSet = AnimatorSet()
        val imageView = fragmentCameraBinding.arrowLeft
        imageView.visibility = View.VISIBLE
        val translationAnim = ObjectAnimator.ofFloat(imageView, "translationX", 200f, 50f)
        translationAnim.repeatCount = 10 // 设置重复次数为无限
        translationAnim.duration = 1000 // 设置动画持续时间
        translationAnim.interpolator = LinearInterpolator() // 设置插值器，可以使动画匀速播放
        val alphaAnim = ObjectAnimator.ofFloat(imageView, "alpha", 1.0f, 0.0f)
        alphaAnim.repeatCount = ValueAnimator.INFINITE
        alphaAnim.duration = 1000
        animatorSet.playTogether(translationAnim, alphaAnim)
        // 设置目标View,播放动画
        animatorSet.start()
    }

    private fun playLeftStop() {
        val imageView = fragmentCameraBinding.arrowLeft
        imageView.visibility = View.INVISIBLE
        // 设置目标View,播放动画
        animatorSet.cancel()
    }

//    private fun playRight() {
//        val animatorSet = AnimatorSet()
//        val imageView = fragmentCameraBinding.arrowBottom
//        imageView.visibility = View.VISIBLE
//        val translationAnim = ObjectAnimator.ofFloat(imageView, "translationY", 680f, 830f)
//        translationAnim.repeatCount = ValueAnimator.INFINITE // 设置重复次数为无限
//        translationAnim.duration = 1000 // 设置动画持续时间
//        translationAnim.interpolator = LinearInterpolator() // 设置插值器，可以使动画匀速播放
//        val alphaAnim = ObjectAnimator.ofFloat(imageView, "alpha", 1.0f, 0.0f)
//        alphaAnim.repeatCount = ValueAnimator.INFINITE
//        alphaAnim.duration = 1000
//        animatorSet.playTogether(translationAnim, alphaAnim)
//        // 设置目标View,播放动画
//        animatorSet.start()
//    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        objectDetectorHelper = ObjectDetectorHelper(
            context = requireContext(),
            objectDetectorListener = this
        )

        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Wait for the views to be properly laid out
        fragmentCameraBinding.viewFinder.post {
            // Set up the camera and its use cases
            setUpCamera()
        }


        // Attach listeners to UI control widgets
//        initBottomSheetControls()
    }

//    private fun initBottomSheetControls() {
//        // When clicked, lower detection score threshold floor
//        fragmentCameraBinding.bottomSheetLayout.thresholdMinus.setOnClickListener {
//            if (objectDetectorHelper.threshold >= 0.1) {
//                objectDetectorHelper.threshold -= 0.1f
//                updateControlsUi()
//            }
//        }
//
//        // When clicked, raise detection score threshold floor
//        fragmentCameraBinding.bottomSheetLayout.thresholdPlus.setOnClickListener {
//            if (objectDetectorHelper.threshold <= 0.8) {
//                objectDetectorHelper.threshold += 0.1f
//                updateControlsUi()
//            }
//        }
//
//        // When clicked, reduce the number of objects that can be detected at a time
//        fragmentCameraBinding.bottomSheetLayout.maxResultsMinus.setOnClickListener {
//            if (objectDetectorHelper.maxResults > 1) {
//                objectDetectorHelper.maxResults--
//                updateControlsUi()
//            }
//        }
//
//        // When clicked, increase the number of objects that can be detected at a time
//        fragmentCameraBinding.bottomSheetLayout.maxResultsPlus.setOnClickListener {
//            if (objectDetectorHelper.maxResults < 5) {
//                objectDetectorHelper.maxResults++
//                updateControlsUi()
//            }
//        }
//
//        // When clicked, decrease the number of threads used for detection
//        fragmentCameraBinding.bottomSheetLayout.threadsMinus.setOnClickListener {
//            if (objectDetectorHelper.numThreads > 1) {
//                objectDetectorHelper.numThreads--
//                updateControlsUi()
//            }
//        }
//
//        // When clicked, increase the number of threads used for detection
//        fragmentCameraBinding.bottomSheetLayout.threadsPlus.setOnClickListener {
//            if (objectDetectorHelper.numThreads < 4) {
//                objectDetectorHelper.numThreads++
//                updateControlsUi()
//            }
//        }
//
//        // When clicked, change the underlying hardware used for inference. Current options are CPU
//        // GPU, and NNAPI
//        fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.setSelection(0, false)
//        fragmentCameraBinding.bottomSheetLayout.spinnerDelegate.onItemSelectedListener =
//            object : AdapterView.OnItemSelectedListener {
//                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
//                    objectDetectorHelper.currentDelegate = p2
//                    updateControlsUi()
//                }
//
//                override fun onNothingSelected(p0: AdapterView<*>?) {
//                    /* no op */
//                }
//            }
//
//        // When clicked, change the underlying model used for object detection
//        fragmentCameraBinding.bottomSheetLayout.spinnerModel.setSelection(0, false)
//        fragmentCameraBinding.bottomSheetLayout.spinnerModel.onItemSelectedListener =
//            object : AdapterView.OnItemSelectedListener {
//                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
//                    objectDetectorHelper.currentModel = p2
//                    updateControlsUi()
//                }
//
//                override fun onNothingSelected(p0: AdapterView<*>?) {
//                    /* no op */
//                }
//            }
//    }

    // Update the values displayed in the bottom sheet. Reset detector.
//    private fun updateControlsUi() {
//        fragmentCameraBinding.bottomSheetLayout.maxResultsValue.text =
//            objectDetectorHelper.maxResults.toString()
//        fragmentCameraBinding.bottomSheetLayout.thresholdValue.text =
//            String.format("%.2f", objectDetectorHelper.threshold)
//        fragmentCameraBinding.bottomSheetLayout.threadsValue.text =
//            objectDetectorHelper.numThreads.toString()
//
//        // Needs to be cleared instead of reinitialized because the GPU
//        // delegate needs to be initialized on the thread using it when applicable
//        objectDetectorHelper.clearObjectDetector()
//        fragmentCameraBinding.overlay.clear()
//    }

    // Initialize CameraX, and prepare to bind the camera use cases
    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                // CameraProvider
                cameraProvider = cameraProviderFuture.get()

                // Build and bind the camera use cases
                bindCameraUseCases()
            },
            ContextCompat.getMainExecutor(requireContext())
        )
    }

    // 获取当前屏幕方向
    fun isLandscape(): Boolean {
        val orientation = resources.configuration.orientation
        return orientation == Configuration.ORIENTATION_LANDSCAPE
    }

// 在 Activity 中调用 isLandscape() 函数判断屏幕方向

    // Declare and bind preview, capture and analysis use cases
    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {

        // CameraProvider
        val cameraProvider =
            cameraProvider ?: throw IllegalStateException("Camera initialization failed.")
        val metrics = windowManager.getCurrentWindowMetrics().bounds
        val screenAspectRatio = aspectRatio(metrics.width(), metrics.height())
        Log.d(TAG, "Preview aspect ratio: $screenAspectRatio")

        val rotation = fragmentCameraBinding.viewFinder.display.rotation
        // CameraSelector - makes assumption that we're only using the back camera
        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        // Preview. Only using the 4:3 ratio because this is the closest to our models
        preview =
            Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(Surface.ROTATION_0)
                .build()
        // 获取屏幕旋转角度

        Log.d("相机", "屏幕旋转的角度：${rotation}")


        // ImageCapture
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            // We request aspect ratio but no resolution to match preview config, but letting
            // CameraX optimize for whatever specific resolution best fits our use cases
            .setTargetAspectRatio(screenAspectRatio)
            // Set initial target rotation, we will have to call this again if rotation changes
            // during the lifecycle of this use case
            .setTargetRotation(rotation)
            .build()


        //   分析相机捕获的图像帧 ImageAnalysis. Using RGBA 8888 to match how our models work
        imageAnalyzer =
            ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(Surface.ROTATION_0)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                // The analyzer can then be assigned to the instance
                .also {
                    it.setAnalyzer(cameraExecutor) { image ->
                        if (!::bitmapBuffer.isInitialized) {
                            // The image rotation and RGB image buffer are initialized only once
                            // the analyzer has started running
                            bitmapBuffer = Bitmap.createBitmap(
                                image.width,
                                image.height,
                                Bitmap.Config.ARGB_8888
                            )
                        }

                        detectObjects(image)
                    }
                }
        // Listener for button used to capture photo
        fragmentCameraBinding?.cameraCaptureButton?.setOnClickListener {

            // Get a stable reference of the modifiable image capture use case
            imageCapture?.let { imageCapture ->

                // Create time stamped name and MediaStore entry.
                val name = SimpleDateFormat(FILENAME, Locale.US)
                    .format(System.currentTimeMillis())
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, PHOTO_TYPE)
                    if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                        val appName = "test"
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/${appName}")
                    }
                }

                // Create output options object which contains file + metadata
                val outputOptions = ImageCapture.OutputFileOptions
                    .Builder(requireContext().contentResolver,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues)
                    .build()

                // Setup image capture listener which is triggered after photo has been taken
                imageCapture.takePicture(
                    outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
                        override fun onError(exc: ImageCaptureException) {
                            Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                        }

                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            val savedUri = output.savedUri
                            Log.d(TAG, "Photo capture succeeded: $savedUri")


                            // Implicit broadcasts will be ignored for devices running API level >= 24
                            // so if you only target API level 24+ you can remove this statement
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                                // Suppress deprecated Camera usage needed for API level 23 and below
                                @Suppress("DEPRECATION")
                                requireActivity().sendBroadcast(
                                    Intent(android.hardware.Camera.ACTION_NEW_PICTURE, savedUri)
                                )
                            }
                        }
                    })

                // We can only change the foreground Drawable using API level 23+ API
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                    // Display flash animation to indicate that photo was captured
                    fragmentCameraBinding.root.postDelayed({
                        fragmentCameraBinding.root.foreground = ColorDrawable(Color.WHITE)
                        fragmentCameraBinding.root.postDelayed(
                            { fragmentCameraBinding.root.foreground = null }, ANIMATION_FAST_MILLIS
                        )
                    }, ANIMATION_SLOW_MILLIS)
                }
            }
        }

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)

            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }


    /**
     *  [androidx.camera.core.ImageAnalysis.Builder] requires enum value of
     *  [androidx.camera.core.AspectRatio]. Currently it has values of 4:3 & 16:9.
     *
     *  Detecting the most suitable ratio for dimensions provided in @params by counting absolute
     *  of preview ratio to one of the provided values.
     *
     *  @param width - preview width
     *  @param height - preview height
     *  @return suitable aspect ratio
     */
    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }
    //接收到图像调用识别helper 进行识别
    private fun detectObjects(image: ImageProxy) {
        // Copy out RGB bits to the shared bitmap buffer
        image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }

        val imageRotation = image.imageInfo.rotationDegrees
        // Pass Bitmap and rotation to the object detector helper for processing and detection
        objectDetectorHelper.detect(bitmapBuffer, imageRotation)
    }


    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d("相机","相机位置变更 ${fragmentCameraBinding.viewFinder.display.rotation}   ==>  $newConfig ")
        imageAnalyzer?.targetRotation = fragmentCameraBinding.viewFinder.display.rotation
    }

    // Update UI after objects have been detected. Extracts original image height/width
    // to scale and place bounding boxes properly through OverlayView
    override fun onResults(
        results: MutableList<Detection>?,
        inferenceTime: Long,
        imageHeight: Int,
        imageWidth: Int
    ) {
        activity?.runOnUiThread {
//            fragmentCameraBinding.bottomSheetLayout.inferenceTimeVal.text =
//                            String.format("%d ms", inferenceTime)
            fragmentCameraBinding.inferenceTimeVal.text =
                String.format("%d ms", inferenceTime)

            //处理识别结果
            val currentTime = System.currentTimeMillis()
            // 每秒更新一次提示
            if (currentTime - lastRecordTime >= 1000) {
                // 处理图像并记录结果
                recordAnalysisResult(results, "" + lastRecordTime)
                lastRecordTime = currentTime
            }

            // Pass necessary information to OverlayView for drawing on the canvas
            fragmentCameraBinding.overlay.setResults(
                results ?: LinkedList<Detection>(),
                imageHeight,
                imageWidth
            )

            // Force a redraw
            fragmentCameraBinding.overlay.invalidate()
        }
    }

    // 定时器，每秒触发一次
    private val timerRunnable = object : Runnable {
        override fun run() {
            handler.postDelayed(this, 1000)
        }
    }


    /**
     * 设置提示文案
     */
    fun showTipsText(text: String) {
        fragmentCameraBinding.detectTip.text = text
    }


    private fun recordAnalysisResult(
        results: MutableList<Detection>?,
        recordAnalysisResult: String
    ) {
        // 处理图像并记录结果的逻辑
        Log.d("", "记录时间戳 $recordAnalysisResult")
        // 创建一个 Set 集合，用于存放 List 中的数据
        val dataSet = HashSet<String>()
        for (result in results ?: LinkedList<Detection>()) {
            val boundingBox = result.boundingBox
            var scaleFactor: Float = 1f
            val top = boundingBox.top * scaleFactor
            val bottom = boundingBox.bottom * scaleFactor
            val left = boundingBox.left * scaleFactor
            val right = boundingBox.right * scaleFactor

//            drawableText =
//                result.categories[0].label + " " + String.format(
//                    "%.2f",
//                    result.categories[0].score
//                ) +
//                        "top :$top bottom: $bottom left: $left right: $right"
            dataSet.add(result.categories[0].label)
        }


        fragmentCameraBinding.detectData.text =  dataSet.joinToString(separator = ", ")


    }


//    override fun onResume() {
//        super.onResume()
//        handler.post(timerRunnable)
//    }


    /**
     * We need a display listener for orientation changes that do not trigger a configuration
     * change, for example if we choose to override config change in manifest or for 180-degree
     * orientation changes.
     */
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = view?.let { view ->
            if (displayId == this@CameraFragment.displayId) {
                Log.d(TAG, "Rotation changed: ${view.display.rotation}")
                imageCapture?.targetRotation = view.display.rotation
                imageAnalyzer?.targetRotation = view.display.rotation
            }
        } ?: Unit
    }
    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(timerRunnable)
    }

    override fun onError(error: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        }
    }
    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val PHOTO_TYPE = "image/jpeg"
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }
}
