package com.cmpe295.iAssist

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.text.method.ScrollingMovementMethod
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import java.util.*
import kotlin.collections.ArrayList

class HearingAssistanceActivity : AppCompatActivity() {
    private var txvResult: TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hearing_assistance)
        txvResult = findViewById<TextView>(R.id.txvResult)
    }

    fun getSpeechInput(view: View?) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US)
        //intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 50000);

        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, 10)
        } else {
            Toast.makeText(this, "Your Device Don't Support Speech Input", Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            10 -> if (resultCode == RESULT_OK && data != null) {
                val result: ArrayList<String> =
                    data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) as ArrayList<String>
                txvResult?.text = result[0]
                txvResult?.movementMethod = ScrollingMovementMethod();
            }
        }
    }
}