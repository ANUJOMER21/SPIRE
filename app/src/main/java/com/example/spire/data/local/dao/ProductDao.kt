package com.example.spire.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.spire.data.local.entity.CategoryEntity
import com.example.spire.data.local.entity.FeedItemEntity
import com.example.spire.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ProductDao {

    @Query(
        """
        SELECT p.* FROM products p
        INNER JOIN feed_items f ON f.productId = p.id
        WHERE f.feedKey = :feedKey
        ORDER BY f.position
        """
    )
    abstract fun observeFeed(feedKey: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    abstract fun observeProduct(id: Int): Flow<ProductEntity?>

    @Query("SELECT COUNT(*) FROM feed_items WHERE feedKey = :feedKey")
    abstract suspend fun feedSize(feedKey: String): Int

    @Query(
        """
        SELECT id FROM products
        WHERE title LIKE '%' || :q || '%' ESCAPE '\'
           OR brand LIKE '%' || :q || '%' ESCAPE '\'
           OR category LIKE '%' || :q || '%' ESCAPE '\'
           OR description LIKE '%' || :q || '%' ESCAPE '\'
        ORDER BY id
        """
    )
    abstract suspend fun searchLocalIds(q: String): List<Int>

    @Upsert
    abstract suspend fun upsertProducts(products: List<ProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertFeedItems(items: List<FeedItemEntity>)

    @Query("DELETE FROM feed_items WHERE feedKey = :feedKey")
    abstract suspend fun clearFeed(feedKey: String)

    @Transaction
    open suspend fun saveFeedPage(feedKey: String, skip: Int, products: List<ProductEntity>) {
        upsertProducts(products)
        if (skip == 0) clearFeed(feedKey)
        insertFeedItems(products.mapIndexed { i, p -> FeedItemEntity(feedKey, skip + i, p.id) })
    }

    @Transaction
    open suspend fun saveFeedIds(feedKey: String, ids: List<Int>) {
        clearFeed(feedKey)
        insertFeedItems(ids.mapIndexed { i, id -> FeedItemEntity(feedKey, i, id) })
    }

    @Query("SELECT * FROM categories ORDER BY position")
    abstract fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("DELETE FROM categories")
    abstract suspend fun clearCategories()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertCategories(categories: List<CategoryEntity>)

    @Transaction
    open suspend fun replaceCategories(categories: List<CategoryEntity>) {
        clearCategories()
        insertCategories(categories)
    }
}
