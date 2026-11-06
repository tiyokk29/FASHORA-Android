package com.fshora.app

import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.bumptech.glide.Glide
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_detail)

        initializeViews()
        setupBackButton()
        displayProduct()
    }

    private fun initializeViews() {
        ivDetailProduct = findViewById(R.id.ivDetailProduct)
        tvDetailCategory = findViewById(R.id.tvDetailCategory)
        tvDetailName = findViewById(R.id.tvDetailName)
        tvDetailPrice = findViewById(R.id.tvDetailPrice)
        tvDetailStock = findViewById(R.id.tvDetailStock)
        tvDetailDescription = findViewById(R.id.tvDetailDescription)
        tvDetailSeller = findViewById(R.id.tvDetailSeller)
    }

    private fun setupBackButton() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun displayProduct() {

        val productName =
            intent.getStringExtra("PRODUCT_NAME") ?: "Nama Produk"

        val category =
            intent.getStringExtra("PRODUCT_CATEGORY") ?: "-"

        val description =
            intent.getStringExtra("PRODUCT_DESCRIPTION") ?: "Tidak ada deskripsi."

        val price =
            intent.getLongExtra("PRODUCT_PRICE", 0L)

        val stock =
            intent.getLongExtra("PRODUCT_STOCK", 0L)

        val imageUrl =
            intent.getStringExtra("PRODUCT_IMAGE_URL") ?: ""

        val sellerId =
            intent.getStringExtra("PRODUCT_SELLER_ID") ?: "-"

        tvDetailName.text = productName
        tvDetailCategory.text = category
        tvDetailDescription.text = description
        tvDetailStock.text = "$stock tersedia"
        tvDetailSeller.text = "ID Penjual: $sellerId"

        val currencyFormat =
            NumberFormat.getCurrencyInstance(Locale("id", "ID"))

        tvDetailPrice.text = currencyFormat.format(price)

        if (imageUrl.isNotBlank()) {
            Glide.with(this)
                .load(imageUrl)
                .centerCrop()
                .placeholder(R.drawable.bg_product_card)
                .error(R.drawable.bg_product_card)
                .into(ivDetailProduct)
        } else {
            ivDetailProduct.setImageResource(
                R.drawable.bg_product_card
            )
        }
    }
}
