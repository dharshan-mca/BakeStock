package com.example.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.*
import com.example.data.repository.BakeStockRepository
import com.example.ui.shared.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryForecastScreen(
    repository: BakeStockRepository,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var recommendations by remember { mutableStateOf<List<SmartReorderRecommendation>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        recommendations = repository.getSmartReorderRecommendations()
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventory Forecast & Smart Reorder", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Predictive Stock Intelligence",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Automated runout estimation based on historical consumption & production usage.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (successMessage != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ForestGreenTertiary.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = successMessage!!,
                                modifier = Modifier.padding(12.dp),
                                color = ForestGreenTertiary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SummaryCard(
                            title = "Items to Reorder",
                            value = "${recommendations.size}",
                            icon = Icons.Default.Autorenew,
                            containerColor = StatusAmber.copy(alpha = 0.15f),
                            contentColor = StatusAmber,
                            modifier = Modifier.weight(1f)
                        )
                        val totalEstCost = recommendations.sumOf { it.suggestedReorderQuantity * it.product.purchasePrice }
                        SummaryCard(
                            title = "Est. Reorder Cost",
                            value = "$${Math.round(totalEstCost * 100.0) / 100.0}",
                            icon = Icons.Default.AttachMoney,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (recommendations.isEmpty()) {
                    item {
                        EmptyStateView(
                            title = "Stock Healthy",
                            message = "No items require reordering. All products exceed reorder thresholds."
                        )
                    }
                } else {
                    items(recommendations, key = { it.product.id }) { rec ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = rec.product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "Supplier: ${rec.supplierName} • ${rec.product.category}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    StockStatusBadge(status = rec.product.stockStatus)
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Current Stock", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${rec.currentStock} ${rec.product.unit.symbol}", fontWeight = FontWeight.Bold)
                                    }
                                    Column {
                                        Text("Daily Usage Rate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${rec.estimatedDailyUsage} ${rec.product.unit.symbol}/day", fontWeight = FontWeight.Bold)
                                    }
                                    Column {
                                        Text("Est. Days Left", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = if (rec.estimatedDaysRemaining > 90) "90+ days" else "${rec.estimatedDaysRemaining} days",
                                            fontWeight = FontWeight.Bold,
                                            color = if (rec.estimatedDaysRemaining <= 3) StatusRed else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            val purchase = Purchase(
                                                id = UUID.randomUUID().toString(),
                                                invoiceNumber = "PO-SMART-${System.currentTimeMillis() / 100000}",
                                                supplierId = "sup_default",
                                                supplierName = rec.supplierName,
                                                purchaseDate = System.currentTimeMillis(),
                                                totalAmount = rec.suggestedReorderQuantity * rec.product.purchasePrice,
                                                paymentStatus = "Pending",
                                                itemsSummary = "${rec.suggestedReorderQuantity} ${rec.product.unit.symbol} x ${rec.product.name}",
                                                createdBy = "Smart Forecast System"
                                            )
                                            repository.addPurchase(
                                                purchase = purchase,
                                                items = listOf(rec.product to rec.suggestedReorderQuantity)
                                            )
                                            successMessage = "Created Purchase Order #${purchase.invoiceNumber} for ${rec.suggestedReorderQuantity} ${rec.product.unit.symbol} ${rec.product.name}"
                                            recommendations = repository.getSmartReorderRecommendations()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("1-Click Purchase Order (${rec.suggestedReorderQuantity} ${rec.product.unit.symbol})")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
