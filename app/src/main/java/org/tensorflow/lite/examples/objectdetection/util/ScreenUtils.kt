package org.tensorflow.lite.examples.objectdetection.util

import android.content.Context


object ScreenUtils {
    /**
     * 获取屏幕宽度（px）
     *
     * @param context
     * @return
     */
    fun getScreenWidth(context: Context): Int {
        return context.resources.displayMetrics.widthPixels
    }

    /**
     * 获取屏幕高度（px）
     *
     * @param context
     * @return
     */
    fun getScreenHeight(context: Context): Int {
        return context.resources.displayMetrics.heightPixels
    }
}