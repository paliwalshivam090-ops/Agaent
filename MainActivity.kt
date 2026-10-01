package com.not.voiceagent

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var tts: TextToSpeech
    private lateinit var status: TextView
    private lateinit var recognizer: SpeechRecognizer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val btn = Button(this).apply { text = "🎤 Speak" }
        status = TextView(this).apply { text = "Tap and speak"; textSize = 18f }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(48, 96, 48, 48)
            addView(btn); addView(status)
        })
        tts = TextToSpeech(this) { tts.language = Locale.ENGLISH }
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(b: Bundle) {
                val said = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: return
                status.text = "You: $said\nThinking…"
                Thread {
                    try {
                        val a = Brain.think(said)
                        runOnUiThread {
                            status.text = "You: $said\nAgent: ${a.reply}"
                            tts.speak(a.reply, TextToSpeech.QUEUE_FLUSH, null, "r")
                            Actions.run(this@MainActivity, a)
                        }
                    } catch (e: Exception) { runOnUiThread { status.text = "Error: ${e.message}" } }
                }.start()
            }
            override fun onError(error: Int) { status.text = "Didn't catch that (code $error)" }
            override fun onReadyForSpeech(p: Bundle?) { status.text = "Listening…" }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(p: Bundle?) {}
            override fun onEvent(t: Int, p: Bundle?) {}
        })
        btn.setOnClickListener {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
                requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA), 1)
            else recognizer.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN"))
        }
    }

    override fun onDestroy() { recognizer.destroy(); tts.shutdown(); super.onDestroy() }
}
