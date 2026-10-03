package com.example.spire.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.spire.R
import com.example.spire.domain.model.Product
import com.example.spire.ui.components.ConfirmDialog
import com.example.spire.ui.components.ErrorState
import com.example.spire.ui.components.FullScreenLoading
import com.example.spire.ui.components.QuantityStepper
import com.example.spire.ui.components.asPrice
import com.example.spire.ui.products.RatingPill
import com.example.spire.ui.theme.SPIRETheme
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ProductDetailScreen(
    productId: Int,
    onBack: () -> Unit,
    onOpenCart: () -> Unit,
    viewModel: ProductDetailViewModel = koinViewModel(parameters = { parametersOf(productId) }),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmAdd by rememberSaveable { mutableStateOf(false) }
    var confirmRemove by rememberSaveable { mutableStateOf(false) }

    val resources = LocalResources.current
    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            val isAdded = event is DetailEvent.Added
            val text = when (event) {
                DetailEvent.Added -> resources.getString(R.string.detail_added_to_cart)
                is DetailEvent.MaxReached -> resources.getString(R.string.detail_only_in_stock, event.stock)
                DetailEvent.OutOfStock -> resources.getString(R.string.detail_out_of_stock)
            }

            val result = withTimeoutOrNull(3_000) {
                snackbarHostState.showSnackbar(
                    message = text,
                    actionLabel = resources.getString(R.string.detail_view_cart).takeIf { isAdded },
                    duration = SnackbarDuration.Indefinite,
                )
            }
            if (result == SnackbarResult.ActionPerformed) onOpenCart()
        }
    }

    val product = state.product
    val error = state.error
    if (confirmAdd && product != null) {
        ConfirmDialog(
            title = stringResource(R.string.detail_add_title),
            message = stringResource(R.string.detail_add_message, product.title, product.priceCents.asPrice()),
            confirmLabel = stringResource(R.string.detail_add),
            onConfirm = { viewModel.addToCart(); confirmAdd = false },
            onDismiss = { confirmAdd = false },
        )
    }
    if (confirmRemove) {
        ConfirmDialog(
            title = stringResource(R.string.detail_remove_title),
            message = stringResource(R.string.detail_remove_message),
            confirmLabel = stringResource(R.string.detail_remove),
            destructive = true,
            onConfirm = { viewModel.decrement(); confirmRemove = false },
            onDismiss = { confirmRemove = false },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },

        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                product != null -> DetailContent(
                    product = product,
                    cartQuantity = state.cartQuantity,
                    onBack = onBack,
                    onAdd = { confirmAdd = true },
                    onIncrement = viewModel::increment,

                    onDecrement = { if (state.cartQuantity <= 1) confirmRemove = true else viewModel.decrement() },
                )
                state.isLoading -> {
                    FullScreenLoading()
                    BackButton(onBack, Modifier.align(Alignment.TopStart))
                }
                error != null -> {
                    ErrorState(error, onRetry = viewModel::refresh)
                    BackButton(onBack, Modifier.align(Alignment.TopStart))
                }
            }
        }
    }
}

@Composable
private fun BackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(12.dp).size(40.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shadowElevation = 2.dp,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.detail_back))
        }
    }
}

@Composable
private fun DetailContent(
    product: Product,
    cartQuantity: Int,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Gallery(product, onBack)
            Column(Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                Text(
                    product.category.replace('-', ' ').uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(6.dp))
                Text(product.title, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RatingPill(product.rating, Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape))
                    StockBadge(product.stock)
                }

                Spacer(Modifier.height(24.dp))
                Text(stringResource(R.string.detail_about), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    product.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(24.dp))
                Text(stringResource(R.string.detail_details), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        InfoRow(stringResource(R.string.detail_brand), product.brand ?: stringResource(R.string.detail_no_brand))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        InfoRow(stringResource(R.string.detail_category), product.category.replace('-', ' ').replaceFirstChar { it.uppercase() })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        InfoRow(
                            stringResource(R.string.detail_stock),
                            if (product.stock > 0) pluralStringResource(R.plurals.detail_stock_units, product.stock, product.stock)
                            else stringResource(R.string.detail_out_of_stock),
                        )
                    }
                }
            }
        }
        BottomBar(product, cartQuantity, onAdd, onIncrement, onDecrement)
    }
}

@Composable
private fun Gallery(product: Product, onBack: () -> Unit) {
    val images = product.images.ifEmpty { listOf(product.thumbnail) }
    val pagerState = rememberPagerState { images.size }
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1.1f)
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        HorizontalPager(pagerState, Modifier.fillMaxSize()) { page ->
            AsyncImage(
                model = images[page],
                contentDescription = product.title,
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentScale = ContentScale.Fit,
            )
        }
        BackButton(onBack, Modifier.align(Alignment.TopStart))
        if (images.size > 1) {
            Row(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(images.size) { i ->
                    val selected = i == pagerState.currentPage
                    Box(
                        Modifier
                            .size(width = if (selected) 18.dp else 6.dp, height = 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun StockBadge(stock: Int) {
    val (label, container, content) = when {
        stock <= 0 -> Triple(stringResource(R.string.detail_out_of_stock), MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        stock <= 5 -> Triple(pluralStringResource(R.plurals.detail_stock_low, stock, stock), MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
        else -> Triple(stringResource(R.string.detail_in_stock), MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
    }
    Surface(shape = CircleShape, color = container) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = content,
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun BottomBar(
    product: Product,
    cartQuantity: Int,
    onAdd: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column {
                Text(stringResource(R.string.detail_price), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(product.priceCents.asPrice(), style = MaterialTheme.typography.headlineSmall)
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                if (cartQuantity == 0) {
                    Button(
                        onClick = onAdd,
                        enabled = product.stock > 0,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = CircleShape,
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(if (product.stock > 0) R.string.detail_add_to_cart else R.string.detail_out_of_stock))
                    }
                } else {
                    QuantityStepper(
                        quantity = cartQuantity,
                        canIncrease = cartQuantity < product.stock,
                        onDecrement = onDecrement,
                        onIncrement = onIncrement,
                    )
                }
            }
        }
    }
}

private fun previewProduct(stock: Int) = Product(
    id = 1,
    title = "Essence Mascara Lash Princess",
    description = "A popular mascara known for its volumizing and lengthening effects.",
    category = "beauty",
    priceCents = 999,
    rating = 4.4,
    stock = stock,
    brand = "Essence",
    thumbnail = "",
    images = emptyList(),
)

@Preview(showBackground = true)
@Composable
private fun DetailContentInStockPreview() {
    SPIRETheme {
        DetailContent(previewProduct(stock = 99), cartQuantity = 0, onBack = {}, onAdd = {}, onIncrement = {}, onDecrement = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun DetailContentLowStockInCartPreview() {
    SPIRETheme {
        DetailContent(previewProduct(stock = 3), cartQuantity = 2, onBack = {}, onAdd = {}, onIncrement = {}, onDecrement = {})
    }
}
