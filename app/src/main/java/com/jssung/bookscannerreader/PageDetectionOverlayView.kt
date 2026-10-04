package com.jssung.bookscannerreader

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot

data class NormalizedQuad(
    val points: List<PointF>
) {
    init { require(points.size == 4) }

    fun copyDeep(): NormalizedQuad =
        NormalizedQuad(points.map { PointF(it.x, it.y) })

    companion object {
        fun defaultLeft() = NormalizedQuad(
            listOf(
                PointF(0.06f, 0.12f), PointF(0.48f, 0.12f),
                PointF(0.48f, 0.90f), PointF(0.06f, 0.90f)
            )
        )
        fun defaultRight() = NormalizedQuad(
            listOf(
                PointF(0.52f, 0.12f), PointF(0.94f, 0.12f),
                PointF(0.94f, 0.90f), PointF(0.52f, 0.90f)
            )
        )
    }
}

class PageDetectionOverlayView(context: Context) : View(context) {
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = Color.rgb(80, 220, 120)
    }
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.YELLOW
    }
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }
    private val shadePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x66000000
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 34f
    }

    private var leftQuad = NormalizedQuad.defaultLeft()
    private var rightQuad = NormalizedQuad.defaultRight()
    private var activeSide = -1
    private var activeCorner = -1

    var autoDetectionEnabled: Boolean = true
        private set

    fun resetAuto() {
        autoDetectionEnabled = true
        invalidate()
    }

    fun updateDetection(left: NormalizedQuad, right: NormalizedQuad) {
        if (!autoDetectionEnabled) return
        leftQuad = left.copyDeep()
        rightQuad = right.copyDeep()
        invalidate()
    }

    fun snapshot(): Pair<NormalizedQuad, NormalizedQuad> =
        leftQuad.copyDeep() to rightQuad.copyDeep()

    private fun sx(p: PointF) = p.x * width
    private fun sy(p: PointF) = p.y * height

    private fun pathFor(q: NormalizedQuad): Path = Path().apply {
        moveTo(sx(q.points[0]), sy(q.points[0]))
        for (i in 1..3) lineTo(sx(q.points[i]), sy(q.points[i]))
        close()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 촬영 영역 외부를 어둡게 하여 "스캐너" 화면임을 분명히 표시
        val full = Path().apply { addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW) }
        val pages = Path().apply {
            addPath(pathFor(leftQuad))
            addPath(pathFor(rightQuad))
        }
        canvas.save()
        canvas.clipPath(pages, android.graphics.Region.Op.DIFFERENCE)
        canvas.drawPath(full, shadePaint)
        canvas.restore()

        canvas.drawPath(pathFor(leftQuad), borderPaint)
        canvas.drawPath(pathFor(rightQuad), borderPaint)

        val centerXTop = (sx(leftQuad.points[1]) + sx(rightQuad.points[0])) / 2f
        val centerYTop = (sy(leftQuad.points[1]) + sy(rightQuad.points[0])) / 2f
        val centerXBottom = (sx(leftQuad.points[2]) + sx(rightQuad.points[3])) / 2f
        val centerYBottom = (sy(leftQuad.points[2]) + sy(rightQuad.points[3])) / 2f
        canvas.drawLine(centerXTop, centerYTop, centerXBottom, centerYBottom, centerPaint)

        listOf(leftQuad, rightQuad).forEach { q ->
            q.points.forEach { p -> canvas.drawCircle(sx(p), sy(p), 13f, handlePaint) }
        }

        canvas.drawText("왼쪽 페이지", sx(leftQuad.points[0]) + 12f, sy(leftQuad.points[0]) + 42f, labelPaint)
        canvas.drawText("오른쪽 페이지", sx(rightQuad.points[0]) + 12f, sy(rightQuad.points[0]) + 42f, labelPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val hit = findNearestCorner(event.x, event.y)
                if (hit != null) {
                    activeSide = hit.first
                    activeCorner = hit.second
                    autoDetectionEnabled = false
                    parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (activeSide >= 0 && activeCorner >= 0) {
                    val nx = (event.x / width).coerceIn(0.01f, 0.99f)
                    val ny = (event.y / height).coerceIn(0.01f, 0.99f)
                    val q = if (activeSide == 0) leftQuad else rightQuad
                    val pts = q.points.mapIndexed { idx, p ->
                        if (idx == activeCorner) PointF(nx, ny) else PointF(p.x, p.y)
                    }
                    if (activeSide == 0) leftQuad = NormalizedQuad(pts) else rightQuad = NormalizedQuad(pts)
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activeSide = -1
                activeCorner = -1
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return true
    }

    private fun findNearestCorner(x: Float, y: Float): Pair<Int, Int>? {
        var best: Pair<Int, Int>? = null
        var bestDistance = 64f
        listOf(leftQuad, rightQuad).forEachIndexed { side, q ->
            q.points.forEachIndexed { corner, p ->
                val d = hypot(x - sx(p), y - sy(p))
                if (d < bestDistance) {
                    bestDistance = d
                    best = side to corner
                }
            }
        }
        return best
    }
}
