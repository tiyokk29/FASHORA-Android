
package com.fshora.app

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SellerDashboardActivity : ComponentActivity() {

    private lateinit var tvSellerGreeting: TextView
    private lateinit var tvTotalProducts: TextView
    private lateinit var tvSellerProductCount: TextView
    private lateinit var tvSellerMessage: TextView
    private lateinit var rvSellerProducts: RecyclerView

    private lateinit var sellerProductAdapter: SellerProductAdapter

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val sellerProducts = mutableListOf<Product>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seller_dashboard)

        if (auth.currentUser == null) {
            openLogin()
            return
        }

        initializeViews()
        setupGreeting()
        setupRecyclerView()
        setupButtons()
    }

    private fun initializeViews() {
        tvSellerGreeting = findViewById(R.id.tvSellerGreeting)
        tvTotalProducts = findViewById(R.id.tvTotalProducts)
        tvSellerProductCount = findViewById(R.id.tvSellerProductCount)
        tvSellerMessage = findViewById(R.id.tvSellerMessage)
        rvSellerProducts = findViewById(R.id.rvSellerProducts)
    }

    private fun setupGreeting() {
        val sellerName = intent.getStringExtra("USER_NAME")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: "Seller"

        tvSellerGreeting.text = "Halo, $sellerName!"
    }

    private fun setupRecyclerView() {
        sellerProductAdapter = SellerProductAdapter(
            products = emptyList(),

            onEditClick = { product ->
                openEditProduct(product)
            },

            onDeleteClick = { product ->
                showDeleteConfirmation(product)
            }
        )

        rvSellerProducts.apply {
            layoutManager = LinearLayoutManager(
                this@SellerDashboardActivity
            )
            adapter = sellerProductAdapter
            isNestedScrollingEnabled = true
        }
    }

    private fun setupButtons() {

        findViewById<View>(R.id.btnAddProduct)
            .setOnClickListener {

                val addProductIntent = Intent(
                    this,
                    AddProductActivity::class.java
                )

                startActivity(addProductIntent)
            }

        findViewById<ImageButton>(R.id.btnSellerLogout)
            .setOnClickListener {
                showLogoutConfirmation()
            }
    }

    private fun openEditProduct(product: Product) {
        val editIntent = Intent(
            this,
            EditProductActivity::class.java
        ).apply {
            putExtra("PRODUCT_ID", product.id)
        }

        startActivity(editIntent)
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Konfirmasi Logout")
            .setMessage(
                "Apakah kamu yakin ingin keluar dari akun Seller FASHORA?"
            )
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Logout") { _, _ ->
                auth.signOut()
                openLogin()
            }
            .show()
    }

    private fun showDeleteConfirmation(product: Product) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Produk")
            .setMessage(
                "Apakah kamu yakin ingin menghapus produk \"${product.name}\"?"
            )
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Hapus") { _, _ ->
                deleteProduct(product)
            }
            .show()
    }

    private fun deleteProduct(product: Product) {
        val currentSellerId = auth.currentUser?.uid

        if (currentSellerId == null) {
            openLogin()
            return
        }

        if (product.sellerId != currentSellerId) {
            Toast.makeText(
                this,
                "Kamu tidak memiliki izin menghapus produk ini.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        db.collection("products")
            .document(product.id)
            .delete()
            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Produk berhasil dihapus.",
                    Toast.LENGTH_SHORT
                ).show()

                loadSellerProducts()
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    "Gagal menghapus produk: ${
                        exception.localizedMessage
                            ?: "Terjadi kesalahan."
                    }",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun loadSellerProducts() {
        val sellerId = auth.currentUser?.uid ?: return

        showMessage("Memuat produk...")

        db.collection("products")
            .whereEqualTo("sellerId", sellerId)
            .get()
            .addOnSuccessListener { result ->

                sellerProducts.clear()

                for (document in result.documents) {

                    val product = Product(
                        id = document.id,
                        name = document.getString("name")
                            ?: "Tanpa Nama",
                        category = document.getString("category")
                            ?: "",
                        description = document.getString("description")
                            ?: "",
                        price = (
                                document.get("price") as? Number
                                )?.toLong() ?: 0L,
                        stock = (
                                document.get("stock") as? Number
                                )?.toLong() ?: 0L,
                        imageUrl = document.getString("imageUrl")
                            ?: "",
                        sellerId = document.getString("sellerId")
                            ?: "",
                        isAvailable = document.getBoolean("isAvailable")
                            ?: true
                    )

                    sellerProducts.add(product)
                }

                updateProductStatistics()

                if (sellerProducts.isEmpty()) {

                    sellerProductAdapter.updateProducts(emptyList())

                    showMessage(
                        "Belum ada produk. Yuk, tambahkan produk pertamamu!"
                    )

                } else {

                    tvSellerMessage.visibility = View.GONE
                    rvSellerProducts.visibility = View.VISIBLE

                    sellerProductAdapter.updateProducts(
                        sellerProducts
                    )
                }
            }
            .addOnFailureListener { exception ->

                showMessage(
                    "Gagal memuat produk.\n" +
                            (
                                    exception.localizedMessage
                                        ?: "Periksa koneksi internet."
                                    )
                )
            }
    }

    private fun updateProductStatistics() {
        val total = sellerProducts.size

        tvTotalProducts.text = total.toString()
        tvSellerProductCount.text = "$total produk"
    }

    private fun showMessage(message: String) {
        tvSellerMessage.text = message
        tvSellerMessage.visibility = View.VISIBLE
        rvSellerProducts.visibility = View.GONE

        tvSellerMessage.setTextColor(
            ContextCompat.getColor(
                this,
                R.color.text_secondary
            )
        )
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

    override fun onResume() {
        super.onResume()

        if (
            ::rvSellerProducts.isInitialized &&
            auth.currentUser != null
        ) {
            loadSellerProducts()
        }
    }
}
