
package com.fshora.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.Executors

class EditProductActivity : ComponentActivity() {

    private lateinit var tilProductName: TextInputLayout
    private lateinit var tilProductCategory: TextInputLayout
    private lateinit var tilProductDescription: TextInputLayout
    private lateinit var tilProductPrice: TextInputLayout
    private lateinit var tilProductStock: TextInputLayout

    private lateinit var etProductName: TextInputEditText
    private lateinit var actProductCategory: MaterialAutoCompleteTextView
    private lateinit var etProductDescription: TextInputEditText
    private lateinit var etProductPrice: TextInputEditText
    private lateinit var etProductStock: TextInputEditText

    private lateinit var ivProductPreview: ImageView
    private lateinit var btnUpdateProduct: MaterialButton
    private lateinit var btnChangeImage: MaterialButton

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val executor = Executors.newSingleThreadExecutor()

    private val categories = listOf(
        "Pria",
        "Wanita",
        "Sepatu",
        "Tas"
    )

    private var productId = ""
    private var currentImageUrl = ""
    private var selectedImageUri: Uri? = null
    private var isSaving = false

    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->

            if (uri != null) {

                selectedImageUri = uri

                Glide.with(this)
                    .load(uri)
                    .centerCrop()
                    .into(ivProductPreview)

                btnChangeImage.text = "Ganti Foto"

                Toast.makeText(
                    this,
                    "Foto baru berhasil dipilih.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_product)

        if (auth.currentUser == null) {
            openLogin()
            return
        }

        productId = intent.getStringExtra("PRODUCT_ID") ?: ""

        if (productId.isBlank()) {
            Toast.makeText(
                this,
                "ID produk tidak ditemukan.",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        initializeViews()
        setupCategoryDropdown()
        setupButtons()
        loadProductData()
    }

    private fun initializeViews() {

        tilProductName = findViewById(R.id.tilEditProductName)
        tilProductCategory = findViewById(R.id.tilEditProductCategory)
        tilProductDescription =
            findViewById(R.id.tilEditProductDescription)
        tilProductPrice = findViewById(R.id.tilEditProductPrice)
        tilProductStock = findViewById(R.id.tilEditProductStock)

        etProductName = findViewById(R.id.etEditProductName)
        actProductCategory =
            findViewById(R.id.actEditProductCategory)
        etProductDescription =
            findViewById(R.id.etEditProductDescription)
        etProductPrice = findViewById(R.id.etEditProductPrice)
        etProductStock = findViewById(R.id.etEditProductStock)

        ivProductPreview =
            findViewById(R.id.ivEditProductPreview)

        btnUpdateProduct =
            findViewById(R.id.btnUpdateProduct)

        btnChangeImage =
            findViewById(R.id.btnChangeProductImage)
    }

    private fun setupCategoryDropdown() {

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            categories
        )

        actProductCategory.setAdapter(adapter)

        actProductCategory.setOnClickListener {
            actProductCategory.showDropDown()
        }
    }

    private fun setupButtons() {

        findViewById<ImageButton>(R.id.btnBackEditProduct)
            .setOnClickListener {
                finish()
            }

        findViewById<View>(R.id.btnCancelEditProduct)
            .setOnClickListener {
                finish()
            }

        btnChangeImage.setOnClickListener {
            imagePicker.launch("image/*")
        }

        btnUpdateProduct.setOnClickListener {
            updateProduct()
        }
    }

    private fun loadProductData() {

        val sellerId = auth.currentUser?.uid ?: return

        setLoadingState(true, "Memuat Produk...")

        db.collection("products")
            .document(productId)
            .get()
            .addOnSuccessListener { document ->

                if (!document.exists()) {

                    Toast.makeText(
                        this,
                        "Produk tidak ditemukan.",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()
                    return@addOnSuccessListener
                }

                val ownerId =
                    document.getString("sellerId") ?: ""

                if (ownerId != sellerId) {

                    Toast.makeText(
                        this,
                        "Kamu tidak memiliki akses ke produk ini.",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()
                    return@addOnSuccessListener
                }

                etProductName.setText(
                    document.getString("name") ?: ""
                )

                actProductCategory.setText(
                    document.getString("category") ?: "",
                    false
                )

                etProductDescription.setText(
                    document.getString("description") ?: ""
                )

                val price = (
                        document.get("price") as? Number
                        )?.toLong() ?: 0L

                val stock = (
                        document.get("stock") as? Number
                        )?.toLong() ?: 0L

                etProductPrice.setText(price.toString())
                etProductStock.setText(stock.toString())

                currentImageUrl =
                    document.getString("imageUrl") ?: ""

                displayProductImage()

                setLoadingState(false)
            }
            .addOnFailureListener { exception ->

                setLoadingState(false)

                Toast.makeText(
                    this,
                    "Gagal memuat produk: ${
                        exception.localizedMessage
                            ?: "Terjadi kesalahan."
                    }",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun displayProductImage() {

        if (currentImageUrl.isNotBlank()) {

            Glide.with(this)
                .load(currentImageUrl)
                .centerCrop()
                .placeholder(R.drawable.bg_product_card)
                .error(R.drawable.bg_product_card)
                .into(ivProductPreview)

        } else {

            ivProductPreview.setImageResource(
                R.drawable.bg_product_card
            )
        }
    }

    private fun updateProduct() {

        if (isSaving) return

        clearErrors()

        val sellerId = auth.currentUser?.uid

        if (sellerId == null) {
            openLogin()
            return
        }

        val name = etProductName.text.toString().trim()
        val category = actProductCategory.text.toString().trim()
        val description =
            etProductDescription.text.toString().trim()
        val priceText = etProductPrice.text.toString().trim()
        val stockText = etProductStock.text.toString().trim()

        var isValid = true

        if (name.length < 3 || name.length > 80) {
            tilProductName.error =
                "Nama produk harus terdiri dari 3–80 karakter."
            isValid = false
        }

        if (category !in categories) {
            tilProductCategory.error =
                "Silakan pilih kategori yang tersedia."
            isValid = false
        }

        if (description.isEmpty() || description.length > 1000) {
            tilProductDescription.error =
                "Deskripsi wajib diisi, maksimal 1000 karakter."
            isValid = false
        }

        val price = priceText.toLongOrNull()

        if (price == null || price < 0L) {
            tilProductPrice.error =
                "Masukkan harga yang valid."
            isValid = false
        }

        val stock = stockText.toLongOrNull()

        if (stock == null || stock < 0L) {
            tilProductStock.error =
                "Masukkan jumlah stok yang valid."
            isValid = false
        }

        if (!isValid) return

        isSaving = true

        setLoadingState(true, "Menyimpan Perubahan...")

        val imageUri = selectedImageUri

        if (imageUri != null) {

            uploadImageToCloudinary(imageUri) { imageUrl ->

                if (imageUrl == null) {

                    isSaving = false
                    setLoadingState(false)

                    Toast.makeText(
                        this,
                        "Gagal mengunggah foto. Periksa koneksi internet.",
                        Toast.LENGTH_LONG
                    ).show()

                    return@uploadImageToCloudinary
                }

                saveProductChanges(
                    name,
                    category,
                    description,
                    price!!,
                    stock!!,
                    imageUrl
                )
            }

        } else {

            saveProductChanges(
                name,
                category,
                description,
                price!!,
                stock!!,
                currentImageUrl
            )
        }
    }

    private fun uploadImageToCloudinary(
        imageUri: Uri,
        callback: (String?) -> Unit
    ) {

        executor.execute {

            var connection: HttpURLConnection? = null

            try {

                val boundary = "----FASHORA${UUID.randomUUID()}"

                val mimeType =
                    contentResolver.getType(imageUri) ?: "image/jpeg"

                connection = URL(
                    "https://api.cloudinary.com/v1_1/wjcoire1/image/upload"
                ).openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.connectTimeout = 30000
                connection.readTimeout = 30000

                connection.setRequestProperty(
                    "Content-Type",
                    "multipart/form-data; boundary=$boundary"
                )

                val output = DataOutputStream(
                    connection.outputStream
                )

                output.writeBytes("--$boundary\r\n")
                output.writeBytes(
                    "Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n"
                )
                output.writeBytes("fashora_products\r\n")

                output.writeBytes("--$boundary\r\n")
                output.writeBytes(
                    "Content-Disposition: form-data; name=\"file\"; filename=\"product_image\"\r\n"
                )
                output.writeBytes(
                    "Content-Type: $mimeType\r\n\r\n"
                )

                val input = contentResolver.openInputStream(imageUri)
                    ?: throw Exception("Tidak dapat membaca foto.")

                input.use { stream ->
                    stream.copyTo(output)
                }

                output.writeBytes("\r\n--$boundary--\r\n")
                output.flush()
                output.close()

                val responseCode = connection.responseCode

                val responseStream =
                    if (responseCode in 200..299) {
                        connection.inputStream
                    } else {
                        connection.errorStream
                    }

                val responseText = responseStream
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    ?: ""

                if (responseCode !in 200..299) {
                    throw Exception(
                        "Cloudinary error ($responseCode): $responseText"
                    )
                }

                val json = JSONObject(responseText)

                val secureUrl =
                    json.optString("secure_url", "")

                if (secureUrl.isBlank()) {
                    throw Exception("URL foto tidak ditemukan.")
                }

                runOnUiThread {
                    callback(secureUrl)
                }

            } catch (exception: Exception) {

                android.util.Log.e(
                    "FASHORA_UPLOAD",
                    "Upload foto edit gagal",
                    exception
                )

                runOnUiThread {
                    callback(null)
                }

            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun saveProductChanges(
        name: String,
        category: String,
        description: String,
        price: Long,
        stock: Long,
        imageUrl: String
    ) {

        val updatedData = hashMapOf<String, Any>(
            "name" to name,
            "category" to category,
            "description" to description,
            "price" to price,
            "stock" to stock,
            "imageUrl" to imageUrl
        )

        db.collection("products")
            .document(productId)
            .update(updatedData)
            .addOnSuccessListener {

                currentImageUrl = imageUrl

                Toast.makeText(
                    this,
                    "Produk berhasil diperbarui!",
                    Toast.LENGTH_SHORT
                ).show()

                setResult(RESULT_OK)
                finish()
            }
            .addOnFailureListener { exception ->

                isSaving = false

                setLoadingState(false)

                Toast.makeText(
                    this,
                    "Gagal memperbarui produk: ${
                        exception.localizedMessage
                            ?: "Terjadi kesalahan."
                    }",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun clearErrors() {

        tilProductName.error = null
        tilProductCategory.error = null
        tilProductDescription.error = null
        tilProductPrice.error = null
        tilProductStock.error = null
    }

    private fun setLoadingState(
        isLoading: Boolean,
        buttonText: String = "Simpan Perubahan"
    ) {

        btnUpdateProduct.isEnabled = !isLoading
        btnUpdateProduct.text = buttonText

        btnChangeImage.isEnabled = !isLoading
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

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}
