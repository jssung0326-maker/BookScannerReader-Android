package com.jssung.bookscannerreader

import android.graphics.PointF
import androidx.camera.core.ImageProxy
import kotlin.math.max
import kotlin.math.min

object AutoPageDetector {
    data class Result(val left: NormalizedQuad, val right: NormalizedQuad)

    fun detect(image: ImageProxy): Result? {
        val plane = image.planes.firstOrNull() ?: return null
        val buffer = plane.buffer
        val width = image.width
        val height = image.height
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        if (width < 32 || height < 32) return null

        val step = max(2, min(width, height) / 120)
        val samples = ArrayList<Int>()
        for (y in 0 until height step step) {
            for (x in 0 until width step step) {
                val index = y * rowStride + x * pixelStride
                if (index < buffer.limit()) samples.add(buffer.get(index).toInt() and 0xff)
            }
        }
        if (samples.isEmpty()) return null
        samples.sort()
        val median = samples[samples.size / 2]
        val upper = samples[(samples.size * 3 / 4).coerceAtMost(samples.lastIndex)]
        val threshold = ((median + upper) / 2 + 8).coerceIn(90, 220)

        fun bbox(x0: Int, x1: Int): FloatArray? {
            var minX = x1
            var minY = height
            var maxX = x0
            var maxY = 0
            var count = 0
            for (y in 0 until height step step) {
                for (x in x0 until x1 step step) {
                    val index = y * rowStride + x * pixelStride
                    if (index >= buffer.limit()) continue
                    val lum = buffer.get(index).toInt() and 0xff
                    if (lum >= threshold) {
                        minX = min(minX, x)
                        maxX = max(maxX, x)
                        minY = min(minY, y)
                        maxY = max(maxY, y)
                        count++
                    }
                }
            }
            if (count < 50 || maxX <= minX || maxY <= minY) return null
            val bw = maxX - minX
            val bh = maxY - minY
            if (bw < (x1 - x0) * 0.35f || bh < height * 0.35f) return null

            // 작은 여백을 안쪽으로 넣어 배경이 페이지에 섞이는 것을 줄인다.
            val padX = (bw * 0.025f).toInt()
            val padY = (bh * 0.025f).toInt()
            return floatArrayOf(
                (minX + padX).toFloat() / width,
                (minY + padY).toFloat() / height,
                (maxX - padX).toFloat() / width,
                (maxY - padY).toFloat() / height
            )
        }

        val mid = width / 2
        val leftBox = bbox(0, mid + width / 12) ?: return null
        val rightBox = bbox(mid - width / 12, width) ?: return null

        fun rectToQuad(b: FloatArray) = NormalizedQuad(
            listOf(
                PointF(b[0], b[1]), PointF(b[2], b[1]),
                PointF(b[2], b[3]), PointF(b[0], b[3])
            )
        )

        var left = rectToQuad(leftBox)
        var right = rectToQuad(rightBox)

        // ImageProxy 픽셀 좌표를 화면 회전 좌표로 변환
        val rotation = image.imageInfo.rotationDegrees
        left = rotate(left, rotation)
        right = rotate(right, rotation)

        // 회전 후 좌/우가 바뀐 경우 x 중심값으로 정렬
        return if (centerX(left) <= centerX(right)) Result(left, right) else Result(right, left)
    }

    private fun centerX(q: NormalizedQuad): Float = q.points.sumOf { it.x.toDouble() }.toFloat() / 4f

    private fun rotate(q: NormalizedQuad, degrees: Int): NormalizedQuad {
        fun t(p: PointF): PointF = when (degrees) {
            90 -> PointF(1f - p.y, p.x)
            180 -> PointF(1f - p.x, 1f - p.y)
            270 -> PointF(p.y, 1f - p.x)
            else -> PointF(p.x, p.y)
        }
        return NormalizedQuad(q.points.map(::t))
    }
}
