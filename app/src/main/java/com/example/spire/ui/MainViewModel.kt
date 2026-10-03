package com.example.spire.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spire.core.ConnectivityObserver
import com.example.spire.domain.repository.CartRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

class MainViewModel(
    cartRepository: CartRepository,
    connectivity: ConnectivityObserver,
) : ViewModel() {

    private companion object {
        const val TAG = "MainViewModel"
    }

    val cartCount: StateFlow<Int> = cartRepository.observeTotalItems()
        .onEach { Log.d(TAG, "cartCount=$it") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val isOnline: StateFlow<Boolean> = connectivity.observeOnline()
        .onEach { Log.d(TAG, "isOnline=$it") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
}
