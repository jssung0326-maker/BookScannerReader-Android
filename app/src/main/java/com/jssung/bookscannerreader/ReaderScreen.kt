package com.jssung.bookscannerreader
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
@Composable fun ReaderScreen(book:BookRecord,store:LibraryStore,speech:SpeechService,onBack:()->Unit){var q by remember{mutableStateOf("")};val pages=book.pages.sortedBy{it.index};val results=if(q.isBlank())emptyList() else pages.filter{it.ocrText.contains(q,true)};Column(Modifier.fillMaxSize()){Row(Modifier.padding(8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedButton(onClick=onBack){Text("← 서재")};OutlinedTextField(value=q,onValueChange={q=it},label={Text("책 전체 검색")},modifier=Modifier.weight(1f))};if(q.isNotBlank())LazyRow(Modifier.padding(horizontal=8.dp)){items(results){p->AssistChip(onClick={speech.speak(p.ocrText)},label={Text("${p.index+1}p")});Spacer(Modifier.width(6.dp))}};LazyColumn{items(pages,key={it.id}){p->Card(Modifier.fillMaxWidth().padding(8.dp)){Column(Modifier.padding(8.dp)){val b=remember(p.imagePath){BitmapFactory.decodeFile(p.imagePath)};if(b!=null)Image(b.asImageBitmap(),null,Modifier.fillMaxWidth());Text("${p.index+1}페이지 · 품질 ${p.qualityScore}");Row{TextButton(onClick={p.bookmarked=!p.bookmarked;store.save()}){Text(if(p.bookmarked)"★ 북마크" else "☆ 북마크")};TextButton(onClick={speech.speak(p.ocrText)}){Text("읽어주기")}};OutlinedTextField(value=p.note,onValueChange={p.note=it;store.save()},label={Text("메모")},modifier=Modifier.fillMaxWidth())}}}}}
