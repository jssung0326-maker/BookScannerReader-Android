package com.jssung.bookscannerreader
import android.graphics.BitmapFactory
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

@Composable fun CameraScreen(book:BookRecord,store:LibraryStore,onDone:()->Unit){
 val ctx=LocalContext.current; val scope=rememberCoroutineScope(); var capture by remember{mutableStateOf<ImageCapture?>(null)}; var status by remember{mutableStateOf("카메라 준비 중")}; var split by remember{mutableFloatStateOf(book.profile.splitRatio)}
 Column(Modifier.fillMaxSize()){
  AndroidView(modifier=Modifier.weight(1f).fillMaxWidth(),factory={c-> val v=PreviewView(c); val f=ProcessCameraProvider.getInstance(c); f.addListener({val p=f.get();val pr=Preview.Builder().build().also{it.setSurfaceProvider(v.surfaceProvider)}; val ic=ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build();capture=ic;p.unbindAll();p.bindToLifecycle(c as androidx.lifecycle.LifecycleOwner,CameraSelector.DEFAULT_BACK_CAMERA,pr,ic);status="촬영 준비 완료"},ContextCompat.getMainExecutor(c));v})
  if(book.profile.scanMode==ScanMode.SPREAD){Text("2페이지 분리선 ${(split*100).toInt()}%");Slider(value=split,onValueChange={split=it},valueRange=.35f..65f/100f)}
  Text(status,Modifier.padding(8.dp)); Row(Modifier.fillMaxWidth().padding(8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
   Button(modifier=Modifier.weight(1f),onClick={val ic=capture?:return@Button;val raw=File(ctx.cacheDir,"raw_${System.currentTimeMillis()}.jpg");val opt=ImageCapture.OutputFileOptions.Builder(raw).build();ic.takePicture(opt,ContextCompat.getMainExecutor(ctx),object:ImageCapture.OnImageSavedCallback{override fun onImageSaved(r:ImageCapture.OutputFileResults){scope.launch{val bm=BitmapFactory.decodeFile(raw.absolutePath);val parts=if(book.profile.scanMode==ScanMode.SPREAD){val q=ImageProcessing.splitSpread(bm,split);if(book.profile.readingDirection==ReadingDirection.LEFT_TO_RIGHT)listOf(q.first,q.second)else listOf(q.second,q.first)}else listOf(bm);parts.forEach{part->val n=ImageProcessing.normalize(part,book.profile.targetWidth,book.profile.targetHeight);val score=ImageProcessing.qualityScore(n);val last=book.pages.lastOrNull();if(last!=null){val prev=BitmapFactory.decodeFile(last.imagePath);if(prev!=null&&ImageProcessing.hamming(ImageProcessing.hash(prev),ImageProcessing.hash(n))<=4){status="중복 페이지 제외";return@forEach}};val dir=File(ctx.filesDir,"books/${book.id}/pages").apply{mkdirs()};val out=File(dir,"page_${book.pages.size+1}.jpg");out.outputStream().use{n.compress(android.graphics.Bitmap.CompressFormat.JPEG,92,it)};val text=runCatching{OcrService.recognize(n)}.getOrDefault("");book.pages.add(PageRecord(index=book.pages.size,imagePath=out.absolutePath,ocrText=text,qualityScore=score))};store.save();runCatching{PdfService.createPdf(ctx,book)};status="${book.pages.size}페이지 저장 완료"}};override fun onError(e:ImageCaptureException){status="촬영 오류: ${e.message}"}})} ){Text("촬영")}; OutlinedButton(onClick=onDone){Text("완료")}
  }
 }
}
