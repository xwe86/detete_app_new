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

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.android.example.cameraxbasic.utils.ANIMATION_FAST_MILLIS
import com.android.example.cameraxbasic.utils.ANIMATION_SLOW_MILLIS
import org.tensorflow.lite.examples.objectdetection.ObjectDetectorHelper
import org.tensorflow.lite.examples.objectdetection.R
import org.tensorflow.lite.examples.objectdetection.databinding.FragmentCameraBinding
import org.tensorflow.lite.examples.objectdetection.databinding.VincCameraBinding
import org.tensorflow.lite.task.vision.detector.Detection
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


class VINCFragment : Fragment(), ObjectDetectorHelper.DetectorListener {

    private val TAG = "ObjectDetection"
    private lateinit var windowManager: WindowManager
    private val animatorSet: AnimatorSet = AnimatorSet()
    private var _fragmentCameraBinding: VincCameraBinding? = null
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

    //识别结果暂存列表
    val recognitionResults: MutableList<Detection>? = mutableListOf()

    //识别统计结果的间隔， 1s内目标数据出现了，就算识别成功
    private val recognitionInterval = 1000L



    private val handler = Handler(Looper.getMainLooper())
    private var tipColseTime = 3000L
    private var tipLeftTime = 3000L
    private var tipOKTime = 3000L


    //从 Paused 状态恢复到 Resumed 状态时，系统会调用 onResume() 方法。
    override fun onResume() {
        super.onResume()
//        // Make sure that all permissions are still present, since the
//        // user could have removed them while the app was in paused state.
//        //PermissionsFragment.hasPermissions(requireContext()) 是一个自定义方法，它通常用于检查应用是否具有特定权限，比如相机权限、存储权限等。如果应用没有所需的权限，条件判断为真。
//        // Navigation.findNavController() 是 Navigation 组件提供的一个方法，用于查找与给定 Activity 相关联的 NavController
//        if (!PermissionsFragment.hasPermissions(requireContext())) {
//            //Navigation.findNavController() 是 Navigation 组件提供的一个方法，用于查找与给定 Activity 相关联的 NavController。
//            //requireActivity() 返回当前 Fragment 所关联的 Activity。
//            //R.id.fragment_container 是用于承载 Fragment 的容器的 ID。通常，这是在布局文件中定义的 NavHostFragment 的 ID。
//            Navigation.findNavController(requireActivity(), R.id.fragment_container)
//                .navigate(CameraFragmentDirections.actionCameraToPermissions())
//        }
//


        handler.post(timerRunnable)
    }

    override fun onDestroyView() {

        super.onDestroyView()
        _fragmentCameraBinding = null
        // Shut down our background executor
        cameraExecutor.shutdown()
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentCameraBinding = VincCameraBinding.inflate(inflater, container, false)
        windowManager = requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val permissionFragment = PermissionsFragment()
        if (!PermissionsFragment.hasPermissions(requireContext())) {
            // 请求授权
            parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, permissionFragment)
                    .addToBackStack(null)
                    .commit()
        }

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

    }


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


        // CameraSelector - makes assumption that we're only using the back camera
        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        lastRecordTime = System.currentTimeMillis();

        // Preview. Only using the 4:3 ratio because this is the closest to our models
        preview =
            Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
                .build()


        // ImageCapture
        // 设置目标分辨率
        // 例如，设置分辨率为 1280x720

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
//            .setTargetResolution(targetResolution)
            // We request aspect ratio but no resolution to match preview config, but letting
            // CameraX optimize for whatever specific resolution best fits our use cases
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            // Set initial target rotation, we will have to call this again if rotation changes
            // during the lifecycle of this use case
            .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
            .build()


        //   分析相机捕获的图像帧 ImageAnalysis. Using RGBA 8888 to match how our models work
        imageAnalyzer =
            ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
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
                            Log.d(tag, "照片尺寸${image.width} ${image.height}")
                        }

                        detectObjects(image)
                    }
                }
        // Listener for button used to capture photo
        //监听按钮拍照
        fragmentCameraBinding?.cameraCaptureButton?.setOnClickListener {
            // 存图
            saveImage()

        }

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageCapture, imageAnalyzer
            )

            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    /**
     * 保存相机当前的图片
     */
    private fun saveImage() {
        imageCapture?.let { imageCapture ->

            Log.i("相机", "开点击了拍照")
            // Create time stamped name and MediaStore entry.
            val name = SimpleDateFormat(FILENAME, Locale.US)
                .format(System.currentTimeMillis())
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, PHOTO_TYPE)
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                    val appName = "test"
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/${appName}")
                }
            }

            // Create output options object which contains file + metadata
            val outputOptions = ImageCapture.OutputFileOptions
                .Builder(
                    requireContext().contentResolver,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )
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
                        val originalBitmap = BitmapFactory.decodeFile( "/storage/emulated/0//Pictures/test/"+name+".jpg" )
                        val x = 0 // 裁剪区域的左上角x坐标
                        val y = 0 // 裁剪区域的左上角y坐标
                        val width = originalBitmap.width*0.8 // 裁剪区域的宽度
                        val height = originalBitmap.height*0.7 // 裁剪区域的高度


                        // 裁剪原始图片
                        val croppedBitmap = Bitmap.createBitmap(originalBitmap, x, y, width.toInt(), height.toInt())


                        // 保存裁剪后的图片到新的文件
                        val croppedPhotoFile: File =
                            File("/storage/emulated/0//Pictures/test/1.jpg")
                        try {
                            FileOutputStream(croppedPhotoFile).use { out ->
                                croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                            }
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }


                        // 清理资源
                        originalBitmap.recycle()
                        croppedBitmap.recycle()


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
        Log.d(
            "相机",
            "相机位置变更 ${fragmentCameraBinding.viewFinder.display.rotation}   ==>  $newConfig "
        )
        imageAnalyzer?.targetRotation = fragmentCameraBinding.viewFinder.display.rotation
    }


    // 停止图像分析
    private fun stopImageAnalyzer() {
        if (imageAnalyzer != null) {
            imageAnalyzer?.clearAnalyzer()
            imageAnalyzer = null
        }
    }


    // Update UI after objects have been detected. Extracts original image height/width
    // to scale and place bounding boxes properly through OverlayView
    //在检测到对象后更新 UI。提取原始图像高度/宽度
    //    通过 OverlayVie 正确缩放和放置边界框
    override fun onResults(
        results: MutableList<Detection>?,
        inferenceTime: Long,
        imageHeight: Int,
        imageWidth: Int
    ) {
        activity?.runOnUiThread {
//            fragmentCameraBinding.bottomSheetLayout.inferenceTimeVal.text =
//                            String.format("%d ms", inferenceTime)

            try {
                fragmentCameraBinding.inferenceTimeVal.text =
                    String.format("%d ms", inferenceTime)

                //处理识别结果
                val currentTime = System.currentTimeMillis()
                // 每秒更新一次提示
                if (currentTime - lastRecordTime >= recognitionInterval) {
                    results?.let { recognitionResults?.addAll(it) }
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

                Log.i("相机"," imageWith：$imageWidth, imageHeight $imageHeight")

                // Force a redraw
                fragmentCameraBinding.overlay.invalidate()
            } catch (e: Exception) {
                Log.e("相机", "相机异常", e)
            }
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
        if (dataSet.size>5){
//            Toast.makeText(context, "45°识别成功开始录图像", Toast.LENGTH_SHORT).show()
            Log.i(tag,"45°识别成功开始录图像")
//            saveImage()
            // 在Activity或Fragment中调用以下代码来显示Toast消息
//            Toast.makeText(context, "45°成功保存图像", Toast.LENGTH_SHORT).show()
        }


        fragmentCameraBinding.detectData.text = dataSet.joinToString(separator = ", ")


    }


    /**
     * We need a display listener for orientation changes that do not trigger a configuration
     * change, for example if we choose to override config change in manifest or for 180-degree
     * orientation changes.
     */
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = view?.let { view ->
            if (displayId == this@VINCFragment.displayId) {
                Log.d(TAG, "Rotation changed: ${view.display.rotation}")
                imageCapture?.targetRotation = view.display.rotation
                imageAnalyzer?.targetRotation = view.display.rotation
            }
        } ?: Unit
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacksAndMessages(null)
        val imageView = fragmentCameraBinding.arrowLeft
        imageView.visibility = View.INVISIBLE
        // 设置目标View,播放动画
        animatorSet.cancel()
        imageAnalyzer?.clearAnalyzer()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        // 设置目标View,播放动画
        animatorSet.cancel()
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
