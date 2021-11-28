package com.cmpe295.iAssist.ui.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.media.Image
import android.media.ImageReader
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.speech.tts.TextToSpeech
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.*
import android.view.TextureView.SurfaceTextureListener
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import com.cmpe295.iAssist.DashboardActivity
import com.cmpe295.iAssist.databinding.FragmentVisualBinding
import com.google.gson.Gson
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import edmt.dev.edmtdevcognitivevision.Contract.AnalysisResult
import edmt.dev.edmtdevcognitivevision.Rest.VisionServiceException
import edmt.dev.edmtdevcognitivevision.VisionServiceClient
import edmt.dev.edmtdevcognitivevision.VisionServiceRestClient
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.*
import java.util.*


class VisualAssistanceFragment : Fragment() {

    var bitmap: Bitmap? = null
    var string: String = "{}"
    lateinit var res : AnalysisResult
    lateinit var visionServiceClient : VisionServiceClient
    companion object {


        val API_KEY="*****"
        val API_LINK="******"

        val ORIENTATIONS = SparseIntArray()
        init {
            ORIENTATIONS.append(Surface.ROTATION_0, 90)
            ORIENTATIONS.append(Surface.ROTATION_90, 0)
            ORIENTATIONS.append(Surface.ROTATION_180, 270)
            ORIENTATIONS.append(Surface.ROTATION_270, 180)
        }
    }
    private lateinit var binding: FragmentVisualBinding
    lateinit var mTextToSpeech: TextToSpeech

    //camera2 stuff
    private val TAG: String? = "AndroidCameraApi"
    private var textureView: TextureView? = null
    protected var cameraDevice: CameraDevice? = null
    protected var cameraCaptureSessions: CameraCaptureSession? = null
    protected var captureRequestBuilder: CaptureRequest.Builder? = null
    private var imageDimension: Size? = null
    private var imageReader: ImageReader? = null
    private val REQUEST_CAMERA_PERMISSION = 200
    private var mBackgroundHandler: Handler? = null
    private var mBackgroundThread: HandlerThread? = null
    private var calculated_distance = 0.0f

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        EventBus.getDefault().register(this)
        binding = FragmentVisualBinding.inflate(layoutInflater)
        return binding.root
    }

    @Subscribe
    public fun onKeyEvent(event : DashboardActivity.Event) {
        // Called by eventBus when an event occurs
        if(event.keyCode == 25) {
            takePicture1()
        }
        if(event.keyCode==24){
            takepicture2()
        }
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textureView = binding.texture
        assert(textureView != null)
        textureView!!.surfaceTextureListener = textureListener

        mTextToSpeech = TextToSpeech(requireContext(), TextToSpeech.OnInitListener { status ->
            if (status != TextToSpeech.ERROR) {
                //if there is no error then set language
                mTextToSpeech.language = Locale.US
            }
        })
    }

    var textureListener: SurfaceTextureListener = object : SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            //open your camera here
            openCamera()
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            // Transform you image captured size according to the surface width and height
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            return false
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
    }

    private val stateCallback: CameraDevice.StateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            //This is called when the camera is open
            Log.e(TAG, "onOpened")
            cameraDevice = camera
            createCameraPreview()
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice!!.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice!!.close()
            cameraDevice = null
        }
    }

    protected fun startBackgroundThread() {
        mBackgroundThread = HandlerThread("Camera Background")
        mBackgroundThread!!.start()
        mBackgroundHandler = Handler(mBackgroundThread!!.getLooper())
    }

    protected fun stopBackgroundThread() {
        mBackgroundThread!!.quitSafely()
        try {
            mBackgroundThread!!.join()
            mBackgroundThread = null
            mBackgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    protected fun takePicture1() {
        if (null == cameraDevice) {
            Log.e(TAG, "cameraDevice is null")
            return
        }
        val manager = getSystemService(requireContext(), CameraManager::class.java)
        try {
            val characteristics = manager?.getCameraCharacteristics(
                cameraDevice!!.id
            )
            var jpegSizes: Array<Size>? = null
            if (characteristics != null) {
                jpegSizes =
                    characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
                        .getOutputSizes(ImageFormat.JPEG)
                Log.e("CameraCharacteristics","${CameraCharacteristics.LENS_INFO_FOCUS_DISTANCE_CALIBRATION}")
            }
            var width = 640
            var height = 480
            if (jpegSizes != null && 0 < jpegSizes.size) {
                width = jpegSizes[0].width
                height = jpegSizes[0].height
            }
            val reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1)
            val outputSurfaces: MutableList<Surface> = ArrayList(2)
            outputSurfaces.add(reader.surface)
            outputSurfaces.add(Surface(textureView!!.surfaceTexture))
            val captureBuilder =
                cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(reader.surface)
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
            // Orientation
            val display = (getSystemService(requireContext(),WindowManager::class.java))
                ?.getDefaultDisplay()
            val rotation: Int = display?.getRotation() ?: 0;
            captureBuilder.set(
                CaptureRequest.JPEG_ORIENTATION,
                ORIENTATIONS.get(rotation)
            )
            val file = File(Environment.getExternalStorageDirectory().toString() + "/pic.jpg")
            val readerListener: ImageReader.OnImageAvailableListener =
                object : ImageReader.OnImageAvailableListener {
                    override fun onImageAvailable(reader: ImageReader) {
                        var image: Image? = null
                        try {
                            image = reader.acquireLatestImage()
                            val buffer = image.planes[0].buffer
                            val bytes = ByteArray(buffer.capacity())
                            buffer[bytes]
                            val clonedBytes = bytes.clone()
                            bitmap = BitmapFactory.decodeByteArray(clonedBytes, 0, clonedBytes.size)
                            visionServiceClient = VisionServiceRestClient(API_KEY, API_LINK)
                            val uiScope = CoroutineScope(Dispatchers.Main)
                            //TODO
                            uiScope.launch {
                                withContext(Dispatchers.Default) {
                                    val outputStream = ByteArrayOutputStream()
                                    // Thread.sleep(3000)
                                    bitmap?.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
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
                                        val result : AnalysisResult = Gson().fromJson<AnalysisResult>(string,AnalysisResult::class.java)

                                        val result_text = StringBuilder()
                                        for(caption in result.description.captions!!){
                                            result_text.append(caption.text)
                                            Log.i("Description",result_text.toString())
                                            var roundup_distance: String? = "%.2f".format(calculated_distance)
                                            val toSpeak = "$result_text at $roundup_distance meters"
                                           //val toSpeak = "$result_text at $calculated_distance meters"
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
                                }
                            }
//                            save(bytes)
                        } catch (e: FileNotFoundException) {
                            e.printStackTrace()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        } finally {
                            image?.close()
                        }
                    }

                    @Throws(IOException::class)
                    private fun save(bytes: ByteArray) {
                        var output: OutputStream? = null
                        try {
                            output = FileOutputStream(file)
                            output.write(bytes)
                        } finally {
                            output?.close()
                        }
                    }
                }
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler)
            val captureListener: CaptureCallback = object : CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    super.onCaptureCompleted(session, request, result)
                    Log.e(
                        TAG,
                        String.format(
                            "captureCallbackListener %s-%f",
                            result.get(CaptureResult.LENS_STATE).toString(),
                            result.get(CaptureResult.LENS_FOCUS_DISTANCE)
                        )
                    )
                    Log.e(
                        TAG,
                        String.format(
                            "AF mode %s-%s",
                            result.get(CaptureResult.CONTROL_AF_MODE).toString(),
                            result.get(CaptureResult.CONTROL_AF_STATE).toString()
                        )
                    )

                    calculated_distance = 1 / result.get(CaptureResult.LENS_FOCUS_DISTANCE)!!
//                    Toast.makeText(
//                        requireContext(),
//                        "Distance: $calculated_distance meters",
//                        Toast.LENGTH_SHORT
//                    ).show()
                    createCameraPreview()
                }
            }
            cameraDevice!!.createCaptureSession(
                outputSurfaces,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        try {
                            session.capture(
                                captureBuilder.build(),
                                captureListener,
                                mBackgroundHandler
                            )
                        } catch (e: CameraAccessException) {
                            e.printStackTrace()
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {}
                },
                mBackgroundHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    protected fun createCameraPreview() {
        try {
            val texture = textureView!!.surfaceTexture!!
            texture.setDefaultBufferSize(imageDimension!!.width, imageDimension!!.height)
            val surface = Surface(texture)
            captureRequestBuilder =
                cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder!!.addTarget(surface)
            cameraDevice!!.createCaptureSession(
                Arrays.asList(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                        //The camera is already closed
                        if (null == cameraDevice) {
                            return
                        }
                        // When the session is ready, we start displaying the preview.
                        cameraCaptureSessions = cameraCaptureSession
                        updatePreview()
                    }

                    override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                        Toast.makeText(
                            requireContext(),
                            "Configuration change",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                null
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun openCamera() {
        val manager = getSystemService(requireContext(),CameraManager::class.java)
        Log.e(TAG, "is camera open")
        try {
            val cameraIds = manager!!.cameraIdList
            val characteristics = manager!!.getCameraCharacteristics(cameraIds.get(0))
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
            imageDimension = map.getOutputSizes(SurfaceTexture::class.java)[0]
            // Add permission for camera and let user grant the permission
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_CAMERA_PERMISSION
                )
                return
            }
            manager.openCamera(cameraIds.get(0), stateCallback, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
        Log.e(TAG, "openCamera X")
    }

    protected fun updatePreview() {
        if (null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return")
        }
        captureRequestBuilder!!.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
        try {
            cameraCaptureSessions!!.setRepeatingRequest(
                captureRequestBuilder!!.build(),
                null,
                mBackgroundHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun closeCamera() {
        if (null != cameraDevice) {
            cameraDevice!!.close()
            cameraDevice = null
        }
        if (null != imageReader) {
            imageReader!!.close()
            imageReader = null
        }
    }
    private fun takepicture2() {

            if (null == cameraDevice) {
                Log.e(TAG, "cameraDevice is null")
                return
            }
            val manager = getSystemService(requireContext(), CameraManager::class.java)
            try {
                val characteristics = manager?.getCameraCharacteristics(
                    cameraDevice!!.id
                )
                var jpegSizes: Array<Size>? = null
                if (characteristics != null) {
                    jpegSizes =
                        characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
                            .getOutputSizes(ImageFormat.JPEG)
                    Log.e("CameraCharacteristics","${CameraCharacteristics.LENS_INFO_FOCUS_DISTANCE_CALIBRATION}")
                }
                var width = 640
                var height = 480
                if (jpegSizes != null && 0 < jpegSizes.size) {
                    width = jpegSizes[0].width
                    height = jpegSizes[0].height
                }
                val reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1)
                val outputSurfaces: MutableList<Surface> = ArrayList(2)
                outputSurfaces.add(reader.surface)
                outputSurfaces.add(Surface(textureView!!.surfaceTexture))
                val captureBuilder =
                    cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                captureBuilder.addTarget(reader.surface)
                captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                // Orientation
                val display = (getSystemService(requireContext(),WindowManager::class.java))
                    ?.getDefaultDisplay()
                val rotation: Int = display?.getRotation() ?: 0;
                captureBuilder.set(
                    CaptureRequest.JPEG_ORIENTATION,
                    ORIENTATIONS.get(rotation)
                )
                val file = File(Environment.getExternalStorageDirectory().toString() + "/pic.jpg")
                val readerListener: ImageReader.OnImageAvailableListener =
                    object : ImageReader.OnImageAvailableListener {
                        override fun onImageAvailable(reader: ImageReader) {
                            var image: Image? = null
                            try {
                                image = reader.acquireLatestImage()
                                val buffer = image.planes[0].buffer
                                val bytes = ByteArray(buffer.capacity())
                                buffer[bytes]
                                val clonedBytes = bytes.clone()
                                bitmap = BitmapFactory.decodeByteArray(clonedBytes, 0, clonedBytes.size)
                                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                                val image: InputImage
                                InputImage.fromBitmap(bitmap, 0).also { image = it }
                                val result = recognizer.process(image)
                                    .addOnSuccessListener { visionText ->
                                        // Task completed successfully
                                        // ...
                                        Log.i("textrecogn",visionText.text)
                                        if (visionText.text!= null) {

                                        if(!::mTextToSpeech.isInitialized){
                                            mTextToSpeech = TextToSpeech(requireContext(), TextToSpeech.OnInitListener {

                                            })
                                        }
                                        mTextToSpeech.speak(visionText.text, TextToSpeech.QUEUE_FLUSH,null)
                                    }
                                    }
                                    .addOnFailureListener { e ->
                                        // Task failed with an exception
                                        // ...
                                        Log.i("ml firebaseeror",e.toString())
                                    }


                            } catch (e: FileNotFoundException) {
                                e.printStackTrace()
                            } catch (e: IOException) {
                                e.printStackTrace()
                            } finally {
                                image?.close()
                            }
                        }

                        @Throws(IOException::class)
                        private fun save(bytes: ByteArray) {
                            var output: OutputStream? = null
                            try {
                                output = FileOutputStream(file)
                                output.write(bytes)
                            } finally {
                                output?.close()
                            }
                        }
                    }
                reader.setOnImageAvailableListener(readerListener, mBackgroundHandler)
                val captureListener: CaptureCallback = object : CaptureCallback() {
                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult
                    ) {
                        super.onCaptureCompleted(session, request, result)
                        Log.e(
                            TAG,
                            String.format(
                                "captureCallbackListener %s-%f",
                                result.get(CaptureResult.LENS_STATE).toString(),
                                result.get(CaptureResult.LENS_FOCUS_DISTANCE)
                            )
                        )
                        Log.e(
                            TAG,
                            String.format(
                                "AF mode %s-%s",
                                result.get(CaptureResult.CONTROL_AF_MODE).toString(),
                                result.get(CaptureResult.CONTROL_AF_STATE).toString()
                            )
                        )

                        calculated_distance = 1 / result.get(CaptureResult.LENS_FOCUS_DISTANCE)!!
//                    Toast.makeText(
//                        requireContext(),
//                        "Distance: $calculated_distance meters",
//                        Toast.LENGTH_SHORT
//                    ).show()
                        createCameraPreview()
                    }
                }
                cameraDevice!!.createCaptureSession(
                    outputSurfaces,
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            try {
                                session.capture(
                                    captureBuilder.build(),
                                    captureListener,
                                    mBackgroundHandler
                                )
                            } catch (e: CameraAccessException) {
                                e.printStackTrace()
                            }
                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) {}
                    },
                    mBackgroundHandler
                )
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }

    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(
                    requireContext(),
                    "Sorry!!!, you can't use this app without granting permission",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.e(TAG, "onResume")
        startBackgroundThread()
        if (textureView!!.isAvailable) {
            openCamera()
        } else {
            textureView!!.surfaceTextureListener = textureListener
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        EventBus.getDefault().unregister(this)
    }


    override fun onPause() {
        Log.e(TAG, "onPause")
        //closeCamera();
        stopBackgroundThread()
        super.onPause()
    }


<<
//    private suspend fun processimage() {
//        withContext(Dispatchers.Default) {
//            val outputStream = ByteArrayOutputStream()
//           // Thread.sleep(3000)
//            bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
//            val inputStream = ByteArrayInputStream(outputStream.toByteArray())
//            try {
//                val features : Array<String> = arrayOf("Description")
//                val details = arrayOf<String>()
//                res = visionServiceClient.analyzeImage(inputStream, features,details)
//                string = Gson().toJson(res)
//                Log.d("result", string);
//
//            } catch (e: VisionServiceException){
//                Log.e("visionexception",e.message.toString())
//            }
//
//           withContext(Dispatchers.Main) {
//               val result : AnalysisResult = Gson().fromJson<AnalysisResult>(string,AnalysisResult::class.java)
//
//                val result_text = StringBuilder()
//                for(caption in result.description.captions!!){
//                    result_text.append(caption.text)
//                    Log.i("Description",result_text.toString())
//                     var roundup_distance: String? = "%.2f".format(calculated_distance)
//                    val toSpeak = "$result_text at $roundup_distance meters"
//                    val toSpeak = "$result_text at $calculated_distance meters"
//                    if (toSpeak != null) {
//                        Log.i("Speech",toSpeak.toString())
//                        if(!::mTextToSpeech.isInitialized){
//                            mTextToSpeech = TextToSpeech(requireContext(), TextToSpeech.OnInitListener {
//
//                            })
//                        }
//                        mTextToSpeech.speak(toSpeak, TextToSpeech.QUEUE_FLUSH,null)
//                    }
//                }
//
//           }
//        }
//
//    }




}