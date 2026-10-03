package com.example.spire.data.repository

import com.example.spire.data.local.dao.CartDao
import com.example.spire.data.mapper.toCartEntity
import com.example.spire.data.mapper.toDomain
import com.example.spire.domain.model.AddToCartResult
import com.example.spire.domain.model.Cart
import com.example.spire.domain.model.Product
import com.example.spire.domain.repository.CartRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CartRepositoryImpl(private val dao: CartDao) : CartRepository {

    override fun observeCart(): Flow<Cart> =
        dao.observeAll().map { items -> Cart(items.map { it.toDomain() }) }

    override fun observeTotalItems(): Flow<Int> = dao.observeTotalItems()

    override fun observeQuantity(productId: Int): Flow<Int> =
        dao.observeQuantity(productId).map { it ?: 0 }

    override suspend fun add(product: Product): AddToCartResult {
        if (product.stock <= 0) return AddToCartResult.OUT_OF_STOCK
        return if (dao.addOrIncrement(product.toCartEntity())) AddToCartResult.ADDED
        else AddToCartResult.MAX_REACHED
    }

    override suspend fun increment(productId: Int): Boolean = dao.increment(productId) > 0

    override suspend fun decrement(productId: Int) = dao.decrementOrRemove(productId)

    override suspend fun remove(productId: Int) = dao.remove(productId)

    override suspend fun clear() = dao.clear()
}
