package com.fshora.app

import android.app.AlertDialog
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Locale

class ProductDetailActivity : ComponentActivity() {

    private lateinit var ivDetailProduct: ImageView
    private lateinit var tvDetailCategory: TextView
    private lateinit var tvDetailName: TextView
    private lateinit var tvDetailPrice: TextView
    private lateinit var tvDetailStock: TextView
    private lateinit var tvDetailDescription: TextView
    private lateinit var tvDetailSeller: TextView
    private lateinit var btnBuyNow: Button

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val timeoutHandler = Handler(Looper.getMainLooper())

    private var productId: String = ""
    private var productName: String = ""
    private var productPrice: Long = 0L
    private var currentStock: Long = 0L
    private var productImageUrl: String = ""
    private var sellerId: String = ""
    private var isProcessingOrder: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_detail)

        initializeViews()
        setupBackButton()
        displayProduct()
        setupBuyButton()
    }

    private fun initializeViews() {
        ivDetailProduct = findViewById(R.id.ivDetailProduct)
        tvDetailCategory = findViewById(R.id.tvDetailCategory)
        tvDetailName = findViewById(R.id.tvDetailName)
        tvDetailPrice = findViewById(R.id.tvDetailPrice)
        tvDetailStock = findViewById(R.id.tvDetailStock)
        tvDetailDescription = findViewById(R.id.tvDetailDescription)
        tvDetailSeller = findViewById(R.id.tvDetailSeller)
        btnBuyNow = findViewById(R.id.btnBuyNow)
    }

    private fun setupBackButton() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun displayProduct() {
        productId = intent.getStringExtra("PRODUCT_ID") ?: ""
        productName = intent.getStringExtra("PRODUCT_NAME") ?: "Nama Produk"
        val category = intent.getStringExtra("PRODUCT_CATEGORY") ?: "-"
        val description = intent.getStringExtra("PRODUCT_DESCRIPTION") ?: "Tidak ada deskripsi."
        productPrice = intent.getLongExtra("PRODUCT_PRICE", 0L)
        currentStock = intent.getLongExtra("PRODUCT_STOCK", 0L)
        productImageUrl = intent.getStringExtra("PRODUCT_IMAGE_URL") ?: ""
        sellerId = intent.getStringExtra("PRODUCT_SELLER_ID") ?: ""

        tvDetailName.text = productName
        tvDetailCategory.text = category
        tvDetailDescription.text = description
        tvDetailPrice.text = formatRupiah(productPrice)

        updateStockUI()
        loadSellerName(sellerId)

        if (productImageUrl.isNotBlank()) {
            Glide.with(this)
                .load(productImageUrl)
                .centerCrop()
                .placeholder(R.drawable.bg_product_card)
                .error(R.drawable.bg_product_card)
                .into(ivDetailProduct)
        } else {
            ivDetailProduct.setImageResource(R.drawable.bg_product_card)
        }
    }

    private fun loadSellerName(uid: String) {
        if (uid.isBlank()) {
            tvDetailSeller.text = "Penjual: Toko FASHORA"
            return
        }

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val sellerName = doc.getString("name")
                    ?.takeIf { it.isNotBlank() }
                    ?: doc.getString("fullName")?.takeIf { it.isNotBlank() }
                    ?: "Toko FASHORA"
                tvDetailSeller.text = "Penjual: $sellerName"
            }
            .addOnFailureListener {
                tvDetailSeller.text = "Penjual: Toko FASHORA"
            }
    }

    private fun updateStockUI() {
        if (currentStock > 0) {
            tvDetailStock.text = "$currentStock tersedia"
            tvDetailStock.setTextColor(ContextCompat.getColor(this, R.color.success))
            btnBuyNow.isEnabled = true
            btnBuyNow.text = "Beli Sekarang"
        } else {
            tvDetailStock.text = "Stok Habis"
            tvDetailStock.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            btnBuyNow.isEnabled = false
            btnBuyNow.text = "Stok Habis"
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun setupBuyButton() {
        btnBuyNow.setOnClickListener {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Toast.makeText(this, "Silakan login terlebih dahulu.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (currentUser.uid == sellerId) {
                Toast.makeText(this, "Kamu tidak dapat membeli produkmu sendiri.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (currentStock <= 0L) {
                Toast.makeText(this, "Maaf, stok produk sedang habis.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isNetworkAvailable()) {
                showNetworkErrorDialog()
                return@setOnClickListener
            }

            showCheckoutDialog()
        }
    }

    private fun showCheckoutDialog() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(56, 32, 56, 16)
        }

        val tvQtyLabel = TextView(this).apply {
            text = "Jumlah Beli (Maks. $currentStock)"
            textSize = 13f
            setTextColor(ContextCompat.getColor(this@ProductDetailActivity, R.color.text_primary))
        }

        val etQuantity = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText("1")
            hint = "Masukkan jumlah"
        }

        val tvAddressLabel = TextView(this).apply {
            text = "Alamat Pengiriman"
            textSize = 13f
            setPadding(0, 24, 0, 0)
            setTextColor(ContextCompat.getColor(this@ProductDetailActivity, R.color.text_primary))
        }

        val etAddress = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            hint = "Contoh: Jl. Merdeka No. 10, Lhokseumawe"
            minLines = 2
        }

        val tvTotalPreview = TextView(this).apply {
            text = "Total Pembayaran: ${formatRupiah(productPrice)}"
            textSize = 15f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 28, 0, 8)
            setTextColor(ContextCompat.getColor(this@ProductDetailActivity, R.color.primary))
        }

        etQuantity.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val qty = s.toString().toLongOrNull() ?: 0L
                val total = qty * productPrice
                tvTotalPreview.text = "Total Pembayaran: ${formatRupiah(total)}"
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        container.addView(tvQtyLabel)
        container.addView(etQuantity)
        container.addView(tvAddressLabel)
        container.addView(etAddress)
        container.addView(tvTotalPreview)

        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Pembelian")
            .setView(container)
            .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("Buat Pesanan") { _, _ ->
                val qty = etQuantity.text.toString().trim().toLongOrNull() ?: 0L
                val address = etAddress.text.toString().trim()

                if (qty <= 0L) {
                    Toast.makeText(this, "Jumlah pembelian minimal 1.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (qty > currentStock) {
                    Toast.makeText(this, "Jumlah melebihi stok tersedia ($currentStock).", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (address.isEmpty()) {
                    Toast.makeText(this, "Alamat pengiriman wajib diisi.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (!isNetworkAvailable()) {
                    showNetworkErrorDialog()
                    return@setPositiveButton
                }

                processOrder(qty, address)
            }
            .show()
    }

    private fun processOrder(quantity: Long, shippingAddress: String) {
        val buyer = auth.currentUser ?: return
        if (productId.isBlank()) {
            Toast.makeText(this, "ID produk tidak valid.", Toast.LENGTH_SHORT).show()
            return
        }

        isProcessingOrder = true
        btnBuyNow.isEnabled = false
        btnBuyNow.text = "Memproses..."

        // Timeout 10 detik jika jaringan tiba-tiba terputus saat proses berlangsung
        val timeoutRunnable = Runnable {
            if (isProcessingOrder) {
                isProcessingOrder = false
                updateStockUI()
                showNetworkErrorDialog()
            }
        }
        timeoutHandler.postDelayed(timeoutRunnable, 10_000L)

        val totalPrice = quantity * productPrice
        val orderData = hashMapOf(
            "productId" to productId,
            "productName" to productName,
            "productImageUrl" to productImageUrl,
            "pricePerItem" to productPrice,
            "quantity" to quantity,
            "totalPrice" to totalPrice,
            "buyerId" to buyer.uid,
            "buyerEmail" to (buyer.email ?: "-"),
            "sellerId" to sellerId,
            "shippingAddress" to shippingAddress,
            "status" to "Diproses",
            "createdAt" to Timestamp.now()
        )

        val remainingStock = currentStock - quantity
        val productRef = db.collection("products").document(productId)

        db.collection("orders")
            .add(orderData)
            .addOnSuccessListener {
                timeoutHandler.removeCallbacks(timeoutRunnable)
                if (!isProcessingOrder) return@addOnSuccessListener
                isProcessingOrder = false

                val stockUpdates = hashMapOf<String, Any>(
                    "stock" to FieldValue.increment(-quantity)
                )
                if (remainingStock <= 0L) {
                    stockUpdates["isAvailable"] = false
                }

                productRef.update(stockUpdates)
                    .addOnCompleteListener {
                        currentStock = remainingStock
                        updateStockUI()
                        showOrderSuccessDialog(quantity, totalPrice)
                    }
            }
            .addOnFailureListener { e ->
                timeoutHandler.removeCallbacks(timeoutRunnable)
                if (!isProcessingOrder) return@addOnFailureListener
                isProcessingOrder = false
                updateStockUI()

                Toast.makeText(
                    this,
                    "Gagal membuat pesanan: ${e.localizedMessage ?: "Periksa koneksi internet."}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun showNetworkErrorDialog() {
        AlertDialog.Builder(this)
            .setTitle("Koneksi Terputus")
            .setMessage("Tidak ada koneksi internet. Aktifkan Wi-Fi atau data seluler terlebih dahulu untuk melanjutkan pembelian.")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showOrderSuccessDialog(quantity: Long, totalPrice: Long) {
        AlertDialog.Builder(this)
            .setTitle("Pesanan Berhasil!")
            .setMessage(
                "Pesanan untuk $quantity x \"$productName\" senilai ${formatRupiah(totalPrice)} " +
                        "telah dicatat dan sedang diproses oleh penjual."
            )
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun formatRupiah(amount: Long): String {
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        return currencyFormat.format(amount)
    }

    override fun onDestroy() {
        timeoutHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}