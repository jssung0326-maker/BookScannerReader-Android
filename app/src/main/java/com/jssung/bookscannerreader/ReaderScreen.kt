package com.jssung.bookscannerreader

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp

@Composable
fun ReaderScreen(
    book: BookRecord,
    store: LibraryStore,
    speech: SpeechService,
    onBack: () -> Unit
) {
    val pages = book.pages.sortedBy { it.index }
    var query by remember { mutableStateOf("") }

    val results = remember(query, pages.size) {
        if (query.isBlank()) emptyList()
        else pages.filter { it.ocrText.contains(query, ignoreCase = true) }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = onBack) { Text("← 서재") }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("책 전체 검색") },
                modifier = Modifier.weight(1f)
            )
        }

        if (query.isNotBlank()) {
            LazyRow(Modifier.padding(horizontal = 8.dp)) {
                items(results, key = { it.id }) { page ->
                    AssistChip(
                        onClick = { speech.speak(page.ocrText) },
                        label = { Text("${page.index + 1}p") }
                    )
                    Spacer(Modifier.width(6.dp))
                }
            }
        }

        LazyColumn(Modifier.fillMaxSize()) {
            items(pages, key = { it.id }) { page ->
                Card(Modifier.fillMaxWidth().padding(8.dp)) {
                    Column(Modifier.padding(8.dp)) {
                        val bitmap = remember(page.imagePath) {
                            BitmapFactory.decodeFile(page.imagePath)
                        }

                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Text("${page.index + 1}페이지 · 품질 ${page.qualityScore}")

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = {
                                    page.bookmarked = !page.bookmarked
                                    store.save()
                                }
                            ) {
                                Text(if (page.bookmarked) "★ 북마크" else "☆ 북마크")
                            }

                            TextButton(onClick = { speech.speak(page.ocrText) }) {
                                Text("읽어주기")
                            }
                        }

                        OutlinedTextField(
                            value = page.note,
                            onValueChange = {
                                page.note = it
                                store.save()
                            },
                            label = { Text("메모") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
