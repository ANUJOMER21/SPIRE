package com.example.spire.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.spire.data.local.entity.CartItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class CartDao {

    @Query("SELECT * FROM cart_items ORDER BY addedAt DESC")
    abstract fun observeAll(): Flow<List<CartItemEntity>>

    @Query("SELECT quantity FROM cart_items WHERE productId = :productId")
    abstract fun observeQuantity(productId: Int): Flow<Int?>

    @Query("SELECT COALESCE(SUM(quantity), 0) FROM cart_items")
    abstract fun observeTotalItems(): Flow<Int>

    @Query("SELECT quantity FROM cart_items WHERE productId = :productId")
    protected abstract suspend fun getQuantity(productId: Int): Int?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertIfAbsent(item: CartItemEntity): Long

    @Query("UPDATE cart_items SET quantity = quantity + 1 WHERE productId = :productId AND quantity < stock")
    abstract suspend fun increment(productId: Int): Int

    @Query("UPDATE cart_items SET quantity = quantity - 1 WHERE productId = :productId AND quantity > 1")
    protected abstract suspend fun decrementRow(productId: Int): Int

    @Query("DELETE FROM cart_items WHERE productId = :productId")
    abstract suspend fun remove(productId: Int)

    @Query("DELETE FROM cart_items")
    abstract suspend fun clear()

    @Transaction
    open suspend fun addOrIncrement(item: CartItemEntity): Boolean =
        if (insertIfAbsent(item) != -1L) true else increment(item.productId) > 0

    @Transaction
    open suspend fun decrementOrRemove(productId: Int) {
        val quantity = getQuantity(productId) ?: return
        if (quantity <= 1) remove(productId) else decrementRow(productId)
    }
}
