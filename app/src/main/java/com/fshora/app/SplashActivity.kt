package com.fshora.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SplashActivity : ComponentActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val cekSesiDanPindah = Runnable {
        checkUserSession()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        handler.postDelayed(cekSesiDanPindah, 1500)
    }

    private fun checkUserSession() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            goToLogin()
            return
        }

        firestore.collection("users")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    auth.signOut()
                    goToLogin()
                    return@addOnSuccessListener
                }

                val role = document.getString("role")
                val fullName = document.getString("fullName")
                    ?: document.getString("name")
                    ?: ""

                if (role == "customer" || role == "seller") {
                    val mainIntent = Intent(this, MainActivity::class.java).apply {
                        putExtra("USER_ROLE", role)
                        putExtra("USER_NAME", fullName)
                        putExtra("USER_UID", currentUser.uid)
                    }
                    startActivity(mainIntent)
                    finish()
                } else {
                    auth.signOut()
                    goToLogin()
                }
            }
            .addOnFailureListener {
                // Jika koneksi offline saat splash, arahkan ke login atau beri fallback aman
                goToLogin()
            }
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        handler.removeCallbacks(cekSesiDanPindah)
        super.onDestroy()
    }
}