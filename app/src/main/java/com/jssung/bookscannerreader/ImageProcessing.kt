package com.jssung.bookscannerreader
import android.graphics.*
import kotlin.math.*
object ImageProcessing {
 fun splitSpread(b:Bitmap, ratio:Float):Pair<Bitmap,Bitmap>{ val s=(b.width*ratio.coerceIn(.3f,.7f)).toInt(); return Bitmap.createBitmap(b,0,0,s,b.height) to Bitmap.createBitmap(b,s,0,b.width-s,b.height) }
 fun normalize(b:Bitmap,w:Int,h:Int):Bitmap{ val sc=min(w.toFloat()/b.width,h.toFloat()/b.height); val nw=(b.width*sc).toInt().coerceAtLeast(1); val nh=(b.height*sc).toInt().coerceAtLeast(1); val src=Bitmap.createScaledBitmap(b,nw,nh,true); val out=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888); val c=Canvas(out); c.drawColor(Color.WHITE); c.drawBitmap(src,(w-nw)/2f,(h-nh)/2f,Paint(Paint.ANTI_ALIAS_FLAG)); return out }
 fun rotate90(b:Bitmap,cw:Boolean=true):Bitmap{ val m=Matrix().apply{postRotate(if(cw)90f else -90f)}; return Bitmap.createBitmap(b,0,0,b.width,b.height,m,true) }
 fun qualityScore(b:Bitmap):Int{ val s=Bitmap.createScaledBitmap(b,min(256,b.width),min(384,b.height),true); var sum=0.0; var sum2=0.0; var edge=0.0; var n=0; var prev=0.0; for(y in 0 until s.height step 4)for(x in 0 until s.width step 4){ val c=s.getPixel(x,y); val l=.299*Color.red(c)+.587*Color.green(c)+.114*Color.blue(c); sum+=l; sum2+=l*l; if(n>0) edge+=abs(l-prev); prev=l; n++ }; if(n==0)return 0; val mean=sum/n; val varc=max(0.0,sum2/n-mean*mean); val exposure=(100-abs(mean-145)/145*100).coerceIn(0.0,100.0); val sharp=min(100.0,edge/n*4); val contrast=min(100.0,varc/35); return (.35*exposure+.45*sharp+.20*contrast).toInt().coerceIn(0,100) }
 fun hash(b:Bitmap):Long{ val s=Bitmap.createScaledBitmap(b,8,8,true); val a=DoubleArray(64); var avg=0.0; var i=0; for(y in 0..7)for(x in 0..7){ val c=s.getPixel(x,y); val v=.299*Color.red(c)+.587*Color.green(c)+.114*Color.blue(c); a[i++]=v; avg+=v }; avg/=64; var h=0L; for(j in a.indices) if(a[j]>=avg) h=h or (1L shl j); return h }
 fun hamming(a:Long,b:Long)=java.lang.Long.bitCount(a xor b)
}
