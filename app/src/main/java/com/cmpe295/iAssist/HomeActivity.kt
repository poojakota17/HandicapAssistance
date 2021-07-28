package com.cmpe295.iAssist

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class HomeActivity : AppCompatActivity() {
    private val permission = 200
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        btn_hearing.setOnClickListener {
            val intent = Intent(this@HomeActivity, HearingAssistanceActivity::class.java)
            startActivity(intent)
        }

        btn_mute.setOnClickListener {
            val intent = Intent(this@HomeActivity, SpeakingAssistanceActivity::class.java)
            startActivity(intent)
        }

    }
}