
package com.fshora.app

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout

    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText

    private lateinit var loginButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        emailLayout = findViewById(R.id.tilEmail)
        passwordLayout = findViewById(R.id.tilPassword)

        emailInput = findViewById(R.id.etEmail)
        passwordInput = findViewById(R.id.etPassword)

        loginButton = findViewById(R.id.btnLogin)

        val registerLink = findViewById<TextView>(R.id.tvGoRegister)
        val forgotPassword = findViewById<TextView>(R.id.tvForgotPassword)

        registerLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        loginButton.setOnClickListener {
            loginUser()
        }

        forgotPassword.setOnClickListener {
            Toast.makeText(
                this,
                "Fitur lupa password akan dibuat pada tahap berikutnya.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun loginUser() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString()

        emailLayout.error = null
        passwordLayout.error = null

        var valid = true

        if (email.isEmpty()) {
            emailLayout.error = "Email wajib diisi"
            valid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.error = "Format email tidak valid"
            valid = false
        }

        if (password.isEmpty()) {
            passwordLayout.error = "Password wajib diisi"
            valid = false
        }

        if (!valid) return

        setLoading(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->

                if (task.isSuccessful) {
                    val user = auth.currentUser

                    if (user == null) {
                        setLoading(false)
                        showMessage("Data pengguna tidak ditemukan.")
                        return@addOnCompleteListener
                    }

                    firestore.collection("users")
                        .document(user.uid)
                        .get()
                        .addOnSuccessListener { document ->

                            if (!document.exists()) {
                                auth.signOut()
                                setLoading(false)
                                showMessage("Profil pengguna tidak ditemukan.")
                                return@addOnSuccessListener
                            }

                            val role = document.getString("role")

                            if (role != "customer" && role != "seller") {
                                auth.signOut()
                                setLoading(false)
                                showMessage("Jenis akun tidak valid.")
                                return@addOnSuccessListener
                            }

                            val fullName = document.getString("fullName") ?: ""

                            setLoading(false)

                            val intent = Intent(this, MainActivity::class.java)
                            intent.putExtra("USER_ROLE", role)
                            intent.putExtra("USER_NAME", fullName)
                            intent.putExtra("USER_UID", user.uid)

                            startActivity(intent)
                            finish()
                        }
                        .addOnFailureListener { exception ->
                            auth.signOut()
                            setLoading(false)
                            showMessage(
                                exception.localizedMessage
                                    ?: "Gagal mengambil profil pengguna."
                            )
                        }

                } else {
                    setLoading(false)
                    showMessage(
                        task.exception?.localizedMessage
                            ?: "Login gagal. Periksa email dan password."
                    )
                }
            }
    }

    private fun setLoading(isLoading: Boolean) {
        loginButton.isEnabled = !isLoading
        loginButton.text = if (isLoading) {
            "MEMERIKSA AKUN..."
        } else {
            "MASUK"
        }
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
