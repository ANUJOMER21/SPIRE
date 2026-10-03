package com.example.spire.ui.detail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spire.core.AppError
import com.example.spire.core.toAppError
import com.example.spire.domain.model.AddToCartResult
import com.example.spire.domain.model.Product
import com.example.spire.domain.repository.CartRepository
import com.example.spire.domain.repository.ProductRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductDetailUiState(
    val product: Product? = null,
    val cartQuantity: Int = 0,
    val isLoading: Boolean = true,
    val error: AppError? = null,
)

sealed interface DetailEvent {
    data object Added : DetailEvent
    data class MaxReached(val stock: Int) : DetailEvent
    data object OutOfStock : DetailEvent
}

class ProductDetailViewModel(
    private val productId: Int,
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository,
) : ViewModel() {

    private companion object {
        const val TAG = "ProductDetailViewModel"
    }

    private data class Load(val isLoading: Boolean = true, val error: AppError? = null)

    private val load = MutableStateFlow(Load())
    private val events = Channel<DetailEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    val uiState: StateFlow<ProductDetailUiState> = combine(
        productRepository.observeProduct(productId),
        cartRepository.observeQuantity(productId),
        load,
    ) { product, qty, l ->
        ProductDetailUiState(product, qty, isLoading = l.isLoading, error = l.error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProductDetailUiState())

    init {
        refresh()
    }

    fun refresh() {
        Log.d(TAG, "refresh productId=$productId")
        load.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            productRepository.refreshProduct(productId)
                .onSuccess {
                    Log.d(TAG, "refresh ok productId=$productId")
                    load.value = Load(isLoading = false)
                }
                .onFailure {
                    Log.d(TAG, "refresh failed productId=$productId: ${it.message}")
                    load.value = Load(isLoading = false, error = it.toAppError())
                }
        }
    }

    fun addToCart() {
        val product = uiState.value.product ?: return
        viewModelScope.launch {
            val event = when (cartRepository.add(product)) {
                AddToCartResult.ADDED -> DetailEvent.Added
                AddToCartResult.MAX_REACHED -> DetailEvent.MaxReached(product.stock)
                AddToCartResult.OUT_OF_STOCK -> DetailEvent.OutOfStock
            }
            Log.d(TAG, "addToCart productId=$productId result=$event")
            events.send(event)
        }
    }

    fun increment() {
        viewModelScope.launch {
            if (!cartRepository.increment(productId)) {
                Log.d(TAG, "increment blocked productId=$productId")
                events.send(DetailEvent.MaxReached(uiState.value.product?.stock ?: 0))
            }
        }
    }

    fun decrement() {
        Log.d(TAG, "decrement productId=$productId")
        viewModelScope.launch { cartRepository.decrement(productId) }
    }
}
