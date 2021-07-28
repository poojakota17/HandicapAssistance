package com.cmpe295.iAssist

import android.Manifest.permission
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                permission.RECORD_AUDIO,
                permission.WRITE_EXTERNAL_STORAGE,
                permission.READ_EXTERNAL_STORAGE
            ),
            PackageManager.PERMISSION_GRANTED
        )
        textToSpeech = TextToSpeech(this,this)
        intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent!!.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer!!.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle) {

            }
            override fun onBeginningOfSpeech() {

            }
            override fun onRmsChanged(rmsdB: Float) {

            }
            override fun onBufferReceived(buffer: ByteArray) {

            }
            override fun onEndOfSpeech() {

            }
            override fun onError(error: Int) {
                val intent = Intent(this@MainActivity, HomeActivity::class.java)
                startActivity(intent)

            }
            override fun onResults(results: Bundle) {
                val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                var string = ""
                if (matches != null) {
                    string = matches[0]
                    if (string == "yes") {

                        val intent = Intent(this@MainActivity, VisualAssistanceActivity::class.java)
                        startActivity(intent)
                    }
                    else if (string == "no") {

                        val intent = Intent(this@MainActivity, HomeActivity::class.java)
                        startActivity(intent)
                    }

                }
            }

            override fun onPartialResults(partialResults: Bundle) {}
            override fun onEvent(eventType: Int, params: Bundle) {}
        })
    }
    override fun onInit(status: Int) {
        textToSpeech!!.speak(
            "Do you need visual Assistance?",
            TextToSpeech.QUEUE_FLUSH,
            null,
            null
        )
       try {
           Thread.sleep(8000)
           speechRecognizer!!.startListening(intent)
       } catch (e: InterruptedException) {
           e.printStackTrace()
       }

    }
}