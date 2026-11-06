
package com.fshora.app

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class CustomerDashboardActivity : ComponentActivity() {

    private lateinit var etSearch: EditText
    private lateinit var rvProducts: RecyclerView
    private lateinit var tvProductMessage: TextView
    private lateinit var productAdapter: ProductAdapter

    private val db = FirebaseFirestore.getInstance()

    private var selectedCategory = "Semua"
    private var searchKeyword = ""

    private val products = mutableListOf<Product>()

    private val categoryViews = mutableMapOf<Int, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_dashboard)

        initializeViews()
        setupGreeting()
        setupRecyclerView()
        setupCategory()
        setupSearch()
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
            // Navigasi detail produk akan dibuat pada tahap berikutnya.
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
                    ContextCompat.getColor(
                        this,
                        R.color.white
                    )
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
                    ContextCompat.getColor(
                        this,
                        R.color.primary
                    )
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

    private fun loadProducts() {
        showMessage("Memuat produk...")

        db.collection("products")
            .whereEqualTo("isAvailable", true)
            .get()
            .addOnSuccessListener { result ->

                products.clear()

                for (document in result.documents) {

                    val product = Product(
                        id = document.id,
                        name = document.getString("name") ?: "Tanpa Nama",
                        category = document.getString("category") ?: "",
                        description = document.getString("description") ?: "",
                        price = (document.get("price") as? Number)?.toLong() ?: 0L,
                        stock = (document.get("stock") as? Number)?.toLong() ?: 0L,
                        imageUrl = document.getString("imageUrl") ?: "",
                        sellerId = document.getString("sellerId") ?: "",
                        isAvailable = document.getBoolean("isAvailable") ?: true
                    )

                    products.add(product)
                }

                filterProducts()
            }
            .addOnFailureListener { exception ->

                showMessage(
                    "Gagal memuat produk.\n" +
                            (exception.localizedMessage
                                ?: "Periksa koneksi internet.")
                )
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

            val searchMatch =
                product.name.contains(
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
}
