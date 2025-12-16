package com.example.gallery2

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewParent
import androidx.appcompat.widget.AppCompatImageView

class TouchImageView : AppCompatImageView {
    private val matrix = Matrix()
    private var mode = NONE

    private val last = PointF()
    private val start = PointF()
    private var minScale = 1f
    private var maxScale = 5f
    private lateinit var m: FloatArray

    private var redundantXSpace = 0f
    private var redundantYSpace = 0f
    private var width = 0f
    private var height = 0f
    private var saveScale = 1f
    private var right = 0f
    private var bottom = 0f
    private var origWidth = 0f
    private var origHeight = 0f
    private var bmWidth = 0f
    private var bmHeight = 0f

    private lateinit var scaleDetector: ScaleGestureDetector
    private lateinit var gestureDetector: GestureDetector

    constructor(context: Context) : super(context) {
        sharedConstructing(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        sharedConstructing(context)
    }

    private fun sharedConstructing(context: Context) {
        super.setClickable(true)
        scaleDetector = ScaleGestureDetector(context, ScaleListener())
        gestureDetector = GestureDetector(context, GestureListener())
        matrix.setTranslate(1f, 1f)
        m = FloatArray(9)
        imageMatrix = matrix
        scaleType = ScaleType.MATRIX

        setOnTouchListener { _, event ->
            scaleDetector.onTouchEvent(event)
            gestureDetector.onTouchEvent(event)

            matrix.getValues(m)
            val x = m[Matrix.MTRANS_X]
            val y = m[Matrix.MTRANS_Y]
            val curr = PointF(event.x, event.y)

            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    last.set(event.x, event.y)
                    start.set(last)
                    mode = DRAG
                }

                MotionEvent.ACTION_POINTER_DOWN -> {
                    last.set(event.x, event.y)
                    start.set(last)
                    mode = ZOOM
                    // 禁止 ViewPager2 拦截触摸事件
                    disallowParentInterceptTouchEvent()
                }

                MotionEvent.ACTION_MOVE -> {
                    if (mode == DRAG) {
                        var deltaX = curr.x - last.x
                        var deltaY = curr.y - last.y
                        val scaleWidth = (origWidth * saveScale).toInt().toFloat()
                        val scaleHeight = (origHeight * saveScale).toInt().toFloat()

                        // 检查图片是否需要水平或垂直滚动
                        val needHorizontalScroll = scaleWidth > width
                        val needVerticalScroll = scaleHeight > height

                        if (needHorizontalScroll || needVerticalScroll) {
                            // 只有在需要水平滚动时才禁止ViewPager2拦截
                            if (needHorizontalScroll) {
                                disallowParentInterceptTouchEvent()
                            }

                            when {
                                scaleWidth < width -> {
                                    deltaX = 0f
                                    if (y + deltaY > 0)
                                        deltaY = -y
                                    else if (y + deltaY < -bottom)
                                        deltaY = -(y + bottom)
                                }
                                scaleHeight < height -> {
                                    deltaY = 0f
                                    if (x + deltaX > 0)
                                        deltaX = -x
                                    else if (x + deltaX < -right)
                                        deltaX = -(x + right)
                                }
                                else -> {
                                    // 图片同时需要水平和垂直滚动
                                    if (x + deltaX > 0)
                                        deltaX = -x
                                    else if (x + deltaX < -right)
                                        deltaX = -(x + right)

                                    if (y + deltaY > 0)
                                        deltaY = -y
                                    else if (y + deltaY < -bottom)
                                        deltaY = -(y + bottom)
                                }
                            }

                            matrix.postTranslate(deltaX, deltaY)
                            last.set(curr.x, curr.y)
                        }
                    } else if (mode == ZOOM) {
                        disallowParentInterceptTouchEvent()
                    }
                }

                MotionEvent.ACTION_UP -> {
                    mode = NONE
                    allowParentInterceptTouchEvent()
                    val xDiff = Math.abs(curr.x - start.x).toInt()
                    val yDiff = Math.abs(curr.y - start.y).toInt()
                    if (xDiff < 3 && yDiff < 3)
                        performClick()
                }

                MotionEvent.ACTION_POINTER_UP -> {
                    mode = NONE
                    allowParentInterceptTouchEvent()
                }
            }

            imageMatrix = matrix
            true
        }
    }

    private fun disallowParentInterceptTouchEvent() {
        parent?.requestDisallowInterceptTouchEvent(true)
    }

    private fun allowParentInterceptTouchEvent() {
        parent?.requestDisallowInterceptTouchEvent(false)
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        width = MeasureSpec.getSize(widthMeasureSpec).toFloat()
        height = MeasureSpec.getSize(heightMeasureSpec).toFloat()

        // Fit width to screen, align to top
        val scaleX = width / bmWidth
        val scale = scaleX // 使用宽度缩放比例
        matrix.setScale(scale, scale)

        // Align to top (no vertical centering)
        redundantYSpace = 0f // 顶部对齐，不需要垂直偏移
        redundantXSpace = 0f // 宽度已经完全填充，不需要水平偏移

        matrix.postTranslate(redundantXSpace, redundantYSpace)

        // 计算图片实际显示的尺寸
        origWidth = scale * bmWidth
        origHeight = scale * bmHeight
        right = origWidth * saveScale - width
        bottom = origHeight * saveScale - height

        imageMatrix = matrix
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            mode = ZOOM
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            var scaleFactor = detector.scaleFactor
            val origScale = saveScale
            saveScale *= scaleFactor

            if (saveScale > maxScale) {
                saveScale = maxScale
                scaleFactor = maxScale / origScale
            } else if (saveScale < minScale) {
                saveScale = minScale
                scaleFactor = minScale / origScale
            }

            right = width * saveScale - width - (2 * redundantXSpace * saveScale)
            bottom = height * saveScale - height - (2 * redundantYSpace * saveScale)

            if (origWidth * saveScale <= width || origHeight * saveScale <= height) {
                matrix.postScale(scaleFactor, scaleFactor, width / 2, height / 2)
                if (scaleFactor < 1) {
                    matrix.getValues(m)
                    val x = m[Matrix.MTRANS_X]
                    val y = m[Matrix.MTRANS_Y]

                    if (scaleFactor < 1) {
                        if ((origWidth * saveScale).toInt() < width.toInt()) {
                            if (y < -bottom)
                                matrix.postTranslate(0f, -(y + bottom))
                            else if (y > 0)
                                matrix.postTranslate(0f, -y)
                        } else {
                            if (x < -right)
                                matrix.postTranslate(-(x + right), 0f)
                            else if (x > 0)
                                matrix.postTranslate(-x, 0f)
                        }
                    }
                }
            } else {
                matrix.postScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
                matrix.getValues(m)
                val x = m[Matrix.MTRANS_X]
                val y = m[Matrix.MTRANS_Y]

                if (scaleFactor < 1) {
                    if (x < -right)
                        matrix.postTranslate(-(x + right), 0f)
                    else if (x > 0)
                        matrix.postTranslate(-x, 0f)

                    if (y < -bottom)
                        matrix.postTranslate(0f, -(y + bottom))
                    else if (y > 0)
                        matrix.postTranslate(0f, -y)
                }
            }
            return true
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            // Double tap to zoom
            if (saveScale == 1f) {
                matrix.postScale(2f, 2f, e.x, e.y)
                saveScale = 2f
            } else {
                matrix.setScale(1f, 1f)
                matrix.postTranslate(redundantXSpace, redundantYSpace)
                saveScale = 1f
            }

            imageMatrix = matrix
            invalidate()
            return true
        }
    }

    fun setImageBitmap(bmWidth: Int, bmHeight: Int) {
        this.bmWidth = bmWidth.toFloat()
        this.bmHeight = bmHeight.toFloat()
    }

    companion object {
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
    }
}
