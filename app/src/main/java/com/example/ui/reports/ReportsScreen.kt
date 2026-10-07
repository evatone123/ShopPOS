package com.example.ui.reports

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.StatCard
import com.example.ui.theme.AppThemeColors
import com.example.ui.theme.CategoryStyleHelper
import com.example.ui.theme.LocalIsDarkTheme
import com.example.util.CurrencyFormatter

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDark = LocalIsDarkTheme.current

    Scaffold(modifier = modifier) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Sales & Inventory Reports",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Track sales revenue, payment methods and stock valuation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Period Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(ReportPeriod.values()) { p ->
                        FilterChip(
                            selected = uiState.period == p,
                            onClick = { viewModel.onPeriodSelected(p) },
                            label = { Text(p.label) }
                        )
                    }
                }
            }

            // Key Metrics 2x2 Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "Total Sales",
                            value = CurrencyFormatter.formatPesewas(uiState.totalSalesPesewas, uiState.settings.currency),
                            icon = Icons.Default.TrendingUp,
                            containerColor = Color(0xFFECFDF5),
                            iconTint = Color(0xFF059669),
                            modifier = Modifier.weight(1f)
                        )

                        StatCard(
                            title = "Transactions",
                            value = "${uiState.transactionsCount}",
                            icon = Icons.Default.ReceiptLong,
                            containerColor = Color(0xFFEEF2FF),
                            iconTint = Color(0xFF4F46E5),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "Items Sold",
                            value = "${uiState.itemsSoldCount}",
                            icon = Icons.Default.ShoppingBag,
                            containerColor = Color(0xFFFFFBEB),
                            iconTint = Color(0xFFD97706),
                            modifier = Modifier.weight(1f)
                        )

                        StatCard(
                            title = "Average Ticket",
                            value = CurrencyFormatter.formatPesewas(uiState.averageTransactionPesewas, uiState.settings.currency),
                            icon = Icons.Default.AttachMoney,
                            containerColor = Color(0xFFCFFAFE),
                            iconTint = Color(0xFF0891B2),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Payment Methods Breakdown
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(Color(0xFFEEF2FF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.size(10.dp))
                            Text(
                                text = "Payment Summary (${uiState.period.label})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (uiState.paymentBreakdowns.isEmpty()) {
                            Text(
                                text = "No payments recorded for this period.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            uiState.paymentBreakdowns.forEach { breakdown ->
                                val style = CategoryStyleHelper.getPaymentStyle(breakdown.method, isDark)
                                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .background(style.containerColor, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = style.icon,
                                                    contentDescription = null,
                                                    tint = style.color,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.size(8.dp))
                                            Text(
                                                text = "${breakdown.method} (${breakdown.count} sales)",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Text(
                                            text = "${CurrencyFormatter.formatPesewas(breakdown.totalPesewas, uiState.settings.currency)} (${String.format("%.1f", breakdown.percentage)}%)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = style.color
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { breakdown.percentage / 100f },
                                        color = style.color,
                                        trackColor = style.containerColor.copy(alpha = 0.5f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(CircleShape),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Best-Selling Products
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(Color(0xFFFEF3C7), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Leaderboard,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.size(10.dp))
                            Text(
                                text = "Best-Selling Products",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (uiState.bestSellers.isEmpty()) {
                            Text(
                                text = "No product sales recorded in this period.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Rank & Product", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.6f))
                                    Text("Qty Sold", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.7f))
                                    Text("Revenue", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            uiState.bestSellers.forEachIndexed { index, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1.6f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val (rankBg, rankText) = when (index) {
                                            0 -> if (isDark) Color(0xFF78350F) to Color(0xFFFDE68A) else Color(0xFFFEF3C7) to Color(0xFFB45309)
                                            1 -> if (isDark) Color(0xFF334155) to Color(0xFFE2E8F0) else Color(0xFFE2E8F0) to Color(0xFF475569)
                                            2 -> if (isDark) Color(0xFF7C2D12) to Color(0xFFFFEDD5) else Color(0xFFFFEDD5) to Color(0xFFC2410C)
                                            else -> if (isDark) Color(0xFF1E293B) to Color(0xFF94A3B8) else Color(0xFFF1F5F9) to Color(0xFF64748B)
                                        }
                                        Surface(
                                            color = rankBg,
                                            shape = CircleShape,
                                            modifier = Modifier.size(22.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${index + 1}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = rankText
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            item.productName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Text(
                                        "${item.totalQuantitySold}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(0.7f)
                                    )

                                    Text(
                                        CurrencyFormatter.formatPesewas(item.totalRevenuePesewas, uiState.settings.currency),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = AppThemeColors.successText,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }

            // Inventory Summary & Valuation Card
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Inventory,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "Current Inventory Valuation",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Total Products:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${uiState.products.size} Items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Cost Value:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    CurrencyFormatter.formatPesewas(uiState.totalInventoryStockValueCostPesewas, uiState.settings.currency),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text("Retail Value:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    CurrencyFormatter.formatPesewas(uiState.totalInventoryStockValueRetailPesewas, uiState.settings.currency),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        val potentialProfit = (uiState.totalInventoryStockValueRetailPesewas - uiState.totalInventoryStockValueCostPesewas).coerceAtLeast(0L)
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Potential Gross Margin:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                CurrencyFormatter.formatPesewas(potentialProfit, uiState.settings.currency),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = AppThemeColors.successText
                            )
                        }
                    }
                }
            }
        }
    }
}
