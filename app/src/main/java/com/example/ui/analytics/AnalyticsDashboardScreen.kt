package com.example.ui.analytics

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.repository.BakeStockRepository
import com.example.ui.shared.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsDashboardScreen(
    repository: BakeStockRepository,
    onBack: () -> Unit
) {
    var analyticsSummary by remember { mutableStateOf<AnalyticsSummary?>(null) }
    var selectedTimeframe by remember { mutableStateOf("Monthly") }

    LaunchedEffect(Unit) {
        analyticsSummary = repository.getAnalyticsSummary()
    }

    val summary = analyticsSummary

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics & Executive Insights", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (summary == null) {
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
                // Timeframe Selector
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Executive Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        SingleChoiceSegmentedButtonRow {
                            listOf("Weekly", "Monthly", "Annual").forEachIndexed { idx, tf ->
                                SegmentedButton(
                                    selected = selectedTimeframe == tf,
                                    onClick = { selectedTimeframe = tf },
                                    shape = SegmentedButtonDefaults.itemShape(index = idx, count = 3)
                                ) {
                                    Text(tf, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                // Metric Cards Grid
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SummaryCard(
                                title = "Stock Valuation",
                                value = "$${summary.totalStockValuation}",
                                icon = Icons.Default.AccountBalanceWallet,
                                containerColor = AmberPrimary.copy(alpha = 0.15f),
                                contentColor = AmberPrimaryDark,
                                modifier = Modifier.weight(1f)
                            )
                            SummaryCard(
                                title = "Monthly Purchases",
                                value = "$${summary.totalPurchasesMonth}",
                                icon = Icons.Default.ShoppingCart,
                                containerColor = ForestGreenTertiary.copy(alpha = 0.15f),
                                contentColor = ForestGreenTertiary,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SummaryCard(
                                title = "Wastage Loss",
                                value = "$${summary.monthlyWastageValuation}",
                                icon = Icons.Default.DeleteOutline,
                                containerColor = StatusRed.copy(alpha = 0.15f),
                                contentColor = StatusRed,
                                modifier = Modifier.weight(1f)
                            )
                            SummaryCard(
                                title = "Stock Health Ratio",
                                value = "${summary.inStockCount} / ${summary.inStockCount + summary.lowStockCount + summary.nearExpiryCount + summary.expiredCount} OK",
                                icon = Icons.Default.HealthAndSafety,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Stock Health Breakdown Visual Chart
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Inventory Health Distribution",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            val total = (summary.inStockCount + summary.lowStockCount + summary.nearExpiryCount + summary.expiredCount).toFloat().coerceAtLeast(1f)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(18.dp)
                                    .clip(RoundedCornerShape(9.dp))
                            ) {
                                Box(modifier = Modifier.weight((summary.inStockCount / total).coerceAtLeast(0.01f)).fillMaxHeight().background(ForestGreenTertiary))
                                Box(modifier = Modifier.weight((summary.lowStockCount / total).coerceAtLeast(0.01f)).fillMaxHeight().background(StatusAmber))
                                Box(modifier = Modifier.weight((summary.nearExpiryCount / total).coerceAtLeast(0.01f)).fillMaxHeight().background(Color(0xFFFF9800)))
                                Box(modifier = Modifier.weight((summary.expiredCount / total).coerceAtLeast(0.01f)).fillMaxHeight().background(StatusRed))
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                LegendItem(color = ForestGreenTertiary, label = "In Stock (${summary.inStockCount})")
                                LegendItem(color = StatusAmber, label = "Low (${summary.lowStockCount})")
                                LegendItem(color = Color(0xFFFF9800), label = "Near Expiry (${summary.nearExpiryCount})")
                                LegendItem(color = StatusRed, label = "Expired (${summary.expiredCount})")
                            }
                        }
                    }
                }

                // Wastage by Reason Donut Chart
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Wastage Valuation by Cause",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            if (summary.wastageByReason.isEmpty()) {
                                Text(
                                    text = "No wastage records logged yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Custom Donut Ring Chart Canvas
                                    Box(
                                        modifier = Modifier.size(120.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Canvas(modifier = Modifier.size(110.dp)) {
                                            val totalVal = summary.wastageByReason.values.sum().toFloat().coerceAtLeast(1f)
                                            var startAngle = -90f
                                            val colors = listOf(StatusRed, Color(0xFFFF9800), StatusAmber, Color(0xFF9C27B0))

                                            summary.wastageByReason.values.forEachIndexed { i, valVal ->
                                                val sweepAngle = (valVal.toFloat() / totalVal) * 360f
                                                drawArc(
                                                    color = colors[i % colors.size],
                                                    startAngle = startAngle,
                                                    sweepAngle = sweepAngle,
                                                    useCenter = false,
                                                    style = Stroke(width = 24.dp.toPx())
                                                )
                                                startAngle += sweepAngle
                                            }
                                        }
                                        Text(
                                            text = "$${summary.monthlyWastageValuation}",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(20.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        val colors = listOf(StatusRed, Color(0xFFFF9800), StatusAmber, Color(0xFF9C27B0))
                                        summary.wastageByReason.entries.forEachIndexed { idx, entry ->
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(colors[idx % colors.size]))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "${entry.key.label}: $${entry.value}",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Category Distribution Breakdown
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Inventory Items by Category",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            summary.categoryDistribution.forEach { (cat, count) ->
                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(cat, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        Text("$count items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { (count.toFloat() / summary.categoryDistribution.values.sum().coerceAtLeast(1)).coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}
