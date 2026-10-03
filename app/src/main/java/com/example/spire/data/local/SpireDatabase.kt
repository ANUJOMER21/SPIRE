package com.example.spire.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.spire.data.local.dao.CartDao
import com.example.spire.data.local.dao.ProductDao
import com.example.spire.data.local.entity.CartItemEntity
import com.example.spire.data.local.entity.CategoryEntity
import com.example.spire.data.local.entity.FeedItemEntity
import com.example.spire.data.local.entity.ProductEntity

@Database(
    entities = [ProductEntity::class, FeedItemEntity::class, CategoryEntity::class, CartItemEntity::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class SpireDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun cartDao(): CartDao
}
