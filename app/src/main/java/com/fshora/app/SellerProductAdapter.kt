
package com.fshora.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import java.text.NumberFormat
import java.util.Locale

class SellerProductAdapter(
    private var products: List<Product>,
    private val onEditClick: (Product) -> Unit,
    private val onDeleteClick: (Product) -> Unit
) : RecyclerView.Adapter<SellerProductAdapter.SellerProductViewHolder>() {

    class SellerProductViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val ivProduct: ImageView =
            itemView.findViewById(R.id.ivSellerProduct)

        val tvCategory: TextView =
            itemView.findViewById(R.id.tvSellerProductCategory)

        val tvName: TextView =
            itemView.findViewById(R.id.tvSellerProductName)

        val tvPrice: TextView =
            itemView.findViewById(R.id.tvSellerProductPrice)

        val tvStock: TextView =
            itemView.findViewById(R.id.tvSellerProductStock)

        val btnEdit: MaterialButton =
            itemView.findViewById(R.id.btnEditProduct)

        val btnDelete: MaterialButton =
            itemView.findViewById(R.id.btnDeleteProduct)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SellerProductViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_seller_product, parent, false)

        return SellerProductViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: SellerProductViewHolder,
        position: Int
    ) {
        val product = products[position]

        holder.tvName.text = product.name
        holder.tvCategory.text = product.category
        holder.tvStock.text = "Stok: ${product.stock}"

        holder.tvPrice.text = NumberFormat
            .getCurrencyInstance(Locale("id", "ID"))
            .format(product.price)

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

        holder.btnEdit.setOnClickListener {
            onEditClick(product)
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick(product)
        }
    }

    override fun getItemCount(): Int {
        return products.size
    }

    fun updateProducts(newProducts: List<Product>) {
        products = newProducts.toList()
        notifyDataSetChanged()
    }
}
