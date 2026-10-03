package com.example.spire.ui.cart

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spire.domain.model.Cart
import com.example.spire.domain.repository.CartRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CartUiState(
    val cart: Cart = Cart(),
    val isLoading: Boolean = true,
)

class CartViewModel(private val repository: CartRepository) : ViewModel() {

    private companion object {
        const val TAG = "CartViewModel"
    }

    val uiState: StateFlow<CartUiState> = repository.observeCart()
        .map { CartUiState(cart = it, isLoading = false) }
        .onEach { Log.d(TAG, "cart updated items=${it.cart.items.size}") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CartUiState())

    fun increment(productId: Int) {
        Log.d(TAG, "increment productId=$productId")
        viewModelScope.launch { repository.increment(productId) }
    }
    fun decrement(productId: Int) {
        Log.d(TAG, "decrement productId=$productId")
        viewModelScope.launch { repository.decrement(productId) }
    }
    fun remove(productId: Int) {
        Log.d(TAG, "remove productId=$productId")
        viewModelScope.launch { repository.remove(productId) }
    }
    fun clear() {
        Log.d(TAG, "clear cart")
        viewModelScope.launch { repository.clear() }
    }
}
