package com.example.spire.domain.repository

import com.example.spire.domain.model.AddToCartResult
import com.example.spire.domain.model.Cart
import com.example.spire.domain.model.Category
import com.example.spire.domain.model.FeedPage
import com.example.spire.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {

    fun observeFeed(query: String, category: String?): Flow<List<Product>>

    suspend fun refreshFeed(query: String, category: String?, skip: Int): Result<FeedPage>

    fun observeCategories(): Flow<List<Category>>
    suspend fun refreshCategories(): Result<Unit>

    fun observeProduct(id: Int): Flow<Product?>
    suspend fun refreshProduct(id: Int): Result<Unit>
}

interface CartRepository {
    fun observeCart(): Flow<Cart>
    fun observeTotalItems(): Flow<Int>
    fun observeQuantity(productId: Int): Flow<Int>

    suspend fun add(product: Product): AddToCartResult
    suspend fun increment(productId: Int): Boolean
    suspend fun decrement(productId: Int)
    suspend fun remove(productId: Int)
    suspend fun clear()
}
