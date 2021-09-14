package com.cmpe295.iAssist.ui.fragments

import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.cmpe295.iAssist.R
import kotlinx.android.synthetic.main.activity_speaking_assistance.*
import kotlinx.android.synthetic.main.fragment_talking.*
import java.util.*

class TalkingAssistanceFragment : Fragment(), TextToSpeech.OnInitListener {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root: View = inflater.inflate(R.layout.fragment_talking,container, false)

        buttonSpeak = root.findViewById(R.id.button_to_speak)
        editText = root.findViewById(R.id.edit_input_for_speech)
        buttonClear = root.findViewById(R.id.button_to_clear)

        buttonSpeak!!.isEnabled = false;
        tts = TextToSpeech(activity, this)

        buttonSpeak!!.setOnClickListener { speakOut() }
        buttonClear!!.setOnClickListener { cleartext() }

        return root
    }

    private var tts: TextToSpeech? = null
    private var buttonSpeak: Button? = null
    private var buttonClear: Button? = null
    private var editText: EditText? = null

    override fun onInit(status: Int) {

        if (status == TextToSpeech.SUCCESS) {
            // set US English as language for tts
            val result = tts!!.setLanguage(Locale.US)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS","The Language specified is not supported!")
            } else {
                buttonSpeak!!.isEnabled = true
            }

        } else {
            Log.e("TTS", "Initilization Failed!")
        }

    }

    private fun speakOut() {
        val text = editText!!.text.toString()
        tts!!.speak(text, TextToSpeech.QUEUE_FLUSH, null,"")
    }

    private fun cleartext() {
        editText?.setText("")
    }
    public override fun onDestroy() {
        // Shutdown TTS
        if (tts != null) {
            tts!!.stop()
            tts!!.shutdown()
        }
        super.onDestroy()
    }

}