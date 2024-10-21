/*
 * Copyright 2022 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.lite.examples.objectdetection

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.util.LinkedList
import kotlin.math.max
import org.tensorflow.lite.task.vision.detector.Detection
import java.util.HashSet

/**
 * 前45度
 */
class OverlayCarFront45View(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var results: List<Detection> = LinkedList<Detection>()
    private var boxPaint = Paint()
    private var tipBoxPaint = Paint()
    private var textBackgroundPaint = Paint()
    private var textPaint = Paint()
    private var textTipPaint = Paint()
    private var scaleFactor: Float = 1f

    private var bounds = Rect()

    init {
        initPaints()
    }

    fun clear() {
        textPaint.reset()
        textBackgroundPaint.reset()
        boxPaint.reset()
        tipBoxPaint.reset()
        textTipPaint.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {
        textBackgroundPaint.color = Color.BLACK
        textBackgroundPaint.style = Paint.Style.FILL
        textBackgroundPaint.textSize = 50f
        textBackgroundPaint.alpha = 128

        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 50f

        textTipPaint.color = Color.WHITE
        textTipPaint.style = Paint.Style.FILL
        textTipPaint.textSize = 80f

        boxPaint.color = ContextCompat.getColor(context!!, R.color.bounding_box_color)
        boxPaint.strokeWidth = 8F
        boxPaint.style = Paint.Style.STROKE

        tipBoxPaint.color = ContextCompat.getColor(context!!, R.color.bounding_box_tip_color)
        tipBoxPaint.strokeWidth = 8F
        tipBoxPaint.style = Paint.Style.STROKE

    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)


        val dataSet = HashSet<String>()

        //查看整个识别数据的边界
        var targetTop = 200f
        var targetBottom = 0f
        var targetLeft = 400f
        var targetRight = 0f


        for (result in results) {
            val boundingBox = result.boundingBox
            dataSet.add(result.categories[0].label)

            targetTop = minOf(targetTop, boundingBox.top)
            targetBottom = maxOf(targetBottom, boundingBox.bottom)
            targetLeft = minOf(targetLeft, boundingBox.left)
            targetRight = maxOf(targetRight, boundingBox.right)

            val top = boundingBox.top * scaleFactor
            val bottom = boundingBox.bottom * scaleFactor
            val left = boundingBox.left * scaleFactor
            val right = boundingBox.right * scaleFactor


            // Draw bounding box around detected objects
            val drawableRect = RectF(left, top, right, bottom)
            canvas.drawRect(drawableRect, boxPaint)

            // Create text to display alongside detected objects
            val drawableText =
                result.categories[0].label + " " + String.format("%.2f", result.categories[0].score)

            // Draw rect behind display text
            textBackgroundPaint.getTextBounds(drawableText, 0, drawableText.length, bounds)
            val textWidth = bounds.width()
            val textHeight = bounds.height()
            val rightPoint = left + textWidth + Companion.BOUNDING_RECT_TEXT_PADDING
            val bottomPoint = top + textHeight + Companion.BOUNDING_RECT_TEXT_PADDING
            canvas.drawRect(
                left,
                top,
                rightPoint,
                bottomPoint,
                textBackgroundPaint
            )
            // Draw text for detected object
            canvas.drawText(drawableText, left, top + bounds.height(), textPaint)
        }


        if (results.isNotEmpty()) {
            drawOneRect(targetTop, targetBottom, targetLeft, targetRight, canvas)
            drawOneRect(10f, 280f, 80f, 580f, canvas)
        }



    }


    fun setResults(
        detectionResults: MutableList<Detection>,
        imageHeight: Int,
        imageWidth: Int,
    ) {
        results = detectionResults

        // PreviewView is in FILL_START mode. So we need to scale up the bounding box to match with
        // the size that the captured images will be displayed.
        scaleFactor = max(width * 1f / imageWidth, height * 1f / imageHeight)
        Log.i(
            "overLayView",
            " scaleFactor $scaleFactor , width：$width, height:$height, imageWith：$imageWidth, imageHeight $imageHeight"
        )
    }

    fun drawOneRect(
        top: Float, bottom: Float, left: Float, right: Float,
        canvas: Canvas
    ) {

        val top = top * scaleFactor
        val bottom = bottom * scaleFactor
        val left = left * scaleFactor
        val right = right * scaleFactor

        val drawableRect = RectF(left, top, right, bottom)
        canvas.drawRect(drawableRect, tipBoxPaint)
    }

    fun drawOneText(
        tipText: String,
        top: Float, bottom: Float,
        canvas: Canvas
    ) {

        val top = top * scaleFactor
        val bottom = bottom * scaleFactor

        canvas.drawText(tipText, top, bottom, textTipPaint)

    }

    companion object {
        private const val BOUNDING_RECT_TEXT_PADDING = 8
    }
}
