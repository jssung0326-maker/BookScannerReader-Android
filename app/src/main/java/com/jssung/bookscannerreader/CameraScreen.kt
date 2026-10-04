package com.jssung.bookscannerreader

import android.graphics.Bitmap
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors

@Composable
fun CameraScreen(
    book: BookRecord,
    store: LibraryStore,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var overlayView by remember { mutableStateOf<PageDetectionOverlayView?>(null) }
    var status by remember { mutableStateOf("카메라 준비 중") }
    var autoMode by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        onDispose { analyzerExecutor.shutdown() }
    }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FIT_CENTER
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE

                        val providerFuture = ProcessCameraProvider.getInstance(ctx)
                        providerFuture.addListener({
                            val provider = providerFuture.get()

                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(surfaceProvider)
                            }

                            val capture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()
                            imageCapture = capture

                            val analysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            analysis.setAnalyzer(analyzerExecutor) { image ->
                                try {
                                    if (autoMode) {
                                        val detected = AutoPageDetector.detect(image)
                                        if (detected != null) {
                                            post {
                                                overlayView?.updateDetection(
                                                    detected.left,
                                                    detected.right
                                                )
                                            }
                                        }
                                    }
                                } finally {
                                    image.close()
                                }
                            }

                            provider.unbindAll()
                            provider.bindToLifecycle(
                                ctx as androidx.lifecycle.LifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                capture,
                                analysis
                            )
                            status = "책 외곽 자동 인식 중"
                        }, ContextCompat.getMainExecutor(ctx))
                    }
                }
            )

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PageDetectionOverlayView(ctx).also {
                        overlayView = it
                    }
                }
            )
        }

        Text(
            "초록 외곽선 = 저장될 페이지 · 흰 점을 끌면 수동 보정",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = autoMode,
                onClick = {
                    autoMode = !autoMode
                    if (autoMode) overlayView?.resetAuto()
                },
                label = { Text(if (autoMode) "자동 외곽선 ON" else "수동 외곽선") }
            )

            OutlinedButton(
                onClick = {
                    autoMode = true
                    overlayView?.resetAuto()
                    status = "외곽선 다시 탐색 중"
                }
            ) {
                Text("자동 재탐지")
            }
        }

        Text(status, Modifier.padding(8.dp))

        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    val capture = imageCapture ?: return@Button
                    val geometry = overlayView?.snapshot() ?: return@Button
                    val raw = File(context.cacheDir, "raw_${System.currentTimeMillis()}.jpg")
                    val options = ImageCapture.OutputFileOptions.Builder(raw).build()

                    capture.takePicture(
                        options,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                                scope.launch {
                                    status = "좌·우 페이지 처리 중..."
                                    val full = ImageProcessing.decodeOriented(raw)
                                    if (full == null) {
                                        status = "사진을 읽지 못했습니다."
                                        return@launch
                                    }

                                    val quads = if (book.profile.scanMode == ScanMode.SPREAD) {
                                        if (book.profile.readingDirection == ReadingDirection.LEFT_TO_RIGHT)
                                            listOf(geometry.first, geometry.second)
                                        else listOf(geometry.second, geometry.first)
                                    } else {
                                        // 1페이지 모드에서는 더 큰 쪽 프레임을 사용
                                        listOf(geometry.first)
                                    }

                                    var replaced = 0
                                    var added = 0

                                    quads.forEach { quad ->
                                        val cropped = runCatching {
                                            ImageProcessing.perspectiveCrop(full, quad)
                                        }.getOrElse {
                                            status = "페이지 외곽 보정 실패"
                                            return@forEach
                                        }

                                        val normalized = ImageProcessing.normalize(
                                            cropped,
                                            book.profile.targetWidth,
                                            book.profile.targetHeight
                                        )

                                        val quality = ImageProcessing.qualityScore(normalized)
                                        val text = runCatching {
                                            OcrService.recognize(normalized)
                                        }.getOrDefault("")

                                        val duplicate = DuplicateDetector.findDuplicate(
                                            book = book,
                                            newText = text,
                                            newBitmap = normalized
                                        )

                                        if (duplicate != null) {
                                            // 사용자의 요구: 같은 페이지는 나중 촬영본으로 무조건 교체
                                            val out = File(duplicate.imagePath)
                                            out.parentFile?.mkdirs()
                                            out.outputStream().use {
                                                normalized.compress(Bitmap.CompressFormat.JPEG, 94, it)
                                            }
                                            duplicate.ocrText = text
                                            duplicate.qualityScore = quality
                                            replaced++
                                        } else {
                                            val dir = File(
                                                context.filesDir,
                                                "books/${book.id}/pages"
                                            ).apply { mkdirs() }

                                            val out = File(
                                                dir,
                                                "page_${System.currentTimeMillis()}_${book.pages.size + 1}.jpg"
                                            )
                                            out.outputStream().use {
                                                normalized.compress(Bitmap.CompressFormat.JPEG, 94, it)
                                            }

                                            book.pages.add(
                                                PageRecord(
                                                    index = book.pages.size,
                                                    imagePath = out.absolutePath,
                                                    ocrText = text,
                                                    qualityScore = quality
                                                )
                                            )
                                            added++
                                        }
                                    }

                                    book.pages.sortedBy { it.index }.forEachIndexed { i, p ->
                                        p.index = i
                                    }

                                    store.save()
                                    runCatching { PdfService.createPdf(context, book) }

                                    status = buildString {
                                        append("${book.pages.size}페이지 저장")
                                        if (added > 0) append(" · 새 페이지 ${added}장")
                                        if (replaced > 0) append(" · 중복 ${replaced}장 최신 촬영본으로 교체")
                                    }
                                }
                            }

                            override fun onError(exc: ImageCaptureException) {
                                status = "촬영 오류: ${exc.message}"
                            }
                        }
                    )
                }
            ) {
                Text("스캔 촬영")
            }

            OutlinedButton(onClick = onDone) {
                Text("완료")
            }
        }
    }
}
