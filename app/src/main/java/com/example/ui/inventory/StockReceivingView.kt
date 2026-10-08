package com.example.ui.inventory

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.entity.Category
import com.example.data.entity.Product
import com.example.data.repository.StockReceivingItem
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.AppThemeColors
import com.example.ui.theme.LocalIsDarkTheme
import com.example.util.CurrencyFormatter

@Composable
fun StockReceivingView(
    manifest: List<StockReceivingItem>,
    referenceNumber: String,
    supplierName: String,
    notes: String,
    currency: String,
    categories: List<Category>,
    lowStockCount: Int,
    isReceivingInProgress: Boolean,
    onReferenceChanged: (String) -> Unit,
    onSupplierChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onGenerateNewReference: () -> Unit,
    onOpenBulkPicker: () -> Unit,
    onOpenScanner: () -> Unit,
    onOpenNewProductDialog: () -> Unit,
    onQuickAddLowStock: () -> Unit,
    onUpdateQuantity: (productId: Long, quantity: Int) -> Unit,
    onUpdateCostPrice: (productId: Long, costPricePesewas: Long) -> Unit,
    onToggleUpdateCatalogCost: (productId: Long) -> Unit,
    onRemoveItem: (productId: Long) -> Unit,
    onSetAllQuantities: (quantity: Int) -> Unit,
    onClearManifest: () -> Unit,
    onConfirmReceiving: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    var showSetAllDialog by remember { mutableStateOf(false) }
    var setAllQtyText by remember { mutableStateOf("20") }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val totalUnits = manifest.sumOf { it.quantityToAdd }
    val totalCost = manifest.sumOf { it.subtotalCostPesewas }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // 1. Shipment & Delivery Details Card
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.LocalShipping,
                                            contentDescription = null,
                                            tint = if (isDark) Color(0xFF34D399) else Color(0xFF16A34A),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Delivery & Inward Batch Details",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Quick clear button if manifest has items
                            if (manifest.isNotEmpty()) {
                                TextButton(
                                    onClick = onClearManifest,
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Clear All", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Reference Number with Regenerate Action
                        OutlinedTextField(
                            value = referenceNumber,
                            onValueChange = onReferenceChanged,
                            label = { Text("Reference / Delivery Note #") },
                            placeholder = { Text("e.g. RCV-20261008-1001 or INV-8492") },
                            trailingIcon = {
                                IconButton(onClick = onGenerateNewReference) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Generate new reference"
                                    )
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("receiving_ref_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Supplier Name
                        OutlinedTextField(
                            value = supplierName,
                            onValueChange = onSupplierChanged,
                            label = { Text("Supplier / Vendor Name (Optional)") },
                            placeholder = { Text("e.g. Accra Central Distributors, Nestlé Direct") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("receiving_supplier_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Notes / Memo
                        OutlinedTextField(
                            value = notes,
                            onValueChange = onNotesChanged,
                            label = { Text("Order Notes / Consignment Details (Optional)") },
                            placeholder = { Text("e.g. Weekly replenishment stock arrival") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("receiving_notes_input")
                        )
                    }
                }
            }

            // 2. Action Toolbar: Bulk Add, Scanner, Quick Low Stock, New Product
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Bulk Product Picker Button (Primary Action)
                    Button(
                        onClick = onOpenBulkPicker,
                        modifier = Modifier
                            .weight(1.2f)
                            .height(44.dp)
                            .testTag("open_bulk_picker_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlaylistAdd,
                                contentDescription = null,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Bulk Add",
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    // Barcode Scanner Button
                    OutlinedButton(
                        onClick = onOpenScanner,
                        modifier = Modifier
                            .weight(0.9f)
                            .height(44.dp)
                            .testTag("receiving_scan_barcode_btn"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Scan",
                                maxLines = 1,
                                softWrap = false,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    // Quick Add Low Stock Button
                    if (lowStockCount > 0) {
                        OutlinedButton(
                            onClick = onQuickAddLowStock,
                            modifier = Modifier
                                .weight(1.1f)
                                .height(44.dp)
                                .testTag("receiving_quick_low_stock_btn"),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFD97706)
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Low ($lowStockCount)",
                                    maxLines = 1,
                                    softWrap = false,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }

                    // New Catalog Product Button
                    OutlinedButton(
                        onClick = onOpenNewProductDialog,
                        modifier = Modifier
                            .weight(0.8f)
                            .height(44.dp)
                            .testTag("receiving_new_product_btn"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "New",
                                maxLines = 1,
                                softWrap = false,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            // 3. Manifest Sub-header & Bulk Utilities
            if (manifest.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Receiving Manifest (${manifest.size} products • $totalUnits units)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedButton(
                            onClick = { showSetAllDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("set_all_qty_btn")
                        ) {
                            Icon(Icons.Default.FormatListNumbered, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Set All Qty", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // 4. Manifest Items or Empty State
            if (manifest.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.PlaylistAdd,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Receiving Manifest is Empty",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Select multiple products from your catalog in one click, scan barcodes, or quickly add all low-stock items to restock your store.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = onOpenBulkPicker,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.PlaylistAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Bulk Select Products")
                                }

                                if (lowStockCount > 0) {
                                    OutlinedButton(
                                        onClick = onQuickAddLowStock,
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.WarningAmber, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Restock Low Items")
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                items(manifest, key = { it.product.id }) { item ->
                    ReceivingManifestItemCard(
                        item = item,
                        currency = currency,
                        onUpdateQuantity = { qty -> onUpdateQuantity(item.product.id, qty) },
                        onUpdateCostPrice = { cost -> onUpdateCostPrice(item.product.id, cost) },
                        onToggleUpdateCatalogCost = { onToggleUpdateCatalogCost(item.product.id) },
                        onRemove = { onRemoveItem(item.product.id) }
                    )
                }
            }
        }

        // 5. Bottom Sticky Summary and Confirmation Bar
        if (manifest.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total Receiving Value",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyFormatter.formatPesewas(totalCost, currency),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Surface(
                            color = Color(0xFFDCFCE7),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "+$totalUnits Units Across ${manifest.size} Products",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF16A34A),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { showConfirmDialog = true },
                        enabled = !isReceivingInProgress && totalUnits > 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("confirm_receive_stock_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isReceivingInProgress) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Updating Inventory...")
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Receive Stock ($totalUnits units)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

    // Set All Quantities Dialog
    if (showSetAllDialog) {
        AlertDialog(
            onDismissRequest = { showSetAllDialog = false },
            title = { Text("Set Same Quantity for All") },
            text = {
                Column {
                    Text(
                        text = "Enter the quantity to apply to all ${manifest.size} products in this receiving order:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = setAllQtyText,
                        onValueChange = { setAllQtyText = it.filter { char -> char.isDigit() } },
                        label = { Text("Quantity per product") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = setAllQtyText.toIntOrNull() ?: 1
                        if (qty > 0) {
                            onSetAllQuantities(qty)
                        }
                        showSetAllDialog = false
                    }
                ) {
                    Text("Apply to All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation Alert
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(28.dp)
                )
            },
            title = { Text("Confirm Stock Receiving") },
            text = {
                Column {
                    Text(
                        text = "You are about to receive $totalUnits units for ${manifest.size} product(s) under reference '$referenceNumber'.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This will immediately increase inventory levels and create stock addition audit logs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        onConfirmReceiving()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Confirm & Receive")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Review")
                }
            }
        )
    }
}

@Composable
private fun ReceivingManifestItemCard(
    item: StockReceivingItem,
    currency: String,
    onUpdateQuantity: (Int) -> Unit,
    onUpdateCostPrice: (Long) -> Unit,
    onToggleUpdateCatalogCost: () -> Unit,
    onRemove: () -> Unit
) {
    var rawCostText by remember(item.unitCostPesewas) {
        mutableStateOf(CurrencyFormatter.pesewasToEditableString(item.unitCostPesewas))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Product Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.product.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "SKU: ${item.product.sku} ${if (!item.product.barcode.isNullOrBlank()) "• Barcode: ${item.product.barcode}" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Remove item",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stock Transition Pill: Current -> + Received -> New
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Stock: ${item.product.stockQuantity} ${item.product.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = Color(0xFF16A34A)
                    )
                    Text(
                        text = "${item.quantityToAdd} received",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A)
                    )
                    Text(
                        text = "= New: ${item.newStockQuantity} ${item.product.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quantity Control Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Quantity to Add:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { if (item.quantityToAdd > 1) onUpdateQuantity(item.quantityToAdd - 1) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease")
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "${item.quantityToAdd}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    IconButton(
                        onClick = { onUpdateQuantity(item.quantityToAdd + 1) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase")
                    }
                }
            }

            // Quick Qty Jump Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(5, 10, 25, 50).forEach { bump ->
                    OutlinedButton(
                        onClick = { onUpdateQuantity(item.quantityToAdd + bump) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+$bump", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Unit Cost & Catalog Update Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = rawCostText,
                    onValueChange = { input: String ->
                        val filtered = input.filter { it.isDigit() || it == '.' }
                        rawCostText = filtered
                        val pesewas = CurrencyFormatter.toPesewas(filtered)
                        onUpdateCostPrice(pesewas)
                    },
                    label = { Text("Unit Cost ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1.2f)
                )

                Column(modifier = Modifier.weight(1.3f)) {
                    Text(
                        text = "Line Cost: ${CurrencyFormatter.formatPesewas(item.subtotalCostPesewas, currency)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Checkbox(
                            checked = item.updateCatalogCost,
                            onCheckedChange = { onToggleUpdateCatalogCost() },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            text = "Update catalog cost",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
