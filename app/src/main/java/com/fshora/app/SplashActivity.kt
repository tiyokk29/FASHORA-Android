package com.fshora.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity

class SplashActivity : ComponentActivity() {

    private val handler = Handler(Looper.getMainLooper())

    private val pindahHalaman = Runnable {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        handler.postDelayed(pindahHalaman, 2000)
    }

    override fun onDestroy() {
        handler.removeCallbacks(pindahHalaman)
        super.onDestroy()
    }
}
