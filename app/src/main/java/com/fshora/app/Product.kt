package com.fshora.app

data class Product(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val description: String = "",
    val price: Long = 0L,
    val stock: Long = 0L,
    val imageUrl: String = "",
    val sellerId: String = "",
    val isAvailable: Boolean = true
)
