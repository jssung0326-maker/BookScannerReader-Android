package com.jssung.bookscannerreader
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
object OcrService {
 private val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
 suspend fun recognize(b:Bitmap):String=suspendCancellableCoroutine{ c -> recognizer.process(InputImage.fromBitmap(b,0)).addOnSuccessListener{c.resume(it.text)}.addOnFailureListener{c.resumeWithException(it)} }
}
