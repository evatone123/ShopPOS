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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MoveToInbox
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
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
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.components.BulkProductPickerDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.SearchInputBar
import com.example.ui.components.StatCard
import com.example.ui.components.StockAdjustmentDialog
import com.example.ui.components.StockReceivingSummaryDialog
import com.example.ui.components.StockStatusBadge
import com.example.ui.products.AddEditProductDialog
import com.example.ui.theme.AppThemeColors
import com.example.ui.theme.LocalIsDarkTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel,
    initialTab: Int = 0,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var productForAdjustment by remember { mutableStateOf<Product?>(null) }

    LaunchedEffect(initialTab) {
        if (initialTab in 0..2 && uiState.selectedTab != initialTab) {
            viewModel.onTabChanged(initialTab)
        }
    }

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
            // Tabs: Stock Overview | Receive Stock | Movement History
            TabRow(selectedTabIndex = uiState.selectedTab) {
                Tab(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.onTabChanged(0) },
                    text = { Text("Stock Overview") },
                    icon = { Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.onTabChanged(1) },
                    text = {
                        val count = uiState.receivingManifest.size
                        if (count > 0) {
                            Text("Receive Stock ($count)")
                        } else {
                            Text("Receive Stock")
                        }
                    },
                    icon = { Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_receive_stock")
                )
                Tab(
                    selected = uiState.selectedTab == 2,
                    onClick = { viewModel.onTabChanged(2) },
                    text = { Text("Movement Logs") },
                    icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            when (uiState.selectedTab) {
                0 -> {
                    // TAB 0: Stock Overview Tab
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Receive Stock Banner
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.LocalShipping,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Stock Delivery & Receiving",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Bulk restock multiple products at once",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Button(
                                    onClick = { viewModel.onTabChanged(1) },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("overview_go_to_receive_btn")
                                ) {
                                    Icon(Icons.Default.MoveToInbox, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Receive Stock", maxLines = 1)
                                }
                            }
                        }

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
                }

                1 -> {
                    // TAB 1: Stock Addition / Receiving Workflow
                    StockReceivingView(
                        manifest = uiState.receivingManifest,
                        referenceNumber = uiState.receivingReference,
                        supplierName = uiState.receivingSupplier,
                        notes = uiState.receivingNotes,
                        currency = uiState.settings.currency,
                        categories = uiState.categories,
                        lowStockCount = uiState.lowStockCount + uiState.outOfStockCount,
                        isReceivingInProgress = uiState.isReceivingInProgress,
                        onReferenceChanged = viewModel::onReceivingReferenceChanged,
                        onSupplierChanged = viewModel::onReceivingSupplierChanged,
                        onNotesChanged = viewModel::onReceivingNotesChanged,
                        onGenerateNewReference = viewModel::generateNewReceivingReference,
                        onOpenBulkPicker = viewModel::openBulkPicker,
                        onOpenScanner = viewModel::openScanner,
                        onOpenNewProductDialog = viewModel::openNewProductDialog,
                        onQuickAddLowStock = { viewModel.quickAddLowStockToManifest(10) },
                        onUpdateQuantity = viewModel::updateReceivingItemQuantity,
                        onUpdateCostPrice = viewModel::updateReceivingItemCost,
                        onToggleUpdateCatalogCost = viewModel::toggleUpdateCatalogCost,
                        onRemoveItem = viewModel::removeReceivingItem,
                        onSetAllQuantities = viewModel::setAllReceivingQuantities,
                        onClearManifest = viewModel::clearReceivingManifest,
                        onConfirmReceiving = viewModel::confirmAndReceiveStock,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                2 -> {
                    // TAB 2: Movement Logs Tab
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Inventory Movements Audit Log",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tracks all sales, shipments, stock receipts, and adjustments.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (uiState.movements.isEmpty()) {
                            EmptyStateView(
                                title = "No Movements Logged",
                                message = "Inventory movements will appear here when sales are completed or stock is received.",
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
    }

    // Dialogs

    // 1. Single Product Adjustment Dialog
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

    // 2. Bulk Product Picker Dialog
    if (uiState.isBulkPickerOpen) {
        BulkProductPickerDialog(
            products = uiState.products,
            categories = uiState.categories,
            currency = uiState.settings.currency,
            alreadyAddedProductIds = uiState.receivingManifest.map { it.product.id }.toSet(),
            onConfirmSelection = { selectedList, defaultQty ->
                viewModel.addProductsToReceivingManifest(selectedList, defaultQty)
            },
            onDismiss = viewModel::closeBulkPicker
        )
    }

    // 3. Barcode Scanner Dialog for Receiving
    if (uiState.isScannerOpen) {
        BarcodeScannerDialog(
            onBarcodeScanned = viewModel::onBarcodeScanned,
            onDismiss = viewModel::closeScanner
        )
    }

    // 4. New Product Dialog (create & add directly to receiving manifest)
    if (uiState.isNewProductDialogOpen) {
        AddEditProductDialog(
            categories = uiState.categories,
            currency = uiState.settings.currency,
            onSaveProduct = { newProduct ->
                viewModel.saveNewProductAndAddToReceiving(newProduct, 10)
            },
            onDismiss = viewModel::closeNewProductDialog
        )
    }

    // 5. Stock Receiving Summary Dialog
    uiState.lastReceivingResult?.let { result ->
        StockReceivingSummaryDialog(
            result = result,
            settings = uiState.settings,
            onViewMovementLogs = {
                viewModel.onTabChanged(2) // Jump to movement logs
            },
            onDismiss = viewModel::dismissReceivingSummary
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
    val isDark = LocalIsDarkTheme.current
    val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.US)
    val dateStr = dateFormat.format(Date(movement.createdAt))

    val isAddition = movement.newStock >= movement.previousStock
    val diff = movement.newStock - movement.previousStock

    val (badgeBg, iconColor, icon) = when {
        movement.type.contains("SALE_RETURN") -> Triple(
            if (isDark) Color(0xFF0369A1).copy(alpha = 0.35f) else Color(0xFFE0F2FE),
            if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
            Icons.Default.AssignmentReturn
        )
        movement.type.contains("SALE") -> Triple(
            if (isDark) Color(0xFF312E81).copy(alpha = 0.35f) else Color(0xFFEEF2FF),
            if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5),
            Icons.Default.TrendingDown
        )
        movement.type.contains("DAMAGED") || movement.type.contains("EXPIRED") -> Triple(
            if (isDark) Color(0xFF881337).copy(alpha = 0.35f) else Color(0xFFFFE4E6),
            if (isDark) Color(0xFFFB7185) else Color(0xFFE11D48),
            Icons.Default.DeleteOutline
        )
        movement.type.contains("CORRECTION") -> Triple(
            if (isDark) Color(0xFF78350F).copy(alpha = 0.35f) else Color(0xFFFEF3C7),
            if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
            Icons.Default.SwapVert
        )
        isAddition -> Triple(
            if (isDark) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFDCFCE7),
            if (isDark) Color(0xFF34D399) else Color(0xFF16A34A),
            Icons.Default.TrendingUp
        )
        else -> Triple(
            if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.35f) else Color(0xFFFEE2E2),
            if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
            Icons.Default.TrendingDown
        )
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
                    if (!movement.reference.isNullOrBlank()) {
                        Text(
                            text = "Ref: ${movement.reference}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
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
                    color = if (isAddition) AppThemeColors.successText else AppThemeColors.dangerText
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
