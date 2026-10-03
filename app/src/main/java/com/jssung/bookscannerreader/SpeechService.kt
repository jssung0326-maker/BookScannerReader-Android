package com.jssung.bookscannerreader
import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale
class SpeechService(ctx:Context):TextToSpeech.OnInitListener{ private val tts=TextToSpeech(ctx.applicationContext,this); var rate=1f; override fun onInit(s:Int){if(s==TextToSpeech.SUCCESS){tts.language=Locale.KOREAN;tts.setSpeechRate(rate)}}; fun speak(t:String){tts.setSpeechRate(rate);tts.speak(t,TextToSpeech.QUEUE_FLUSH,null,"reader")}; fun stop()=tts.stop(); fun shutdown()=tts.shutdown() }
