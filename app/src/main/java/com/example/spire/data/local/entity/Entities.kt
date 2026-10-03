package com.example.spire.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val description: String,
    val category: String,
    val priceCents: Long,
    val rating: Double,
    val stock: Int,
    val brand: String?,
    val thumbnail: String,
    val images: List<String>,
    val cachedAt: Long,
)

@Entity(
    tableName = "feed_items",
    primaryKeys = ["feedKey", "position"],
    indices = [Index("productId")],
)
data class FeedItemEntity(
    val feedKey: String,
    val position: Int,
    val productId: Int,
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val slug: String,
    val name: String,
    val position: Int,
)

@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey val productId: Int,
    val title: String,
    val thumbnail: String,
    val priceCents: Long,
    val quantity: Int,
    val stock: Int,
    val addedAt: Long,
)
