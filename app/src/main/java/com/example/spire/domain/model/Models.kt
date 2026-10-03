package com.example.spire.domain.model

data class Product(
    val id: Int,
    val title: String,
    val description: String,
    val category: String,

    val priceCents: Long,
    val rating: Double,
    val stock: Int,
    val brand: String?,
    val thumbnail: String,
    val images: List<String>,
)

data class Category(
    val slug: String,
    val name: String,
)

data class CartItem(
    val productId: Int,
    val title: String,
    val thumbnail: String,
    val priceCents: Long,
    val quantity: Int,
    val stock: Int,
) {
    val lineTotalCents: Long get() = priceCents * quantity
    val canIncrease: Boolean get() = quantity < stock
}

data class Cart(val items: List<CartItem> = emptyList()) {
    val totalItems: Int get() = items.sumOf { it.quantity }
    val totalCents: Long get() = items.sumOf { it.lineTotalCents }
    val isEmpty: Boolean get() = items.isEmpty()
}

enum class AddToCartResult { ADDED, MAX_REACHED, OUT_OF_STOCK }

data class FeedPage(val endReached: Boolean)
