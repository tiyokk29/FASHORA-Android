
package com.fshora.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentUser = auth.currentUser
        val role = intent.getStringExtra("USER_ROLE")
        val userName = intent.getStringExtra("USER_NAME")
        val userUid = intent.getStringExtra("USER_UID")

        // Pastikan pengguna sudah login dan UID sesuai.
        if (
            currentUser == null ||
            role.isNullOrEmpty() ||
            userUid.isNullOrEmpty() ||
            currentUser.uid != userUid
        ) {
            openLogin()
            return
        }

        when (role) {

            "customer" -> {
                val dashboardIntent = Intent(
                    this,
                    CustomerDashboardActivity::class.java
                ).apply {
                    putExtra("USER_NAME", userName)
                    putExtra("USER_UID", userUid)
                }

                startActivity(dashboardIntent)
                finish()
            }

            "seller" -> {
                val dashboardIntent = Intent(
                    this,
                    SellerDashboardActivity::class.java
                ).apply {
                    putExtra("USER_NAME", userName)
                    putExtra("USER_UID", userUid)
                }

                startActivity(dashboardIntent)
                finish()
            }

            else -> {
                auth.signOut()
                openLogin()
            }
        }
    }

    private fun openLogin() {
        val loginIntent = Intent(
            this,
            LoginActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(loginIntent)
        finish()
    }
}
