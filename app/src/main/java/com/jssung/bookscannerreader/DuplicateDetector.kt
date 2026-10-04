package com.jssung.bookscannerreader

import android.graphics.Bitmap
import android.graphics.BitmapFactory

object DuplicateDetector {

    fun findDuplicate(book: BookRecord, newText: String, newBitmap: Bitmap): PageRecord? {
        val newNorm = normalizeText(newText)
        val newHash = ImageProcessing.perceptualHash(newBitmap)

        // 책 전체에서 찾되 최근 페이지부터 검사한다.
        return book.pages.asReversed().firstOrNull { page ->
            val oldNorm = normalizeText(page.ocrText)
            val textMatch = if (newNorm.length >= 20 && oldNorm.length >= 20) {
                similarity(newNorm, oldNorm) >= 0.78
            } else false

            val imageMatch = BitmapFactory.decodeFile(page.imagePath)?.let { oldBitmap ->
                ImageProcessing.hammingDistance(
                    ImageProcessing.perceptualHash(oldBitmap),
                    newHash
                ) <= 5
            } ?: false

            textMatch || imageMatch
        }
    }

    private fun normalizeText(s: String): String =
        s.lowercase()
            .replace(Regex("\\s+"), "")
            .replace(Regex("[^0-9a-z가-힣]"), "")

    private fun similarity(a: String, b: String): Double {
        if (a.isBlank() || b.isBlank()) return 0.0
        val gram = 3
        val sa = shingles(a, gram)
        val sb = shingles(b, gram)
        if (sa.isEmpty() || sb.isEmpty()) return 0.0
        val intersection = sa.intersect(sb).size.toDouble()
        val union = sa.union(sb).size.toDouble()
        return if (union == 0.0) 0.0 else intersection / union
    }

    private fun shingles(s: String, n: Int): Set<String> {
        if (s.length < n) return setOf(s)
        return (0..s.length - n).map { s.substring(it, it + n) }.toSet()
    }
}
