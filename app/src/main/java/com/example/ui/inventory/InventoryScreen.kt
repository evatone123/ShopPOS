package com.example.ui.inventory

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.InventoryMovement
import com.example.data.entity.Product
import com.example.ui.components.EmptyStateView
import com.example.ui.components.SearchInputBar
import com.example.ui.components.StatCard
import com.example.ui.components.StockAdjustmentDialog
import com.example.ui.components.StockStatusBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var productForAdjustment by remember { mutableStateOf<Product?>(null) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearFeedback()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tabs: Stock Overview | Movement History
            TabRow(selectedTabIndex = uiState.selectedTab) {
                Tab(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.onTabChanged(0) },
                    text = { Text("Stock Status") },
                    icon = { Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.onTabChanged(1) },
                    text = { Text("Movement Logs") },
                    icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            if (uiState.selectedTab == 0) {
                // Stock Overview Tab
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(10.dp))

                    // Alert stats summary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "Low Stock Alerts",
                            value = "${uiState.lowStockCount}",
                            icon = Icons.Default.Warning,
                            containerColor = Color(0xFFFFF7ED),
                            iconTint = Color(0xFFEA580C),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.onFilterChanged(InventoryFilter.LOW_STOCK) }
                        )

                        StatCard(
                            title = "Out of Stock",
                            value = "${uiState.outOfStockCount}",
                            icon = Icons.Default.Inventory,
                            containerColor = Color(0xFFFEF2F2),
                            iconTint = Color(0xFFDC2626),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.onFilterChanged(InventoryFilter.OUT_OF_STOCK) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Search input
                    SearchInputBar(
                        query = uiState.searchQuery,
                        onQueryChange = viewModel::onSearchQueryChanged,
                        placeholder = "Search inventory products...",
                        modifier = Modifier.testTag("inventory_search_bar")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Filter chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(InventoryFilter.values()) { filter ->
                            val isSelected = uiState.currentFilter == filter
                            val (chipIcon, iconColor) = when (filter) {
                                InventoryFilter.ALL -> Pair(Icons.Default.ListAlt, Color(0xFF4F46E5))
                                InventoryFilter.IN_STOCK -> Pair(Icons.Default.CheckCircle, Color(0xFF10B981))
                                InventoryFilter.LOW_STOCK -> Pair(Icons.Default.Warning, Color(0xFFF59E0B))
                                InventoryFilter.OUT_OF_STOCK -> Pair(Icons.Default.ErrorOutline, Color(0xFFEF4444))
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.onFilterChanged(filter) },
                                label = { Text(filter.label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = chipIcon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else iconColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Table Header
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Product",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1.4f)
                            )
                            Text(
                                text = "Stock / Min",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "Status / Action",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1.3f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Inventory Items List
                    if (uiState.filteredProducts.isEmpty()) {
                        EmptyStateView(
                            title = "No Inventory Found",
                            message = if (uiState.searchQuery.isNotBlank()) "No products match '${uiState.searchQuery}'" else "No products found for selected filter",
                            icon = Icons.Default.Inventory,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(uiState.filteredProducts, key = { it.id }) { product ->
                                InventoryRowCard(
                                    product = product,
                                    onAdjustStock = { productForAdjustment = product }
                                )
                            }
                        }
                    }
                }
            } else {
                // Movement History Tab
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Inventory Movements Audit Log",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tracks all sales, additions, write-offs, and manual corrections.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (uiState.movements.isEmpty()) {
                        EmptyStateView(
                            title = "No Movements Logged",
                            message = "Inventory movements will appear here when sales are completed or stock is adjusted.",
                            icon = Icons.Default.History,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        val productMap = remember(uiState.products) {
                            uiState.products.associateBy { it.id }
                        }

                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(uiState.movements, key = { it.id }) { movement ->
                                val productName = productMap[movement.productId]?.name ?: "Product #${movement.productId}"
                                MovementLogCard(movement = movement, productName = productName)
                            }
                        }
                    }
                }
            }
        }
    }

    // Stock Adjustment Dialog
    productForAdjustment?.let { product ->
        StockAdjustmentDialog(
            product = product,
            onConfirm = { type, qty, reason ->
                viewModel.adjustStock(product.id, type, qty, reason)
                productForAdjustment = null
            },
            onDismiss = { productForAdjustment = null }
        )
    }
}

@Composable
fun InventoryRowCard(
    product: Product,
    onAdjustStock: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1.4f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "SKU: ${product.sku}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${product.stockQuantity} ${product.unit}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Min: ${product.minimumStock}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                modifier = Modifier.weight(1.3f),
                horizontalAlignment = Alignment.End
            ) {
                StockStatusBadge(status = product.getStockStatus())
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onAdjustStock,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Adjust", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}

@Composable
fun MovementLogCard(
    movement: InventoryMovement,
    productName: String
) {
    val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.US)
    val dateStr = dateFormat.format(Date(movement.createdAt))

    val isAddition = movement.newStock >= movement.previousStock
    val diff = movement.newStock - movement.previousStock

    val (badgeBg, iconColor, icon) = when {
        movement.type.contains("SALE_RETURN") -> Triple(Color(0xFFE0F2FE), Color(0xFF0284C7), Icons.Default.AssignmentReturn)
        movement.type.contains("SALE") -> Triple(Color(0xFFEEF2FF), Color(0xFF4F46E5), Icons.Default.TrendingDown)
        movement.type.contains("DAMAGED") || movement.type.contains("EXPIRED") -> Triple(Color(0xFFFFE4E6), Color(0xFFE11D48), Icons.Default.DeleteOutline)
        movement.type.contains("CORRECTION") -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), Icons.Default.SwapVert)
        isAddition -> Triple(Color(0xFFDCFCE7), Color(0xFF16A34A), Icons.Default.TrendingUp)
        else -> Triple(Color(0xFFFEE2E2), Color(0xFFDC2626), Icons.Default.TrendingDown)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(badgeBg, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = productName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${movement.type.replace("_", " ")} • $dateStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!movement.reason.isNullOrBlank()) {
                        Text(
                            text = movement.reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (diff > 0) "+$diff" else "$diff",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isAddition) Color(0xFF16A34A) else Color(0xFFDC2626)
                )
                Text(
                    text = "${movement.previousStock} → ${movement.newStock}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
