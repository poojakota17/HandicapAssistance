package com.cmpe295.iAssist.ui.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.cmpe295.iAssist.Constants
import com.cmpe295.iAssist.R
import com.cmpe295.iAssist.VisualAssistanceActivity
import com.cmpe295.iAssist.databinding.ActivityVisualAssistanceBinding
import com.cmpe295.iAssist.databinding.FragmentVisualBinding
import com.google.gson.Gson
import edmt.dev.edmtdevcognitivevision.Contract.AnalysisResult
import edmt.dev.edmtdevcognitivevision.Rest.VisionServiceException
import edmt.dev.edmtdevcognitivevision.VisionServiceClient
import edmt.dev.edmtdevcognitivevision.VisionServiceRestClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*

class VisualAssistanceFragment : Fragment() {

    var bitmap: Bitmap? = null
    var string: String = "{}"
    lateinit var res : AnalysisResult
    lateinit var visionServiceClient : VisionServiceClient
    companion object {
        val API_KEY = "*********"
        val API_LINK = "************"
    }
    private lateinit var binding: FragmentVisualBinding
    private var imageCapture: ImageCapture?=null
    lateinit var mTextToSpeech: TextToSpeech

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentVisualBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(allPermissionGranted()){
            startCamera()
            Toast.makeText(requireActivity(), "We have permission", Toast.LENGTH_SHORT).show()
        }else{
            requestPermissions(Constants.REQUIRED_PERMISSIONS, Constants.REQUEST_CODE_PERMISSIONS)
        }

        binding.btnTakePhoto.setOnClickListener {
            takePhoto()
        }

        mTextToSpeech = TextToSpeech(requireContext(), TextToSpeech.OnInitListener { status ->
            if (status != TextToSpeech.ERROR) {
                //if there is no error then set language
                mTextToSpeech.language = Locale.US
            }
        })
    }

    private fun allPermissionGranted() =
        Constants.REQUIRED_PERMISSIONS.all {
            context?.let { it1 -> ContextCompat.checkSelfPermission(it1, it) } == PackageManager.PERMISSION_GRANTED
        }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == Constants.REQUEST_CODE_PERMISSIONS){
            if(allPermissionGranted()){
                //code
                startCamera()
            }else{
                Toast.makeText(requireActivity(), "Permission not granted by the user", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    private fun takePhoto(){

        val imageCapture = imageCapture?: return
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(requireContext()), object :ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)
                    Log.i("assist","captured photo")
                    // code to call azure api
                    /**
                     * Convert Image Proxy to Bitmap
                     */
                    fun toBitmap(image: ImageProxy): Bitmap? {
                        val byteBuffer = image.planes[0].buffer
                        byteBuffer.rewind()
                        val bytes = ByteArray(byteBuffer.capacity())
                        byteBuffer[bytes]
                        val clonedBytes = bytes.clone()
                        return BitmapFactory.decodeByteArray(clonedBytes, 0, clonedBytes.size)
                    }

                    bitmap = toBitmap(image)
                    visionServiceClient = VisionServiceRestClient(VisualAssistanceFragment.API_KEY, VisualAssistanceFragment.API_LINK)
                    val uiScope = CoroutineScope(Dispatchers.Main)
                    //TODO
                    uiScope.launch {
                        processimage()
                    }
                    image.close()
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                    Log.e(Constants.TAG, "onError: ${exception.message}", exception)
                }
            }
        )
    }

    private fun startCamera(){
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also { mPreview->
                    mPreview.setSurfaceProvider(
                        binding.viewFinder.surfaceProvider
                    )
                }
            imageCapture = ImageCapture.Builder()
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            }catch (e: Exception){
                Log.d(Constants.TAG, "StartCamera Fail:", e)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }


    private suspend fun processimage() {
        withContext(Dispatchers.Default) {
            val outputStream = ByteArrayOutputStream()
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
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
                    Log.i("Description",result_text.toString())
                    val toSpeak = result_text.toString()
                    if (toSpeak != null) {
                        Log.i("Speech",toSpeak.toString())
                        if(!::mTextToSpeech.isInitialized){
                            mTextToSpeech = TextToSpeech(requireContext(), TextToSpeech.OnInitListener {

                            })
                        }
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