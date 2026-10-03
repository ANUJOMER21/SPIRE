package com.example.spire.data.mapper

import com.example.spire.data.local.entity.CartItemEntity
import com.example.spire.data.local.entity.CategoryEntity
import com.example.spire.data.local.entity.ProductEntity
import com.example.spire.data.remote.dto.CategoryDto
import com.example.spire.data.remote.dto.ProductDto
import com.example.spire.domain.model.CartItem
import com.example.spire.domain.model.Category
import com.example.spire.domain.model.Product

fun Double.toCents(): Long = Math.round(this * 100)

fun ProductDto.toEntity(now: Long = System.currentTimeMillis()) = ProductEntity(
    id = id,
    title = title,
    description = description,
    category = category,
    priceCents = price.toCents(),
    rating = rating,
    stock = stock,
    brand = brand,
    thumbnail = thumbnail,
    images = images,
    cachedAt = now,
)

fun ProductEntity.toDomain() = Product(
    id = id,
    title = title,
    description = description,
    category = category,
    priceCents = priceCents,
    rating = rating,
    stock = stock,
    brand = brand,
    thumbnail = thumbnail,
    images = images,
)

fun CategoryEntity.toDomain() = Category(slug = slug, name = name)

fun CategoryDto.toEntity(position: Int) = CategoryEntity(slug = slug, name = name, position = position)

fun CartItemEntity.toDomain() = CartItem(
    productId = productId,
    title = title,
    thumbnail = thumbnail,
    priceCents = priceCents,
    quantity = quantity,
    stock = stock,
)

fun Product.toCartEntity(now: Long = System.currentTimeMillis()) = CartItemEntity(
    productId = id,
    title = title,
    thumbnail = thumbnail,
    priceCents = priceCents,
    quantity = 1,
    stock = stock,
    addedAt = now,
)
