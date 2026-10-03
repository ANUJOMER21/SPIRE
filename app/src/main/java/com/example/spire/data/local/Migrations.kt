package com.example.spire.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE products_new (
                id INTEGER NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL,
                category TEXT NOT NULL, priceCents INTEGER NOT NULL, rating REAL NOT NULL,
                stock INTEGER NOT NULL, brand TEXT, thumbnail TEXT NOT NULL,
                images TEXT NOT NULL, cachedAt INTEGER NOT NULL, PRIMARY KEY(id)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO products_new
            SELECT id, title, description, category, CAST(ROUND(price * 100) AS INTEGER),
                   rating, stock, brand, thumbnail, images, cachedAt
            FROM products
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE products")
        db.execSQL("ALTER TABLE products_new RENAME TO products")

        db.execSQL(
            """
            CREATE TABLE cart_items_new (
                productId INTEGER NOT NULL, title TEXT NOT NULL, thumbnail TEXT NOT NULL,
                priceCents INTEGER NOT NULL, quantity INTEGER NOT NULL, stock INTEGER NOT NULL,
                addedAt INTEGER NOT NULL, PRIMARY KEY(productId)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO cart_items_new
            SELECT productId, title, thumbnail, CAST(ROUND(price * 100) AS INTEGER),
                   quantity, stock, addedAt
            FROM cart_items
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE cart_items")
        db.execSQL("ALTER TABLE cart_items_new RENAME TO cart_items")
    }
}
