package com.jssung.bookscannerreader

import android.graphics.*
import androidx.exifinterface.media.ExifInterface
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object ImageProcessing {

    fun decodeOriented(file: File): Bitmap? {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null
        val exif = runCatching { ExifInterface(file) }.getOrNull()
        val rotation = when (exif?.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (rotation == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(rotation) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun perspectiveCrop(bitmap: Bitmap, quad: NormalizedQuad): Bitmap {
        val src = FloatArray(8)
        quad.points.forEachIndexed { i, p ->
            src[i * 2] = p.x.coerceIn(0f, 1f) * bitmap.width
            src[i * 2 + 1] = p.y.coerceIn(0f, 1f) * bitmap.height
        }

        fun dist(ax: Float, ay: Float, bx: Float, by: Float): Float =
            sqrt((ax - bx) * (ax - bx) + (ay - by) * (ay - by))

        val top = dist(src[0], src[1], src[2], src[3])
        val right = dist(src[2], src[3], src[4], src[5])
        val bottom = dist(src[4], src[5], src[6], src[7])
        val left = dist(src[6], src[7], src[0], src[1])

        val outW = max(320, max(top, bottom).toInt())
        val outH = max(480, max(left, right).toInt())

        val dst = floatArrayOf(
            0f, 0f,
            outW.toFloat(), 0f,
            outW.toFloat(), outH.toFloat(),
            0f, outH.toFloat()
        )

        val matrix = Matrix()
        val ok = matrix.setPolyToPoly(src, 0, dst, 0, 4)
        if (!ok) {
            val minX = quad.points.minOf { it.x }.coerceIn(0f, 1f)
            val maxX = quad.points.maxOf { it.x }.coerceIn(0f, 1f)
            val minY = quad.points.minOf { it.y }.coerceIn(0f, 1f)
            val maxY = quad.points.maxOf { it.y }.coerceIn(0f, 1f)
            return Bitmap.createBitmap(
                bitmap,
                (minX * bitmap.width).toInt(),
                (minY * bitmap.height).toInt(),
                max(1, ((maxX - minX) * bitmap.width).toInt()),
                max(1, ((maxY - minY) * bitmap.height).toInt())
            )
        }

        // setPolyToPoly is src→dst. Canvas needs bitmap transform in that same direction.
        val output = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        Canvas(output).apply {
            drawColor(Color.WHITE)
            drawBitmap(bitmap, matrix, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        }
        return output
    }

    fun splitSpread(bitmap: Bitmap, ratio: Float): Pair<Bitmap, Bitmap> {
        val split = (bitmap.width * ratio.coerceIn(0.25f, 0.75f)).toInt()
        val left = Bitmap.createBitmap(bitmap, 0, 0, split, bitmap.height)
        val right = Bitmap.createBitmap(bitmap, split, 0, bitmap.width - split, bitmap.height)
        return left to right
    }

    fun normalize(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val scale = min(targetWidth.toFloat() / bitmap.width, targetHeight.toFloat() / bitmap.height)
        val newW = max(1, (bitmap.width * scale).toInt())
        val newH = max(1, (bitmap.height * scale).toInt())
        val scaled = Bitmap.createScaledBitmap(bitmap, newW, newH, true)
        val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.WHITE)
        val x = (targetWidth - newW) / 2f
        val y = (targetHeight - newH) / 2f
        canvas.drawBitmap(scaled, x, y, Paint(Paint.ANTI_ALIAS_FLAG))
        return output
    }

    fun rotate90(bitmap: Bitmap, clockwise: Boolean = true): Bitmap {
        val matrix = Matrix().apply { postRotate(if (clockwise) 90f else -90f) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun qualityScore(bitmap: Bitmap): Int {
        val sampleW = min(bitmap.width, 320)
        val sampleH = min(bitmap.height, 480)
        val sample = Bitmap.createScaledBitmap(bitmap, sampleW, sampleH, true)
        var sum = 0.0
        var sumSq = 0.0
        var edge = 0.0
        var count = 0
        var prev = 0.0
        for (y in 0 until sample.height step 4) {
            for (x in 0 until sample.width step 4) {
                val c = sample.getPixel(x, y)
                val l = 0.299 * Color.red(c) + 0.587 * Color.green(c) + 0.114 * Color.blue(c)
                sum += l
                sumSq += l * l
                if (count > 0) edge += abs(l - prev)
                prev = l
                count++
            }
        }
        if (count == 0) return 0
        val mean = sum / count
        val variance = max(0.0, sumSq / count - mean * mean)
        val exposure = 100.0 - abs(mean - 145.0) / 145.0 * 100.0
        val sharp = min(100.0, edge / count * 4.0)
        val contrast = min(100.0, variance / 35.0)
        return (0.35 * exposure.coerceIn(0.0, 100.0) +
                0.45 * sharp + 0.20 * contrast).toInt().coerceIn(0, 100)
    }

    fun perceptualHash(bitmap: Bitmap): Long {
        val small = Bitmap.createScaledBitmap(bitmap, 8, 8, true)
        val values = DoubleArray(64)
        var avg = 0.0
        var i = 0
        for (y in 0 until 8) for (x in 0 until 8) {
            val c = small.getPixel(x, y)
            val v = 0.299 * Color.red(c) + 0.587 * Color.green(c) + 0.114 * Color.blue(c)
            values[i++] = v
            avg += v
        }
        avg /= 64.0
        var hash = 0L
        for (j in values.indices) if (values[j] >= avg) hash = hash or (1L shl j)
        return hash
    }

    fun hammingDistance(a: Long, b: Long): Int = java.lang.Long.bitCount(a xor b)
}
