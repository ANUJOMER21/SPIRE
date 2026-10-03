package com.example.spire.ui.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.spire.R
import com.example.spire.core.AppError
import com.example.spire.domain.model.Product
import com.example.spire.ui.components.ConfirmDialog
import com.example.spire.ui.components.ErrorState
import com.example.spire.ui.components.MessageState
import com.example.spire.ui.components.asPrice
import com.example.spire.ui.components.rememberShimmerBrush
import com.example.spire.ui.components.title
import com.example.spire.ui.components.userMessage
import com.example.spire.ui.theme.SPIRETheme
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.androidx.compose.koinViewModel

@Composable
fun ProductListScreen(
    onProductClick: (Int) -> Unit,
    viewModel: ProductListViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var pendingAddId by rememberSaveable { mutableStateOf<Int?>(null) }
    val resources = LocalResources.current

    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            val message = when (event) {
                is ListEvent.Added -> resources.getString(R.string.products_snackbar_added, event.title)
                is ListEvent.MaxReached -> resources.getString(R.string.products_snackbar_max_reached, event.stock)
                ListEvent.OutOfStock -> resources.getString(R.string.products_snackbar_out_of_stock)
            }

            withTimeoutOrNull(3_000) {
                snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Indefinite)
            }
        }
    }

    state.products.firstOrNull { it.id == pendingAddId }?.let { product ->
        ConfirmDialog(
            title = stringResource(R.string.products_add_dialog_title),
            message = stringResource(R.string.products_add_dialog_message, product.title, product.priceCents.asPrice()),
            confirmLabel = stringResource(R.string.products_add_dialog_confirm),
            onConfirm = { viewModel.addToCart(product); pendingAddId = null },
            onDismiss = { pendingAddId = null },
        )
    }

    Box(Modifier.fillMaxSize()) {
        ProductListContent(
            state = state,
            onQueryChange = viewModel::onQueryChange,
            onCategorySelected = viewModel::onCategorySelected,
            onRefresh = viewModel::refresh,
            onLoadMore = viewModel::loadMore,
            onProductClick = onProductClick,
            onQuickAdd = { pendingAddId = it.id },
        )
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductListContent(
    state: ProductListUiState,
    onQueryChange: (String) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onProductClick: (Int) -> Unit,
    onQuickAdd: (Product) -> Unit,
) {
    val gridState = rememberLazyGridState()
    LoadMoreEffect(gridState, itemCount = state.products.size, onLoadMore = onLoadMore)
    val full: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }
    val shimmer = rememberShimmerBrush()

    PullToRefreshBox(
        isRefreshing = state.isRefreshing && state.products.isNotEmpty(),
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header", span = full) { Header(state.query, onQueryChange) }
            if (state.categories.isNotEmpty()) {
                item(key = "chips", span = full) {
                    CategoryChips(state, onCategorySelected)
                }
            }

            when (val content = state.content) {
                ListContent.Loading -> items(6, key = { "skeleton$it" }) { SkeletonCard(shimmer) }
                is ListContent.Error -> item(key = "error", span = full) {
                    ErrorState(content.error, onRetry = onRefresh, modifier = Modifier.height(420.dp))
                }
                ListContent.Empty -> item(key = "empty", span = full) {
                    EmptyState(state.isSearching, state.query, onRefresh)
                }
                ListContent.Content -> {
                    if (state.error != null) {
                        item(key = "inlineError", span = full) { InlineError(state.error, onRefresh) }
                    }
                    items(state.products, key = { it.id }) { product ->
                        ProductCard(
                            product = product,
                            quantityInCart = state.cartQuantities[product.id] ?: 0,
                            onClick = { onProductClick(product.id) },
                            onQuickAdd = { onQuickAdd(product) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                    if (state.isLoadingMore) {
                        item(key = "loadingMore", span = full) {
                            Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(isSearching: Boolean, query: String, onRefresh: () -> Unit) {
    MessageState(
        title = stringResource(if (isSearching) R.string.products_no_results_title else R.string.products_empty_title),
        message = if (isSearching) {
            stringResource(R.string.products_no_results_message, query.trim())
        } else stringResource(R.string.products_empty_message),
        icon = Icons.Default.Search,
        actionLabel = if (isSearching) null else stringResource(R.string.products_refresh),
        onAction = if (isSearching) null else onRefresh,
        modifier = Modifier.height(420.dp),
    )
}

@Composable
private fun Header(query: String, onQueryChange: (String) -> Unit) {
    Column(Modifier.padding(top = 20.dp, bottom = 4.dp)) {
        Text(stringResource(R.string.products_discover_title), style = MaterialTheme.typography.headlineMedium)
        Text(
            stringResource(R.string.products_discover_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.products_search_placeholder)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.products_clear_search))
                    }
                }
            },
            singleLine = true,
            shape = CircleShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}

@Composable
private fun LoadMoreEffect(gridState: LazyGridState, itemCount: Int, onLoadMore: () -> Unit) {
    LaunchedEffect(gridState, itemCount) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { last -> if (itemCount > 0 && last >= itemCount - 4) onLoadMore() }
    }
}

@Composable
private fun InlineError(error: AppError, onRetry: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Row(
            Modifier.padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(20.dp),
            )
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(error.title(), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                Text(error.userMessage(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
            }
            TextButton(onClick = onRetry) { Text(stringResource(R.string.products_retry)) }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorStatePreview() {
    SPIRETheme {
        ErrorState(AppError.NoInternet(), onRetry = {}, modifier = Modifier.height(420.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyStatePreview() {
    SPIRETheme {
        EmptyState(isSearching = true, query = "zzzz", onRefresh = {})
    }
}
