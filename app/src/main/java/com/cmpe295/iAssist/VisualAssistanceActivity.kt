package com.cmpe295.iAssist

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.KeyEvent
import android.view.WindowInsets
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import edmt.dev.edmtdevcognitivevision.Contract.AnalysisResult
import edmt.dev.edmtdevcognitivevision.Rest.VisionServiceException
import edmt.dev.edmtdevcognitivevision.VisionServiceClient
import edmt.dev.edmtdevcognitivevision.VisionServiceRestClient
import kotlinx.android.synthetic.main.activity_visual_assistance.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*


class VisualAssistanceActivity : AppCompatActivity(){

    lateinit var mTextToSpeech:TextToSpeech
    lateinit var bitmap: Bitmap
    var string: String = "{}"
    lateinit var res : AnalysisResult
    lateinit var visionServiceClient : VisionServiceClient
    companion object {
        val API_KEY = "*********"
        val API_LINK = "************"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_visual_assistance)

        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
        if (ContextCompat.checkSelfPermission(
                this@VisualAssistanceActivity,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this@VisualAssistanceActivity, arrayOf(
                    Manifest.permission.CAMERA
                ), 100
            )
        }

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, 100)

//        bt_open.setOnClickListener {
//            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//            startActivityForResult(intent, 100)
//        }

        mTextToSpeech = TextToSpeech(applicationContext, TextToSpeech.OnInitListener { status ->
            if (status != TextToSpeech.ERROR) {
                //if there is no error then set language
                mTextToSpeech.language = Locale.US
            }
        })

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100) {
            val captureImage = data!!.extras!!["data"] as Bitmap?
            image_view!!.setImageBitmap(captureImage)
            if (captureImage != null) {
                bitmap = captureImage
            }

        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            event.startTracking()
            Log.d("Test", "Short")
            visionServiceClient = VisionServiceRestClient(API_KEY, API_LINK)
            val uiScope = CoroutineScope(Dispatchers.Main)
            //TODO
            uiScope.launch {
                processimage()
            }
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private suspend fun processimage() {
        withContext(Dispatchers.Default) {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            val inputStream = ByteArrayInputStream(outputStream.toByteArray())
            try {
                val features : Array<String> = arrayOf("Description")
                val details = arrayOf<String>()
                res = visionServiceClient.analyzeImage(inputStream, features,details)
                string = Gson().toJson(res)
                Log.d("result", string);

            } catch (e: VisionServiceException){
                Log.e("visionexception",e.message.toString())
            }

            withContext(Dispatchers.Main) {
                //val txt_result : TextView = findViewById(R.id.textresult)
                val result : AnalysisResult = Gson().fromJson<AnalysisResult>(string,AnalysisResult::class.java)

                val result_text = StringBuilder()
                for(caption in result.description.captions!!){
                    result_text.append(caption.text)
                    Log.e("description",result_text.toString())
                    val toSpeak = result_text.toString()
                    if (toSpeak != null) {
                        Log.e("speech",toSpeak.toString())
                        mTextToSpeech.speak(toSpeak, TextToSpeech.QUEUE_FLUSH,null)
                    }
                }

            }
//            //get text
//            val toSpeak = textresult.text.toString()
//            if (toSpeak != null) {
//                mTextToSpeech.speak(toSpeak, TextToSpeech.QUEUE_FLUSH,null)
//            }
        }
    }



}