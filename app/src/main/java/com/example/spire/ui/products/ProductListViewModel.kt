package com.example.spire.ui.products

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spire.core.AppError
import com.example.spire.core.ConnectivityObserver
import com.example.spire.core.toAppError
import com.example.spire.domain.model.AddToCartResult
import com.example.spire.domain.model.Category
import com.example.spire.domain.model.Product
import com.example.spire.domain.repository.CartRepository
import com.example.spire.domain.repository.ProductRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ListContent {
    data object Loading : ListContent
    data object Content : ListContent
    data object Empty : ListContent
    data class Error(val error: AppError) : ListContent
}

data class ProductListUiState(
    val query: String = "",
    val selectedCategory: String? = null,
    val categories: List<Category> = emptyList(),
    val products: List<Product> = emptyList(),
    val isRefreshing: Boolean = true,
    val isLoadingMore: Boolean = false,
    val endReached: Boolean = false,
    val error: AppError? = null,
    val isOnline: Boolean = true,

    val cartQuantities: Map<Int, Int> = emptyMap(),
) {
    val isSearching: Boolean get() = query.isNotBlank()

    val content: ListContent
        get() = when {
            products.isNotEmpty() -> ListContent.Content
            isRefreshing -> ListContent.Loading
            error != null -> ListContent.Error(error)
            else -> ListContent.Empty
        }
}

sealed interface ListEvent {
    data class Added(val title: String) : ListEvent
    data class MaxReached(val stock: Int) : ListEvent
    data object OutOfStock : ListEvent
}

private const val KEY_QUERY = "query"
private const val KEY_CATEGORY = "category"

private data class FeedParams(val query: String, val category: String?)

private data class Status(
    val load: LoadState,
    val isOnline: Boolean,
    val cartQuantities: Map<Int, Int>,
)

private data class LoadState(
    val isRefreshing: Boolean = true,
    val isLoadingMore: Boolean = false,
    val endReached: Boolean = false,
    val error: AppError? = null,
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class ProductListViewModel(
    private val handle: SavedStateHandle,
    private val repository: ProductRepository,
    private val cartRepository: CartRepository,
    connectivity: ConnectivityObserver,
) : ViewModel() {

    private companion object {
        const val TAG = "ProductListViewModel"
    }

    private val rawQuery: StateFlow<String> = handle.getStateFlow(KEY_QUERY, "")
    private val category: StateFlow<String?> = handle.getStateFlow<String?>(KEY_CATEGORY, null)
    private val loadState = MutableStateFlow(LoadState())
    private val isOnline = connectivity.observeOnline()
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private var loadMoreJob: Job? = null
    private val refreshRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val events = Channel<ListEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    private val feed: StateFlow<FeedParams> = combine(
        rawQuery.debounce { if (it.isBlank()) 0L else 400L }.map { it.trim() }.distinctUntilChanged(),
        category,
    ) { q, c -> FeedParams(q, if (q.isEmpty()) c else null) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, FeedParams(rawQuery.value.trim(), category.value))

    private val categories = repository.observeCategories()

    private val cartQuantities = cartRepository.observeCart()
        .map { cart -> cart.items.associate { it.productId to it.quantity } }

    private val status = combine(loadState, isOnline, cartQuantities, ::Status)

    val uiState: StateFlow<ProductListUiState> = combine(
        rawQuery,
        category,
        categories,
        feed.flatMapLatest { repository.observeFeed(it.query, it.category) },
        status,
    ) { query, selected, categories, products, status ->
        ProductListUiState(
            query = query,
            selectedCategory = selected,
            categories = categories,
            products = products,
            isRefreshing = status.load.isRefreshing,
            isLoadingMore = status.load.isLoadingMore,
            endReached = status.load.endReached,
            error = status.load.error,
            isOnline = status.isOnline,
            cartQuantities = status.cartQuantities,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductListUiState())

    init {
        viewModelScope.launch {
            merge(feed.map { }, refreshRequests, backOnlineRetries())
                .collectLatest { loadPage(skip = 0) }
        }
        refreshCategories()
    }

    fun onQueryChange(value: String) {
        Log.d(TAG, "onQueryChange query='$value'")
        handle[KEY_QUERY] = value
    }

    fun onCategorySelected(slug: String?) {
        Log.d(TAG, "onCategorySelected slug=$slug")
        if (rawQuery.value.isNotEmpty()) handle[KEY_QUERY] = ""
        handle[KEY_CATEGORY] = if (slug == category.value) null else slug
    }

    fun refresh() {
        Log.d(TAG, "refresh requested")
        refreshRequests.tryEmit(Unit)
    }

    fun loadMore() {
        val s = loadState.value
        if (s.isRefreshing || s.isLoadingMore || s.endReached) return
        val loaded = uiState.value.products.size
        if (loaded == 0) return
        Log.d(TAG, "loadMore skip=$loaded")
        loadMoreJob?.cancel()
        loadMoreJob = viewModelScope.launch { loadPage(skip = loaded) }
    }

    private fun backOnlineRetries(): Flow<Unit> = isOnline.drop(1)
        .filter { it }
        .onEach { if (categories.first().isEmpty()) refreshCategories() }
        .filter { loadState.value.error != null }
        .map { }

    private suspend fun loadPage(skip: Int) {
        val params = feed.value
        Log.d(TAG, "loadPage skip=$skip query='${params.query}' category=${params.category}")
        if (skip == 0) loadMoreJob?.cancel()
        loadState.update {
            if (skip == 0) it.copy(isRefreshing = true, isLoadingMore = false, error = null, endReached = false)
            else it.copy(isLoadingMore = true, error = null)
        }
        repository.refreshFeed(params.query, params.category, skip)
            .onSuccess { page ->
                Log.d(TAG, "loadPage ok skip=$skip endReached=${page.endReached}")
                loadState.value = LoadState(isRefreshing = false, endReached = page.endReached)
            }
            .onFailure { e ->
                Log.d(TAG, "loadPage failed skip=$skip: ${e.message}")
                loadState.update {
                    it.copy(isRefreshing = false, isLoadingMore = false, error = e.toAppError())
                }
            }
    }

    fun addToCart(product: Product) {
        viewModelScope.launch {
            val event = when (cartRepository.add(product)) {
                AddToCartResult.ADDED -> ListEvent.Added(product.title)
                AddToCartResult.MAX_REACHED -> ListEvent.MaxReached(product.stock)
                AddToCartResult.OUT_OF_STOCK -> ListEvent.OutOfStock
            }
            Log.d(TAG, "addToCart productId=${product.id} result=$event")
            events.send(event)
        }
    }

    private fun refreshCategories() {
        viewModelScope.launch { repository.refreshCategories() }
    }
}
