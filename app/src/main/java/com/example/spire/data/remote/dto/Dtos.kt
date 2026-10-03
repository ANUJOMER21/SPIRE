package com.example.spire.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProductDto(
    val id: Int,
    val title: String,
    val description: String = "",
    val category: String = "",
    val price: Double = 0.0,
    val rating: Double = 0.0,
    val stock: Int = 0,
    val brand: String? = null,
    val thumbnail: String = "",
    val images: List<String> = emptyList(),
)

@Serializable
data class ProductsResponse(
    val products: List<ProductDto> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class CategoryDto(
    val slug: String,
    val name: String,
)
