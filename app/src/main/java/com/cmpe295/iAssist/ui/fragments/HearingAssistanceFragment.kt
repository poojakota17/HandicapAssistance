package com.cmpe295.iAssist.ui.fragments

import android.Manifest
import android.Manifest.permission.*
import android.content.Context
import android.content.Context.VIBRATOR_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Vibrator
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import com.cmpe295.iAssist.R
import kotlinx.android.synthetic.main.activity_hearing_assistance.*
import java.util.*
import kotlin.collections.ArrayList

class HearingAssistanceFragment : Fragment(), RecognitionListener{
    private var returnedText: TextView? = null
    private var returnedError: TextView? = null
    private var progressBar: ProgressBar? = null
    private var speech: SpeechRecognizer? = null
    private var recognizerIntent: Intent? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root: View = inflater.inflate(R.layout.fragment_hearing,container,false)

        // UI initialisation
        returnedText = root.findViewById(R.id.textView1)
        returnedError = root.findViewById(R.id.errorView1)
        progressBar = root.findViewById(R.id.progressBar1)
        progressBar!!.visibility = View.INVISIBLE

        // start speech recogniser
        resetSpeechRecognizer()

        // start progress bar
        progressBar!!.visibility = View.VISIBLE
        progressBar!!.isIndeterminate = true


        setRecogniserIntent()
        speech!!.startListening(recognizerIntent)

        return root
    }

    private fun resetSpeechRecognizer() {
        if (speech != null) speech!!.destroy()
        speech = SpeechRecognizer.createSpeechRecognizer(activity)
        Log.e("VSR", "isRecognitionAvailable: " + SpeechRecognizer.isRecognitionAvailable(activity))
        if (SpeechRecognizer.isRecognitionAvailable(activity)) speech!!.setRecognitionListener(this) else finish()
    }

    private fun setRecogniserIntent() {
        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        recognizerIntent!!.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
            "en"
        )
        recognizerIntent!!.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        recognizerIntent!!.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 10)
        recognizerIntent!!.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (permissions != null) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                speech!!.startListening(recognizerIntent)
            } else {
                Toast.makeText(activity, "Permission Denied!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun finish() {
    }

    override fun onResume() {
        Log.e("VSR", "resume")
        super.onResume()
        resetSpeechRecognizer()
        speech!!.startListening(recognizerIntent)
    }

    override fun onPause() {
        Log.e("VSR", "pause")
        super.onPause()
        speech!!.stopListening()
    }

    override fun onStop() {
        Log.e("VSR", "stop")
        super.onStop()
        if (speech != null) {
            speech!!.destroy()
        }
    }

    override fun onBeginningOfSpeech() {
        Log.e("VSR", "onBeginningOfSpeech")
        val vibe = context?.getSystemService(VIBRATOR_SERVICE) as Vibrator
        vibe.vibrate(200)
        progressBar!!.isIndeterminate = false
        progressBar!!.max = 10
    }

    override fun onBufferReceived(buffer: ByteArray) {
        Log.e("VSR", "onBufferReceived: $buffer")
    }

    override fun onEndOfSpeech() {
        Log.e("VSR", "onEndOfSpeech")
        progressBar!!.isIndeterminate = true
        speech!!.stopListening()
    }

    override fun onResults(results: Bundle) {
        Log.e("VSR", "onResults")
        val matches = results
            .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
//        String text = "";
//        for (String result : matches)
//            text += result + "\n";
        returnedText!!.text = matches!![0]
        returnedText?.movementMethod = ScrollingMovementMethod();
        returnedError!!.text = "Match found"
        speech!!.startListening(recognizerIntent)
    }

    override fun onError(errorCode: Int) {
        val errorMessage = getErrorText(errorCode)
        Log.e("VSR", "FAILED $errorMessage")
        returnedError!!.text = errorMessage

        // rest voice recogniser
        resetSpeechRecognizer()
        speech!!.startListening(recognizerIntent)
    }

    override fun onEvent(arg0: Int, arg1: Bundle) {
        Log.e("VSR", "onEvent")
    }

    override fun onPartialResults(arg0: Bundle) {
        val data: ArrayList<*>? = arg0.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val word = data!![data.size - 1] as String
        Log.e("VSR", "onPartialResults: $word")
        returnedText!!.text = word
        returnedError!!.text = "Listening..."
        Log.e("VSR", "onPartialResults")
    }

    override fun onReadyForSpeech(arg0: Bundle) {
        Log.e("VSR", "onReadyForSpeech")
    }

    override fun onRmsChanged(rmsdB: Float) {
        Log.e("VSR", "onRmsChanged: $rmsdB")
        progressBar!!.progress = rmsdB.toInt()
    }

    private fun getErrorText(errorCode: Int): String {
        val message: String = when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
            SpeechRecognizer.ERROR_SERVER -> "error from server"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Didn't understand, please try again."
        }
        return message
    }

    companion object {
        private const val PERMISSIONS_REQUEST_RECORD_AUDIO = 1
    }
}