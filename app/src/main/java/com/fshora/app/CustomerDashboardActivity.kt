
package com.fshora.app

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Locale

class CustomerDashboardActivity : ComponentActivity() {

    private lateinit var etSearch: EditText
    private lateinit var productGrid: GridLayout

    private val db = FirebaseFirestore.getInstance()

    private var selectedCategory = "Semua"
    private var searchKeyword = ""

    private val products = mutableListOf<Product>()

    data class Product(
        val id: String,
        val name: String,
        val category: String,
        val description: String,
        val price: Long,
        val stock: Long
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_dashboard)

        val greeting = findViewById<TextView>(R.id.tvGreeting)

        val userName = intent.getStringExtra("USER_NAME")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: "Fashion Lover"

        greeting.text = "Halo, $userName!"

        etSearch = findViewById(R.id.etSearch)
        productGrid = findViewById(R.id.productGrid)

        setupCategory()
        setupSearch()
        loadProducts()
    }

    private fun setupCategory() {
        val categories = mapOf(
            R.id.catAll to "Semua",
            R.id.catMen to "Pria",
            R.id.catWomen to "Wanita",
            R.id.catShoes to "Sepatu",
            R.id.catBags to "Tas"
        )

        categories.forEach { (viewId, category) ->
            findViewById<View>(viewId).setOnClickListener {
                selectedCategory = category
                filterProducts()
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
                        stock = (document.get("stock") as? Number)?.toLong() ?: 0L
                    )

                    products.add(product)
                }

                filterProducts()
            }
            .addOnFailureListener { exception ->
                showMessage(
                    "Gagal memuat produk.\n" +
                            (exception.localizedMessage ?: "Periksa koneksi internet.")
                )
            }
    }

    private fun filterProducts() {
        productGrid.removeAllViews()

        val filteredProducts = products.filter { product ->

            val categoryMatch =
                selectedCategory == "Semua" ||
                        product.category.equals(selectedCategory, ignoreCase = true)

            val searchMatch =
                product.name.contains(searchKeyword, ignoreCase = true)

            categoryMatch && searchMatch
        }

        if (filteredProducts.isEmpty()) {
            showMessage("Produk tidak ditemukan.")
            return
        }

        filteredProducts.forEach { product ->
            productGrid.addView(createProductCard(product))
        }
    }

    private fun createProductCard(product: Product): TextView {

        val currencyFormat = NumberFormat.getCurrencyInstance(
            Locale("id", "ID")
        )

        val card = TextView(this).apply {
            text = """
                ${product.name}

                ${product.category}
                ${currencyFormat.format(product.price)}
                Stok: ${product.stock}

                ${product.description}
            """.trimIndent()

            textSize = 14f
            setTextColor(getColor(R.color.text_primary))
            setPadding(24, 24, 24, 24)
            setBackgroundResource(R.drawable.bg_product_card)
            gravity = Gravity.CENTER_VERTICAL
        }

        val params = GridLayout.LayoutParams().apply {
            width = 0
            height = GridLayout.LayoutParams.WRAP_CONTENT

            columnSpec = GridLayout.spec(
                GridLayout.UNDEFINED,
                1f
            )

            setMargins(8, 8, 8, 8)
        }

        card.layoutParams = params

        return card
    }

    private fun showMessage(message: String) {
        productGrid.removeAllViews()

        val messageView = TextView(this).apply {
            text = message
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(getColor(R.color.text_secondary))
            setPadding(24, 40, 24, 40)
        }

        val params = GridLayout.LayoutParams().apply {
            width = GridLayout.LayoutParams.MATCH_PARENT
            height = GridLayout.LayoutParams.WRAP_CONTENT
            columnSpec = GridLayout.spec(0, 2)
        }

        messageView.layoutParams = params

        productGrid.addView(messageView)
    }
}
