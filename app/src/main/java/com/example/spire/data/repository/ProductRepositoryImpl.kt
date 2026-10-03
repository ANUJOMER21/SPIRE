package com.example.spire.data.repository

import com.example.spire.data.remote.apiCall
import com.example.spire.data.local.dao.ProductDao
import com.example.spire.data.mapper.toDomain
import com.example.spire.data.mapper.toEntity
import com.example.spire.data.remote.DummyJsonApi
import com.example.spire.domain.model.Category
import com.example.spire.domain.model.FeedPage
import com.example.spire.domain.model.Product
import com.example.spire.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProductRepositoryImpl(
    private val api: DummyJsonApi,
    private val dao: ProductDao,
) : ProductRepository {

    override fun observeFeed(query: String, category: String?): Flow<List<Product>> =
        dao.observeFeed(feedKey(query, category)).map { list -> list.map { it.toDomain() } }

    override suspend fun refreshFeed(query: String, category: String?, skip: Int): Result<FeedPage> {
        val q = query.trim()
        val key = feedKey(q, category)
        val limit = DummyJsonApi.PAGE_SIZE
        return apiCall {
            val response = when {
                q.isNotEmpty() -> api.searchProducts(q, limit, skip)
                category != null -> api.getProductsByCategory(category, limit, skip)
                else -> api.getProducts(limit, skip)
            }
            dao.saveFeedPage(key, skip, response.products.map { it.toEntity() })
            FeedPage(endReached = response.products.isEmpty() || skip + response.products.size >= response.total)
        }.onFailure {

            if (q.isNotEmpty() && skip == 0 && dao.feedSize(key) == 0) {
                dao.saveFeedIds(key, dao.searchLocalIds(q.escapeLike()))
            }
        }
    }

    override fun observeCategories(): Flow<List<Category>> =
        dao.observeCategories().map { list -> list.map { it.toDomain() } }

    override suspend fun refreshCategories(): Result<Unit> = apiCall {
        val categories = api.getCategories().mapIndexed { i, dto -> dto.toEntity(i) }
        dao.replaceCategories(categories)
    }

    override fun observeProduct(id: Int): Flow<Product?> =
        dao.observeProduct(id).map { it?.toDomain() }

    override suspend fun refreshProduct(id: Int): Result<Unit> = apiCall {
        dao.upsertProducts(listOf(api.getProduct(id).toEntity()))
    }

    private fun feedKey(query: String, category: String?): String {
        val q = query.trim().lowercase()
        return when {
            q.isNotEmpty() -> "q:$q"
            category != null -> "cat:$category"
            else -> "all"
        }
    }

    private fun String.escapeLike() =
        replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
}
