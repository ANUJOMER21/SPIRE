package com.example.spire.ui.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.spire.domain.model.Cart
import com.example.spire.R
import com.example.spire.domain.model.CartItem
import com.example.spire.ui.components.ConfirmDialog
import com.example.spire.ui.components.MessageState
import com.example.spire.ui.components.QuantityStepper
import com.example.spire.ui.components.asPrice
import com.example.spire.ui.theme.SPIRETheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun CartScreen(
    onBrowseProducts: () -> Unit,
    onProductClick: (Int) -> Unit,
    viewModel: CartViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when {

        state.isLoading -> Unit
        state.cart.isEmpty -> MessageState(
            title = stringResource(R.string.cart_empty_title),
            message = stringResource(R.string.cart_empty_message),
            icon = Icons.Default.ShoppingCart,
            actionLabel = stringResource(R.string.cart_empty_action),
            onAction = onBrowseProducts,
        )
        else -> CartContent(
            cart = state.cart,
            onIncrement = viewModel::increment,
            onDecrement = viewModel::decrement,
            onRemove = viewModel::remove,
            onClear = viewModel::clear,
            onProductClick = onProductClick,
        )
    }
}

@Composable
private fun CartContent(
    cart: Cart,
    onIncrement: (Int) -> Unit,
    onDecrement: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onClear: () -> Unit,
    onProductClick: (Int) -> Unit,
) {
    var pendingRemoveId by rememberSaveable { mutableStateOf<Int?>(null) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }

    cart.items.firstOrNull { it.productId == pendingRemoveId }?.let { item ->
        ConfirmDialog(
            title = stringResource(R.string.cart_remove_title),
            message = stringResource(R.string.cart_remove_message, item.title),
            confirmLabel = stringResource(R.string.cart_remove_confirm),
            destructive = true,
            onConfirm = { onRemove(item.productId); pendingRemoveId = null },
            onDismiss = { pendingRemoveId = null },
        )
    }
    if (confirmClear) {
        ConfirmDialog(
            title = stringResource(R.string.cart_clear_title),
            message = pluralStringResource(R.plurals.cart_clear_message, cart.totalItems, cart.totalItems),
            confirmLabel = stringResource(R.string.cart_clear_all),
            destructive = true,
            onConfirm = { onClear(); confirmClear = false },
            onDismiss = { confirmClear = false },
        )
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.cart_title), style = MaterialTheme.typography.headlineMedium)
                Text(
                    pluralStringResource(R.plurals.cart_item_count, cart.totalItems, cart.totalItems),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = { confirmClear = true }) { Text(stringResource(R.string.cart_clear_all)) }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(cart.items, key = { it.productId }) { item ->
                CartRow(
                    item = item,
                    onIncrement = onIncrement,

                    onDecrement = { id -> if (item.quantity <= 1) pendingRemoveId = id else onDecrement(id) },
                    onRemove = { pendingRemoveId = item.productId },
                    onProductClick = onProductClick,
                )
            }
        }
        SummaryCard(cart)
    }
}

@Composable
private fun CartRow(
    item: CartItem,
    onIncrement: (Int) -> Unit,
    onDecrement: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onProductClick: (Int) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onProductClick(item.productId) },
            ) {
                AsyncImage(
                    model = item.thumbnail,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize().padding(6.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        item.title,
                        modifier = Modifier.weight(1f).padding(top = 2.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    IconButton(onClick = { onRemove(item.productId) }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.cart_remove_item_description, item.title),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Text(
                    stringResource(R.string.cart_price_each, item.priceCents.asPrice()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    QuantityStepper(
                        quantity = item.quantity,
                        canIncrease = item.canIncrease,
                        onDecrement = { onDecrement(item.productId) },
                        onIncrement = { onIncrement(item.productId) },
                    )
                    Text(item.lineTotalCents.asPrice(), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(cart: Cart) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp,
    ) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
            SummaryRow(stringResource(R.string.cart_summary_items), cart.totalItems.toString())
            Spacer(Modifier.height(10.dp))
            SummaryRow(stringResource(R.string.cart_summary_subtotal), cart.totalCents.asPrice())
            HorizontalDivider(Modifier.padding(vertical = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.cart_summary_total), style = MaterialTheme.typography.titleLarge)
                Text(
                    cart.totalCents.asPrice(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}

private val previewCart = Cart(
    listOf(
        CartItem(1, "Essence Mascara Lash Princess", "", 999, 2, 99),
        CartItem(2, "Red Lipstick", "", 1299, 1, 1),
    ),
)

@Preview(showBackground = true, name = "Cart")
@Composable
private fun CartContentPreview() {
    SPIRETheme(darkTheme = false) {
        CartContent(previewCart, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, name = "Cart dark")
@Composable
private fun CartContentDarkPreview() {
    SPIRETheme(darkTheme = true) {
        CartContent(previewCart, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, name = "Cart empty")
@Composable
private fun CartEmptyPreview() {
    SPIRETheme(darkTheme = false) {
        MessageState(
            title = stringResource(R.string.cart_empty_title),
            message = stringResource(R.string.cart_empty_message),
            icon = Icons.Default.ShoppingCart,
            actionLabel = stringResource(R.string.cart_empty_action),
            onAction = {},
        )
    }
}
