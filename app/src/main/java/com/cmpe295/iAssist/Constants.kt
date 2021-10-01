package com.cmpe295.iAssist

import android.Manifest

object Constants {

    const val TAG = "cameraX"
    const val FILE_FORMAT = "yy-MM-dd-HH-mm-ss-SSS"
    const val REQUEST_CODE_PERMISSIONS = 123
    val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
}