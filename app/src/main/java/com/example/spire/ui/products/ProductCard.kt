package com.example.spire.ui.products

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.spire.R
import com.example.spire.domain.model.Product
import com.example.spire.ui.components.asPrice
import com.example.spire.ui.components.asRating
import com.example.spire.ui.components.title
import com.example.spire.ui.theme.RatingStar
import com.example.spire.ui.theme.SPIRETheme

@Composable
internal fun ProductCard(
    product: Product,
    quantityInCart: Int,
    onClick: () -> Unit,
    onQuickAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(6.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.15f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            ),
                        ),
                    ),
            ) {
                AsyncImage(
                    model = product.thumbnail,
                    contentDescription = product.title,
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                    contentScale = ContentScale.Fit,
                )
                RatingPill(
                    product.rating,
                    Modifier.align(Alignment.TopStart).padding(8.dp),
                )
            }
            Row(
                Modifier.padding(start = 6.dp, top = 8.dp, end = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        product.category.replace('-', ' ').uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        product.title,
                        style = MaterialTheme.typography.titleSmall,
                        minLines = 2,
                        maxLines = 2,
                        lineHeight = 18.sp,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        product.priceCents.asPrice(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                QuickAddButton(quantityInCart, enabled = product.stock > 0, onClick = onQuickAdd)
            }
        }
    }
}

@Composable
private fun QuickAddButton(quantityInCart: Int, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = if (quantityInCart > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary,
        contentColor = if (quantityInCart > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.size(34.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (quantityInCart > 0) {
                Text(quantityInCart.toString(), style = MaterialTheme.typography.labelMedium)
            } else {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.products_add_to_cart_cd), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun RatingPill(rating: Double, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Star, contentDescription = stringResource(R.string.products_rating_cd), tint = RatingStar, modifier = Modifier.size(14.dp))
            Spacer(Modifier.size(3.dp))
            Text(rating.asRating(), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
internal fun SkeletonCard(brush: Brush) {
    Column(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(10.dp)).background(brush))
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth(0.4f).height(10.dp).clip(CircleShape).background(brush))
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(14.dp).clip(CircleShape).background(brush))
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth(0.6f).height(14.dp).clip(CircleShape).background(brush))
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth(0.35f).height(16.dp).clip(CircleShape).background(brush))
        Spacer(Modifier.height(6.dp))
    }
}

private val previewProduct = Product(
    id = 1,
    title = "Essence Mascara Lash Princess",
    description = "Volumizing and lengthening mascara.",
    category = "beauty",
    priceCents = 999,
    rating = 4.5,
    stock = 99,
    brand = "Essence",
    thumbnail = "",
    images = emptyList(),
)

@Preview(showBackground = true, widthDp = 190)
@Composable
private fun ProductCardPreview() {
    SPIRETheme {
        ProductCard(previewProduct, quantityInCart = 0, onClick = {}, onQuickAdd = {})
    }
}

@Preview(showBackground = true, widthDp = 190)
@Composable
private fun ProductCardInCartPreview() {
    SPIRETheme {
        ProductCard(previewProduct, quantityInCart = 2, onClick = {}, onQuickAdd = {})
    }
}
