package com.jssung.bookscannerreader
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
class MainActivity:ComponentActivity(){lateinit var store:LibraryStore;lateinit var speech:SpeechService;override fun onCreate(s:Bundle?){super.onCreate(s);store=LibraryStore(this);speech=SpeechService(this);val req=registerForActivityResult(ActivityResultContracts.RequestPermission()){};if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)req.launch(Manifest.permission.CAMERA);setContent{MaterialTheme{var sel by remember{mutableStateOf<BookRecord?>(null)};var scan by remember{mutableStateOf(false)};var read by remember{mutableStateOf(false)};when{sel!=null&&scan->CameraScreen(sel!!,store){scan=false};sel!=null&&read->ReaderScreen(sel!!,store,speech){read=false;sel=null};else->LibraryScreen(store.books.toList(),onNew={val b=BookRecord(title="새 책 ${store.books.size+1}");store.addBook(b);sel=b;scan=true},onScan={sel=it;scan=true},onRead={sel=it;read=true},onDelete={store.deleteBook(it)})}}}};override fun onDestroy(){speech.shutdown();super.onDestroy()}}
@Composable fun LibraryScreen(books:List<BookRecord>,onNew:()->Unit,onScan:(BookRecord)->Unit,onRead:(BookRecord)->Unit,onDelete:(BookRecord)->Unit){Scaffold(floatingActionButton={FloatingActionButton(onClick=onNew){Icon(Icons.Default.Add,"새 책")}}){pad->Column(Modifier.padding(pad).padding(12.dp)){Text("Book Scanner Reader",style=MaterialTheme.typography.headlineMedium);Text("Android v1.0-alpha · Galaxy Note8 테스트용");Spacer(Modifier.height(12.dp));if(books.isEmpty())Text("+ 버튼을 눌러 첫 책을 만들어 주세요.") else LazyColumn{items(books,key={it.id}){b->Card(Modifier.fillMaxWidth().padding(vertical=6.dp)){Column(Modifier.padding(12.dp)){Text(b.title,style=MaterialTheme.typography.titleMedium);Text("${b.pages.size}페이지");Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick={onScan(b)}){Text("스캔")};OutlinedButton(onClick={onRead(b)}){Text("읽기")};TextButton(onClick={onDelete(b)}){Text("삭제")}}}}}}}}}
