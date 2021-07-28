package com.cmpe295.iAssist

import android.Manifest.permission
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    // private var textView: TextView? = null
    //private var intent: Intent? = null
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
        //textView = findViewById(R.id.textView)
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
        //startButton();

//       textToSpeech!!.speak(
//           "Please tell me, how can I help you?",
//           TextToSpeech.QUEUE_FLUSH,
//           null,
//           null
//       )
//       try {
//           Thread.sleep(3000)
//       } catch (e: InterruptedException) {
//           e.printStackTrace()
//       }
//       speechRecognizer!!.startListening(intent)
    }

//    private fun startButton() {
//        textToSpeech?.speak(
//            "Please tell me, how can I help you?",
//            TextToSpeech.QUEUE_FLUSH,
//            null,
//            null
//        )
//        try {
//            Thread.sleep(3000)
//        } catch (e: InterruptedException) {
//            e.printStackTrace()
//        }
//        speechRecognizer!!.startListening(intent)
//    }

    private fun createMethod() {
        Toast.makeText(applicationContext, "Create called", Toast.LENGTH_SHORT).show()
    }

    override fun onInit(status: Int) {
        Toast.makeText(applicationContext, "oninit", Toast.LENGTH_SHORT).show()
        textToSpeech!!.speak(
            "Do you need visual Assistance?",
            TextToSpeech.QUEUE_FLUSH,
            null,
            null
        )

//        @Suppress("DEPRECATION")
//        Handler().postDelayed(
//            {
//                speechRecognizer!!.startListening(intent)
//            },
//            10000)
       try {
           Thread.sleep(8000)
           speechRecognizer!!.startListening(intent)
       } catch (e: InterruptedException) {
           e.printStackTrace()
       }

    }
}