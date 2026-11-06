
package com.fshora.app

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.view.setPadding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val role = intent.getStringExtra("USER_ROLE")
        val userName = intent.getStringExtra("USER_NAME")
        val userUid = intent.getStringExtra("USER_UID")

        if (FirebaseAuth.getInstance().currentUser == null ||
            role.isNullOrEmpty() ||
            userUid.isNullOrEmpty()
        ) {
            openLogin()
            return
        }

        when (role) {
            "customer" -> {
                val dashboardIntent = Intent(
                    this,
                    CustomerDashboardActivity::class.java
                )

                dashboardIntent.putExtra("USER_NAME", userName)
                dashboardIntent.putExtra("USER_UID", userUid)

                startActivity(dashboardIntent)
                finish()
            }

            "seller" -> {
                showSellerComingSoon(userName ?: "Seller")
            }

            else -> {
                FirebaseAuth.getInstance().signOut()
                openLogin()
            }
        }
    }

    private fun showSellerComingSoon(name: String) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(32)
            setBackgroundColor(
                android.graphics.Color.rgb(247, 248, 245)
            )
        }

        val title = TextView(this).apply {
            text = "Halo, $name!"
            textSize = 24f
            setTextColor(android.graphics.Color.rgb(35, 75, 60))
            gravity = android.view.Gravity.CENTER
        }

        val message = TextView(this).apply {
            text = "Dashboard Seller sedang dalam tahap pengembangan."
            textSize = 15f
            setTextColor(android.graphics.Color.DKGRAY)
            gravity = android.view.Gravity.CENTER
            setPadding(0, 20, 0, 20)
        }

        val logout = android.widget.Button(this).apply {
            text = "Logout"
            setOnClickListener {
                FirebaseAuth.getInstance().signOut()
                openLogin()
            }
        }

        container.addView(title)
        container.addView(message)
        container.addView(logout)

        setContentView(container)
    }

    private fun openLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
