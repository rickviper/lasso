package com.lasso.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * Phase 3 freehand selection surface: tracks one finger, renders a smoothed
 * path, keeps the stroke visible after release. A new touch starts a new
 * (replacement) selection.
 */
class SelectionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 8f * resources.displayMetrics.density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val path = Path()
    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var lastX = 0f
    private var lastY = 0f
    private var hasStroke = false

    fun hasSelection(): Boolean = hasStroke

    fun selectionBounds(): RectF? {
        if (!hasStroke) return null
        val bounds = RectF()
        path.computeBounds(bounds, true)
        return bounds
    }

    fun clear() {
        path.reset()
        hasStroke = false
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = event.getPointerId(0)
                lastX = event.x
                lastY = event.y
                path.reset()
                path.moveTo(lastX, lastY)
                hasStroke = true
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val index = event.findPointerIndex(activePointerId)
                if (index < 0) return false
                val x = event.getX(index)
                val y = event.getY(index)
                // Midpoint quadratic smoothing.
                path.quadTo(lastX, lastY, (lastX + x) / 2f, (lastY + y) / 2f)
                lastX = x
                lastY = y
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val index = event.findPointerIndex(activePointerId)
                if (index >= 0 && event.actionMasked == MotionEvent.ACTION_UP) {
                    path.lineTo(event.getX(index), event.getY(index))
                    invalidate()
                }
                activePointerId = MotionEvent.INVALID_POINTER_ID
                // Stroke stays visible after release.
                return true
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (event.getPointerId(event.actionIndex) == activePointerId) {
                    activePointerId = MotionEvent.INVALID_POINTER_ID
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (hasStroke) {
            canvas.drawPath(path, strokePaint)
        }
    }
}