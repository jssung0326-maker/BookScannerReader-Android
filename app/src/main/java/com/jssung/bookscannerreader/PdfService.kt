package com.jssung.bookscannerreader
import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import java.io.*
object PdfService {
 fun createPdf(ctx:Context,book:BookRecord):File{ val dir=File(ctx.filesDir,"books/${book.id}").apply{mkdirs()}; val out=File(dir,"book.pdf"); val doc=PdfDocument(); book.pages.sortedBy{it.index}.forEachIndexed{idx,p-> val b=BitmapFactory.decodeFile(p.imagePath)?:return@forEachIndexed; val info=PdfDocument.PageInfo.Builder(b.width,b.height,idx+1).create(); val pg=doc.startPage(info); pg.canvas.drawColor(Color.WHITE); pg.canvas.drawBitmap(b,0f,0f,Paint()); doc.finishPage(pg)}; FileOutputStream(out).use{doc.writeTo(it)}; doc.close(); book.pdfPath=out.absolutePath; return out }
}
