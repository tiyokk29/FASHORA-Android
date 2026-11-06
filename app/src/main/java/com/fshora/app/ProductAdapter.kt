package com.fshora.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.NumberFormat
import java.util.Locale

class ProductAdapter(
    private var products: List<Product>,
    private val onProductClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val ivProduct: ImageView =
            itemView.findViewById(R.id.ivProduct)

        val tvProductName: TextView =
            itemView.findViewById(R.id.tvProductName)

        val tvProductCategory: TextView =
            itemView.findViewById(R.id.tvProductCategory)

        val tvProductPrice: TextView =
            itemView.findViewById(R.id.tvProductPrice)

        val tvProductStock: TextView =
            itemView.findViewById(R.id.tvProductStock)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ProductViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)

        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ProductViewHolder,
        position: Int
    ) {
        val product = products[position]

        holder.tvProductName.text = product.name
        holder.tvProductCategory.text = product.category
        holder.tvProductStock.text = "Stok: ${product.stock}"

        val currencyFormat =
            NumberFormat.getCurrencyInstance(Locale("id", "ID"))

        holder.tvProductPrice.text =
            currencyFormat.format(product.price)

        if (product.imageUrl.isNotBlank()) {
            Glide.with(holder.itemView.context)
                .load(product.imageUrl)
                .centerCrop()
                .placeholder(R.drawable.bg_product_card)
                .error(R.drawable.bg_product_card)
                .into(holder.ivProduct)
        } else {
            holder.ivProduct.setImageResource(
                R.drawable.bg_product_card
            )
        }

        holder.itemView.setOnClickListener {
            onProductClick(product)
        }
    }

    override fun getItemCount(): Int = products.size

    fun updateProducts(newProducts: List<Product>) {
        products = newProducts
        notifyDataSetChanged()
    }
}
