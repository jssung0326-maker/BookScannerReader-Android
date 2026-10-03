package com.jssung.bookscannerreader
import java.util.UUID

enum class ScanMode { SINGLE, SPREAD }
enum class ReadingDirection { LEFT_TO_RIGHT, RIGHT_TO_LEFT }
data class ScanProfile(
 val scanMode: ScanMode = ScanMode.SPREAD,
 val targetWidth: Int = 1600,
 val targetHeight: Int = 2400,
 val splitRatio: Float = 0.5f,
 val readingDirection: ReadingDirection = ReadingDirection.LEFT_TO_RIGHT,
 val autoCapture: Boolean = false,
 val qualityThreshold: Int = 65,
 val dewarpStrength: Float = 0.25f
)
data class PageRecord(
 val id:String=UUID.randomUUID().toString(), var index:Int, var imagePath:String,
 var ocrText:String="", var qualityScore:Int=0, var note:String="", var bookmarked:Boolean=false
)
data class BookRecord(
 val id:String=UUID.randomUUID().toString(), var title:String, var author:String="",
 var pages:MutableList<PageRecord> = mutableListOf(), var profile:ScanProfile=ScanProfile(),
 var lastReadPage:Int=0, var pdfPath:String?=null, var createdAt:Long=System.currentTimeMillis()
)
