package com.example.spire.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.spire.ui.cart.CartScreen
import com.example.spire.ui.detail.ProductDetailScreen
import com.example.spire.ui.products.ProductListScreen

@Composable
fun SpireNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = ProductsRoute, modifier = modifier) {
        composable<ProductsRoute> {
            ProductListScreen(
                onProductClick = { navController.navigate(ProductDetailRoute(it)) },
            )
        }
        composable<ProductDetailRoute> { entry ->
            val route = entry.toRoute<ProductDetailRoute>()
            ProductDetailScreen(
                productId = route.productId,
                onBack = { navController.popBackStack() },
                onOpenCart = { navController.navigateTopLevel(CartRoute) },
            )
        }
        composable<CartRoute> {
            CartScreen(
                onBrowseProducts = { navController.navigateTopLevel(ProductsRoute) },
                onProductClick = { navController.navigate(ProductDetailRoute(it)) },
            )
        }
    }
}

fun NavHostController.navigateTopLevel(route: Any) {
    navigate(route) {
        popUpTo(ProductsRoute) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
