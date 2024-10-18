package org.tensorflow.lite.examples.objectdetection

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import android.util.Log
import java.util.*

/**
 * Author       wildma
 * Github       https://github.com/wildma
 * Date         2018/6/24
 * Desc	        ${加速度控制器，用来控制对焦}
 */
class SensorControler private constructor(context: Context) : SensorEventListener {
    private val mSensorManager: SensorManager
    private val mSensor: Sensor
    private var mX = 0
    private var mY = 0
    private var mZ = 0
    private var lastStaticStamp: Long = 0
    var mCalendar: Calendar? = null
    private var foucsing = 1 //1 表示没有被锁定 0表示被锁定
    var isFocusing = false
    var canFocusIn = false //内部是否能够对焦控制机制
    var canFocus = false
    private var STATUE = STATUS_NONE
    fun onStart() {
        restParams()
        canFocus = true
        mSensorManager.registerListener(
            this, mSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    fun onStop() {
        mCameraFocusListener = null
        mSensorManager.unregisterListener(this, mSensor)
        canFocus = false
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor == null) {
            return
        }
        if (isFocusing) {
            restParams()
            return
        }
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0].toInt()
            val y = event.values[1].toInt()
            val z = event.values[2].toInt()
            mCalendar = Calendar.getInstance()
            val stamp = System.currentTimeMillis() // 1393844912
            val second = stamp/1000 // 53
            if (STATUE != STATUS_NONE) {
                val px = Math.abs(mX - x)
                val py = Math.abs(mY - y)
                val pz = Math.abs(mZ - z)
                //                Log.d(TAG, "pX:" + px + "  pY:" + py + "  pZ:" + pz + "    stamp:"
                //                        + stamp + "  second:" + second);
                val value = Math.sqrt((px * px + py * py + pz * pz).toDouble())
                if (value > 1.4) {
                    //                    textviewF.setText("检测手机在移动..");
                    //                    Log.i(TAG,"mobile moving");
                    STATUE = STATUS_MOVE
                } else {
                    //                    textviewF.setText("检测手机静止..");
                    //                    Log.i(TAG,"mobile static");
                    //上一次状态是move，记录静态时间点
                    if (STATUE == STATUS_MOVE) {
                        lastStaticStamp = stamp
                        canFocusIn = true
                    }
                    if (canFocusIn) {
                        if (stamp - lastStaticStamp > DELEY_DURATION) {
                            //移动后静止一段时间，可以发生对焦行为
                            if (!isFocusing) {
                                canFocusIn = false
                                //                                onCameraFocus();
                                if (mCameraFocusListener != null) {
                                    mCameraFocusListener!!.onFocus()
                                }
                                //                                Log.i(TAG,"mobile focusing");
                            }
                        }
                    }
                    STATUE = STATUS_STATIC
                }
            } else {
                lastStaticStamp = stamp
                STATUE = STATUS_STATIC
            }
            mX = x
            mY = y
            mZ = z
        }
    }

    /**
     * 重置参数
     */
    private fun restParams() {
        STATUE = STATUS_NONE
        canFocusIn = false
        mX = 0
        mY = 0
        mZ = 0
    }

    /**
     * 对焦是否被锁定
     *
     * @return
     */
    val isFocusLocked: Boolean
        get() = if (canFocus) {
            foucsing <= 0
        } else false

    /**
     * 锁定对焦
     */
    fun lockFocus() {
        isFocusing = true
        foucsing--
        Log.i(TAG, "lockFocus")
    }

    /**
     * 解锁对焦
     */
    fun unlockFocus() {
        isFocusing = false
        foucsing++
        Log.i(TAG, "unlockFocus")
    }

    fun restFoucs() {
        foucsing = 1
    }

    private var mCameraFocusListener: CameraFocusListener? = null

    init {
        mSensorManager = context.getSystemService(Activity.SENSOR_SERVICE) as SensorManager
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) // TYPE_GRAVITY
    }

    interface CameraFocusListener {
        fun onFocus()
    }

    fun setCameraFocusListener(mCameraFocusListener: CameraFocusListener?) {
        this.mCameraFocusListener = mCameraFocusListener
    }

    companion object {
        const val TAG = "SensorControler"
        const val DELEY_DURATION = 500
        private var mInstance: SensorControler? = null
        const val STATUS_NONE = 0
        const val STATUS_STATIC = 1
        const val STATUS_MOVE = 2
        fun getInstance(context: Context): SensorControler? {
            if (mInstance == null) {
                mInstance = SensorControler(context)
            }
            return mInstance
        }
    }
}