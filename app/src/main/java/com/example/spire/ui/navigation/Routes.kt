package com.example.spire.ui.navigation

import kotlinx.serialization.Serializable

@Serializable data object ProductsRoute
@Serializable data class ProductDetailRoute(val productId: Int)
@Serializable data object CartRoute
