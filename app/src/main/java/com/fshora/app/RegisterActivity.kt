package com.fshora.app

import android.os.Bundle
import android.util.Patterns
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var nameLayout: TextInputLayout
    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var confirmLayout: TextInputLayout

    private lateinit var nameInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var confirmInput: TextInputEditText

    private lateinit var roleGroup: RadioGroup
    private lateinit var registerButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        nameLayout = findViewById(R.id.tilFullName)
        emailLayout = findViewById(R.id.tilRegisterEmail)
        passwordLayout = findViewById(R.id.tilRegisterPassword)
        confirmLayout = findViewById(R.id.tilConfirmPassword)

        nameInput = findViewById(R.id.etFullName)
        emailInput = findViewById(R.id.etRegisterEmail)
        passwordInput = findViewById(R.id.etRegisterPassword)
        confirmInput = findViewById(R.id.etConfirmPassword)

        roleGroup = findViewById(R.id.rgAccountRole)
        registerButton = findViewById(R.id.btnRegister)

        val loginLink = findViewById<TextView>(R.id.tvGoLogin)

        loginLink.setOnClickListener {
            finish()
        }

        registerButton.setOnClickListener {
            registerUser()
        }
    }

    private fun registerUser() {
        val name = nameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString()
        val confirmPassword = confirmInput.text.toString()

        clearErrors()

        var valid = true

        if (name.isEmpty()) {
            nameLayout.error = "Nama lengkap wajib diisi"
            valid = false
        }

        if (email.isEmpty()) {
            emailLayout.error = "Email wajib diisi"
            valid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.error = "Format email tidak valid"
            valid = false
        }

        if (password.isEmpty()) {
            passwordLayout.error = "Password wajib diisi"
            valid = false
        } else if (password.length < 6) {
            passwordLayout.error = "Password minimal 6 karakter"
            valid = false
        }

        if (confirmPassword.isEmpty()) {
            confirmLayout.error = "Konfirmasi password wajib diisi"
            valid = false
        } else if (password != confirmPassword) {
            confirmLayout.error = "Password tidak sama"
            valid = false
        }

        if (roleGroup.checkedRadioButtonId == -1) {
            Toast.makeText(
                this,
                "Silakan pilih jenis akun",
                Toast.LENGTH_SHORT
            ).show()
            valid = false
        }

        if (!valid) return

        val role = if (roleGroup.checkedRadioButtonId == R.id.rbSeller) {
            "seller"
        } else {
            "customer"
        }

        setLoading(true)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->

                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser

                    if (firebaseUser == null) {
                        setLoading(false)
                        Toast.makeText(
                            this,
                            "Akun dibuat, tetapi data pengguna tidak ditemukan",
                            Toast.LENGTH_LONG
                        ).show()
                        return@addOnCompleteListener
                    }

                    val userData = hashMapOf(
                        "uid" to firebaseUser.uid,
                        "fullName" to name,
                        "email" to email,
                        "role" to role,
                        "createdAt" to FieldValue.serverTimestamp()
                    )

                    firestore.collection("users")
                        .document(firebaseUser.uid)
                        .set(userData)
                        .addOnSuccessListener {
                            auth.signOut()
                            setLoading(false)

                            Toast.makeText(
                                this,
                                "Registrasi berhasil! Silakan login.",
                                Toast.LENGTH_LONG
                            ).show()

                            finish()
                        }
                        .addOnFailureListener { exception ->

                            firebaseUser.delete()
                                .addOnCompleteListener { deleteTask ->
                                    auth.signOut()
                                    setLoading(false)

                                    val message = if (deleteTask.isSuccessful) {
                                        "Gagal menyimpan profil: ${exception.localizedMessage}"
                                    } else {
                                        "Profil gagal disimpan dan akun perlu diperiksa di Firebase Authentication."
                                    }

                                    Toast.makeText(
                                        this,
                                        message,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        }

                } else {
                    setLoading(false)

                    Toast.makeText(
                        this,
                        task.exception?.localizedMessage
                            ?: "Registrasi gagal. Silakan coba lagi.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun clearErrors() {
        nameLayout.error = null
        emailLayout.error = null
        passwordLayout.error = null
        confirmLayout.error = null
    }

    private fun setLoading(isLoading: Boolean) {
        registerButton.isEnabled = !isLoading
        registerButton.text = if (isLoading) {
            "MEMBUAT AKUN..."
        } else {
            "BUAT AKUN"
        }
    }
}
