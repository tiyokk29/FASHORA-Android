package com.fshora.app

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Locale

class CustomerDashboardActivity : ComponentActivity() {

    private lateinit var etSearch: EditText
    private lateinit var rvProducts: RecyclerView
    private lateinit var tvProductMessage: TextView
    private lateinit var productAdapter: ProductAdapter

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var selectedCategory = "Semua"
    private var searchKeyword = ""

    private val products = mutableListOf<Product>()
    private val categoryViews = mutableMapOf<Int, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_dashboard)

        if (auth.currentUser == null) {
            openLogin()
            return
        }

        initializeViews()
        setupGreeting()
        setupRecyclerView()
        setupCategory()
        setupSearch()
        setupLogout()
        setupBottomNavigation()
        loadProducts()
    }

    private fun initializeViews() {
        etSearch = findViewById(R.id.etSearch)
        rvProducts = findViewById(R.id.rvProducts)

        tvProductMessage = TextView(this).apply {
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(
                ContextCompat.getColor(
                    this@CustomerDashboardActivity,
                    R.color.text_secondary
                )
            )
            setPadding(24, 40, 24, 40)
            visibility = View.GONE
        }

        val parent = rvProducts.parent as ViewGroup
        val index = parent.indexOfChild(rvProducts)
        parent.addView(tvProductMessage, index)
    }

    private fun setupGreeting() {
        val greeting = findViewById<TextView>(R.id.tvGreeting)

        val userName = intent.getStringExtra("USER_NAME")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: "Fashion Lover"

        greeting.text = "Halo, $userName!"
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter(emptyList()) { product ->

            val detailIntent = Intent(
                this,
                ProductDetailActivity::class.java
            ).apply {
                putExtra("PRODUCT_ID", product.id)
                putExtra("PRODUCT_NAME", product.name)
                putExtra("PRODUCT_CATEGORY", product.category)
                putExtra("PRODUCT_DESCRIPTION", product.description)
                putExtra("PRODUCT_PRICE", product.price)
                putExtra("PRODUCT_STOCK", product.stock)
                putExtra("PRODUCT_IMAGE_URL", product.imageUrl)
                putExtra("PRODUCT_SELLER_ID", product.sellerId)
            }

            startActivity(detailIntent)
        }

        rvProducts.apply {
            layoutManager = GridLayoutManager(
                this@CustomerDashboardActivity,
                2
            )
            adapter = productAdapter
            isNestedScrollingEnabled = false
            setHasFixedSize(false)
        }
    }

    private fun setupCategory() {
        categoryViews.clear()

        val categories = mapOf(
            R.id.catAll to "Semua",
            R.id.catMen to "Pria",
            R.id.catWomen to "Wanita",
            R.id.catShoes to "Sepatu",
            R.id.catBags to "Tas"
        )

        categoryViews.putAll(categories)

        categories.forEach { (viewId, category) ->

            findViewById<View>(viewId).setOnClickListener {
                selectedCategory = category
                updateCategoryAppearance()
                filterProducts()
            }
        }

        updateCategoryAppearance()
    }

    private fun updateCategoryAppearance() {
        categoryViews.forEach { (viewId, category) ->

            val categoryView = findViewById<TextView>(viewId)
            val isSelected = category == selectedCategory

            if (isSelected) {
                categoryView.setBackgroundResource(
                    R.drawable.bg_category_selected
                )
                categoryView.setTextColor(
                    ContextCompat.getColor(this, R.color.white)
                )
                categoryView.setTypeface(
                    null,
                    android.graphics.Typeface.BOLD
                )
            } else {
                categoryView.setBackgroundResource(
                    R.drawable.bg_category
                )
                categoryView.setTextColor(
                    ContextCompat.getColor(this, R.color.primary)
                )
                categoryView.setTypeface(
                    null,
                    android.graphics.Typeface.NORMAL
                )
            }
        }
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                searchKeyword = s.toString().trim()
                filterProducts()
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupLogout() {
        findViewById<View>(R.id.btnCustomerLogout)
            .setOnClickListener {

                AlertDialog.Builder(this)
                    .setTitle("Konfirmasi Logout")
                    .setMessage(
                        "Apakah kamu yakin ingin keluar dari akun FASHORA?"
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
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavCustomer)
        val catalogContainer = findViewById<ScrollView>(R.id.layoutCatalogContainer)
        val ordersContainer = findViewById<LinearLayout>(R.id.layoutOrdersContainer)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_catalog -> {
                    catalogContainer.visibility = View.VISIBLE
                    ordersContainer.visibility = View.GONE
                    loadProducts()
                    true
                }
                R.id.nav_orders -> {
                    catalogContainer.visibility = View.GONE
                    ordersContainer.visibility = View.VISIBLE
                    loadMyOrders()
                    true
                }
                else -> false
            }
        }
    }

    private fun loadProducts() {
        showMessage("Memuat produk...")

        db.collection("products")
            .whereEqualTo("isAvailable", true)
            .get()
            .addOnSuccessListener { result ->

                products.clear()

                for (document in result.documents) {

                    products.add(
                        Product(
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
                    )
                }

                filterProducts()
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

    private fun loadMyOrders() {
        val buyerId = auth.currentUser?.uid ?: return
        val orderListLayout = findViewById<LinearLayout>(R.id.llOrderList)
        orderListLayout.removeAllViews()

        val statusView = TextView(this).apply {
            text = "Memuat riwayat pesanan..."
            textSize = 14f
            setTextColor(
                ContextCompat.getColor(
                    this@CustomerDashboardActivity,
                    R.color.text_secondary
                )
            )
        }
        orderListLayout.addView(statusView)

        db.collection("orders")
            .whereEqualTo("buyerId", buyerId)
            .get()
            .addOnSuccessListener { snapshot ->
                orderListLayout.removeAllViews()
                if (snapshot.isEmpty) {
                    statusView.text = "Belum ada riwayat pesanan."
                    orderListLayout.addView(statusView)
                    return@addOnSuccessListener
                }

                val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                for (doc in snapshot.documents) {
                    val name = doc.getString("productName") ?: "Produk"
                    val qty = doc.getLong("quantity") ?: 1L
                    val total = doc.getLong("totalPrice") ?: 0L
                    val status = doc.getString("status") ?: "Diproses"
                    val address = doc.getString("shippingAddress") ?: "-"

                    val card = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setBackgroundResource(R.drawable.bg_category)
                        setPadding(36, 30, 36, 30)
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { bottomMargin = 24 }
                    }

                    val tvTitle = TextView(this).apply {
                        text = "$name (${qty}x)"
                        textSize = 16f
                        setTypeface(null, android.graphics.Typeface.BOLD)
                        setTextColor(
                            ContextCompat.getColor(
                                this@CustomerDashboardActivity,
                                R.color.text_primary
                            )
                        )
                    }

                    val tvDetail = TextView(this).apply {
                        text = "Total: ${formatter.format(total)}\nAlamat: $address\nStatus: $status"
                        textSize = 13f
                        setPadding(0, 12, 0, 0)
                        setTextColor(
                            ContextCompat.getColor(
                                this@CustomerDashboardActivity,
                                R.color.text_secondary
                            )
                        )
                    }

                    card.addView(tvTitle)
                    card.addView(tvDetail)
                    orderListLayout.addView(card)
                }
            }
            .addOnFailureListener { e ->
                orderListLayout.removeAllViews()
                statusView.text = "Gagal memuat pesanan: ${e.localizedMessage ?: "Periksa koneksi."}"
                orderListLayout.addView(statusView)
            }
    }

    private fun filterProducts() {
        val filteredProducts = products.filter { product ->

            val categoryMatch =
                selectedCategory == "Semua" ||
                        product.category.equals(
                            selectedCategory,
                            ignoreCase = true
                        )

            val searchMatch = product.name.contains(
                searchKeyword,
                ignoreCase = true
            )

            categoryMatch && searchMatch
        }

        if (filteredProducts.isEmpty()) {
            showMessage("Produk tidak ditemukan.")
        } else {
            tvProductMessage.visibility = View.GONE
            rvProducts.visibility = View.VISIBLE
            productAdapter.updateProducts(filteredProducts)
        }
    }

    private fun showMessage(message: String) {
        rvProducts.visibility = View.GONE
        tvProductMessage.visibility = View.VISIBLE
        tvProductMessage.text = message

        tvProductMessage.setTextColor(
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
        if (::rvProducts.isInitialized && auth.currentUser != null) {
            loadProducts()
        }
    }
}