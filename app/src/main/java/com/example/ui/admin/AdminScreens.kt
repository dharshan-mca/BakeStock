package com.example.ui.admin

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.repository.BakeStockRepository
import com.example.ui.shared.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminDashboardScreen(
    repository: BakeStockRepository,
    onNavigateToProducts: () -> Unit,
    onNavigateToPurchases: () -> Unit,
    onNavigateToSuppliers: () -> Unit,
    onNavigateToStaff: () -> Unit,
    onNavigateToRecipes: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToWastage: () -> Unit,
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToForecast: () -> Unit = {},
    onNavigateToFirebaseStatus: () -> Unit = {},
    onNavigateToProductDetails: (String) -> Unit
) {
    val products by repository.allProducts.collectAsState(initial = emptyList())
    val lowStockProducts by repository.lowStockProducts.collectAsState(initial = emptyList())
    val purchases by repository.allPurchases.collectAsState(initial = emptyList())
    val wastageRecords by repository.allWastageRecords.collectAsState(initial = emptyList())
    val requests by repository.allIngredientRequests.collectAsState(initial = emptyList())

    val totalGroceryCount = products.count { it.productType == ProductType.GROCERY }
    val totalRawMaterials = products.count { it.productType == ProductType.BAKING_RAW_MATERIAL || it.productType == ProductType.PERISHABLE }
    val totalFinishedProducts = products.count { it.productType == ProductType.FINISHED_BAKERY_PRODUCT }
    val totalStockValue = products.sumOf { it.currentQuantity * it.purchasePrice }

    val now = System.currentTimeMillis()
    val nearExpiryProducts = products.filter { it.expiryDate <= now + (7L * 24 * 60 * 60 * 1000) && it.expiryDate > now }
    val expiredProducts = products.filter { it.expiryDate <= now }

    val todayPurchasesTotal = purchases.filter { isToday(it.purchaseDate) }.sumOf { it.totalAmount }
    val weeklyPurchasesTotal = purchases.sumOf { it.totalAmount }
    val totalWastageValue = wastageRecords.sumOf { it.totalWastageValue }

    val samplePurchaseData = listOf(
        "Mon" to 420.0,
        "Tue" to 850.0,
        "Wed" to 310.0,
        "Thu" to 1120.0,
        "Fri" to 640.0,
        "Sat" to 950.0,
        "Sun" to 580.0
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Admin Overview Dashboard",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Real-time stock summary, purchases & wastage tracking",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Summary Metric Cards Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryCard(
                        title = "Stock Value",
                        value = "$${String.format("%.2f", totalStockValue)}",
                        icon = Icons.Default.AttachMoney,
                        containerColor = AmberPrimary.copy(alpha = 0.15f),
                        contentColor = AmberPrimaryDark,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = "Today's Purchases",
                        value = "$${String.format("%.2f", todayPurchasesTotal)}",
                        icon = Icons.Default.ShoppingCart,
                        containerColor = ForestGreenTertiary.copy(alpha = 0.15f),
                        contentColor = ForestGreenTertiary,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToPurchases
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryCard(
                        title = "Grocery Items",
                        value = "$totalGroceryCount items",
                        icon = Icons.Default.Inventory2,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToProducts
                    )
                    SummaryCard(
                        title = "Raw Materials",
                        value = "$totalRawMaterials items",
                        icon = Icons.Default.Grain,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToProducts
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryCard(
                        title = "Low Stock Alert",
                        value = "${lowStockProducts.size} items",
                        icon = Icons.Default.Warning,
                        containerColor = StatusAmber.copy(alpha = 0.15f),
                        contentColor = StatusAmber,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = "Near / Expired",
                        value = "${nearExpiryProducts.size + expiredProducts.size} items",
                        icon = Icons.Default.EventBusy,
                        containerColor = StatusRed.copy(alpha = 0.15f),
                        contentColor = StatusRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Quick Navigation Hub
        item {
            SectionHeader(title = "Quick Navigation Hub")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item { NavPill(label = "Firebase Status", icon = Icons.Default.CloudSync, onClick = onNavigateToFirebaseStatus) }
                item { NavPill(label = "Analytics", icon = Icons.Default.Analytics, onClick = onNavigateToAnalytics) }
                item { NavPill(label = "Forecast & Reorder", icon = Icons.Default.TrendingUp, onClick = onNavigateToForecast) }
                item { NavPill(label = "Products", icon = Icons.Default.Category, onClick = onNavigateToProducts) }
                item { NavPill(label = "Purchases", icon = Icons.Default.Receipt, onClick = onNavigateToPurchases) }
                item { NavPill(label = "Suppliers", icon = Icons.Default.LocalShipping, onClick = onNavigateToSuppliers) }
                item { NavPill(label = "Staff Accounts", icon = Icons.Default.People, onClick = onNavigateToStaff) }
                item { NavPill(label = "Recipes", icon = Icons.Default.MenuBook, onClick = onNavigateToRecipes) }
                item { NavPill(label = "Wastage", icon = Icons.Default.Delete, onClick = onNavigateToWastage) }
                item { NavPill(label = "Reports", icon = Icons.Default.Assessment, onClick = onNavigateToReports) }
            }
        }

        // Purchase Trends Chart
        item {
            SectionHeader(title = "Weekly Purchase Overview")
            PurchaseBarChart(dataPoints = samplePurchaseData)
        }

        // Low Stock Items Section
        item {
            SectionHeader(title = "Low Stock Items", actionText = "View All", onActionClick = onNavigateToProducts)
            if (lowStockProducts.isEmpty()) {
                Text(
                    text = "All products have sufficient stock level.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    lowStockProducts.take(4).forEach { prod ->
                        ProductItemCard(product = prod, onClick = { onNavigateToProductDetails(prod.id) })
                    }
                }
            }
        }

        // Pending Kitchen Requests
        item {
            SectionHeader(title = "Pending Kitchen Requests")
            val pending = requests.filter { it.status == RequestStatus.PENDING }
            if (pending.isEmpty()) {
                Text(
                    text = "No pending kitchen ingredient requests.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    pending.take(3).forEach { req ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = req.requestNumber, fontWeight = FontWeight.Bold)
                                    Text(text = "Requested by ${req.requestedByName}", style = MaterialTheme.typography.bodySmall)
                                }
                                PriorityBadge(priority = req.priority)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NavPill(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.wrapContentWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun ProductItemCard(product: Product, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (product.productType) {
                            ProductType.FINISHED_BAKERY_PRODUCT -> Icons.Default.BakeryDining
                            ProductType.PERISHABLE -> Icons.Default.AcUnit
                            else -> Icons.Default.Inventory2
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "${product.category} • $${product.purchasePrice}/${product.unit.symbol}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${product.currentQuantity} ${product.unit.symbol}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                StockStatusBadge(status = product.stockStatus)
            }
        }
    }
}

private fun isToday(timestamp: Long): Boolean {
    val cal1 = Calendar.getInstance()
    val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
