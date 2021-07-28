package com.cmpe295.iAssist

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class HomeActivity : AppCompatActivity() {
//    private var speechRecognizer: SpeechRecognizer? = null
//    private var textToSpeech: TextToSpeech? = null
    private val permission = 200
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

      //  buttonSpeak = this.button_speak

     //   buttonSpeak!!.isEnabled = true;

        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

//        ActivityCompat.requestPermissions(
//            this,
//            arrayOf(
//                Manifest.permission.RECORD_AUDIO,
//                Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                Manifest.permission.READ_EXTERNAL_STORAGE
//            ),
//            PackageManager.PERMISSION_GRANTED
//        )
//        //textView = findViewById(R.id.textView)
//        textToSpeech = TextToSpeech(this,this)
//        intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
//        intent!!.putExtra(
//            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
//            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
//        )
//
//        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
//        speechRecognizer!!.setRecognitionListener(object : RecognitionListener {
//            override fun onReadyForSpeech(params: Bundle) {}
//            override fun onBeginningOfSpeech() {}
//            override fun onRmsChanged(rmsdB: Float) {}
//            override fun onBufferReceived(buffer: ByteArray) {}
//            override fun onEndOfSpeech() {}
//            override fun onError(error: Int) {}
//            override fun onResults(results: Bundle) {
//                val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
//                var string = ""
//                textView.text = ""
//                if (matches != null) {
//                    string = matches[0]
//                    textView.text = string
//                    if (string == "yes") {
//                        val intent = Intent(this@HomeActivity, VisualAssistanceActivity::class.java)
//                        startActivity(intent)
//                    }
//                    else {
//                        val intent = Intent(this@HomeActivity, HomeActivity::class.java)
//                        startActivity(intent)
//
//                    }
//                }
//            }
//
//            override fun onPartialResults(partialResults: Bundle) {}
//            override fun onEvent(eventType: Int, params: Bundle) {}
//        })
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

        btn_hearing.setOnClickListener {
            val intent = Intent(this@HomeActivity, HearingAssistanceActivity::class.java)
            startActivity(intent)
        }

        btn_mute.setOnClickListener {
            val intent = Intent(this@HomeActivity, SpeakingAssistanceActivity::class.java)
            startActivity(intent)
        }

    }

//    override fun onInit(status: Int) {
//        Toast.makeText(applicationContext, "oninit", Toast.LENGTH_SHORT).show()
//        textToSpeech!!.speak(
//            "Do you need visual Assistance?",
//            TextToSpeech.QUEUE_FLUSH,
//            null,
//            null
//        )
//
////        @Suppress("DEPRECATION")
////        Handler().postDelayed(
////            {
////                speechRecognizer!!.startListening(intent)
////            },
////            10000)
//        try {
//            Thread.sleep(10000)
//            speechRecognizer!!.startListening(intent)
//        } catch (e: InterruptedException) {
//            e.printStackTrace()
//        }
//
//    }
}