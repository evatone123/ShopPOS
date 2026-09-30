package com.example.ui.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.Category
import com.example.data.entity.Product
import com.example.ui.components.BarcodeScannerDialog
import com.example.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductDialog(
    initialProduct: Product? = null,
    categories: List<Category>,
    prefilledBarcode: String? = null,
    currency: String = "GHS",
    onSaveProduct: (Product) -> Unit,
    onDismiss: () -> Unit
) {
    val isEditing = initialProduct != null

    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var sku by remember { mutableStateOf(initialProduct?.sku ?: "") }
    var barcode by remember {
        mutableStateOf(initialProduct?.barcode ?: prefilledBarcode ?: "")
    }
    var selectedCategoryId by remember {
        mutableStateOf(initialProduct?.categoryId ?: categories.firstOrNull()?.id)
    }
    var costPriceStr by remember {
        mutableStateOf(
            if (initialProduct != null) CurrencyFormatter.pesewasToEditableString(initialProduct.costPricePesewas)
            else "0.00"
        )
    }
    var sellingPriceStr by remember {
        mutableStateOf(
            if (initialProduct != null) CurrencyFormatter.pesewasToEditableString(initialProduct.sellingPricePesewas)
            else "0.00"
        )
    }
    var stockQuantityStr by remember {
        mutableStateOf((initialProduct?.stockQuantity ?: 0).toString())
    }
    var minimumStockStr by remember {
        mutableStateOf((initialProduct?.minimumStock ?: 10).toString())
    }
    var unit by remember { mutableStateOf(initialProduct?.unit ?: "Pcs") }
    var isActive by remember { mutableStateOf(initialProduct?.active ?: true) }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val commonUnits = listOf("Pcs", "Bottle", "Loaf", "Sachet", "Tin", "Box", "Pack", "Kg", "Roll")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) "Edit Product" else "Add New Product",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Product Name
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Product Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // SKU & Barcode Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text("SKU (Optional)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_sku_input")
                    )

                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("Barcode") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { showBarcodeScanner = true }) {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    contentDescription = "Scan barcode",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("product_barcode_input")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = it }
                ) {
                    val currentCategoryName = categories.find { it.id == selectedCategoryId }?.name ?: "Select Category"
                    OutlinedTextField(
                        value = currentCategoryName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCategoryId = cat.id
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Cost Price & Selling Price Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = costPriceStr,
                        onValueChange = { costPriceStr = it },
                        label = { Text("Cost ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = sellingPriceStr,
                        onValueChange = {
                            sellingPriceStr = it
                            errorMessage = null
                        },
                        label = { Text("Selling ($currency) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_selling_price_input")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Stock Quantity, Minimum Stock & Unit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = stockQuantityStr,
                        onValueChange = { stockQuantityStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Stock Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = minimumStockStr,
                        onValueChange = { minimumStockStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Min Alert") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit") },
                        singleLine = true,
                        modifier = Modifier.weight(0.9f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Active Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Product Status",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isActive) "Active (Available for sale)" else "Inactive (Hidden from POS)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it }
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                errorMessage = "Product Name is required"
                                return@Button
                            }
                            val sellingPesewas = CurrencyFormatter.toPesewas(sellingPriceStr)
                            if (sellingPesewas <= 0L) {
                                errorMessage = "Selling price must be greater than 0.00"
                                return@Button
                            }
                            val costPesewas = CurrencyFormatter.toPesewas(costPriceStr)
                            val stock = stockQuantityStr.toIntOrNull() ?: 0
                            val minStock = minimumStockStr.toIntOrNull() ?: 10

                            val now = System.currentTimeMillis()
                            val product = Product(
                                id = initialProduct?.id ?: 0L,
                                name = name.trim(),
                                sku = if (sku.isNotBlank()) sku.trim() else name.take(4).uppercase() + (100..999).random(),
                                barcode = barcode.trim().takeIf { it.isNotBlank() },
                                categoryId = selectedCategoryId,
                                costPricePesewas = costPesewas,
                                sellingPricePesewas = sellingPesewas,
                                stockQuantity = stock,
                                minimumStock = minStock,
                                unit = if (unit.isNotBlank()) unit.trim() else "Pcs",
                                active = isActive,
                                createdAt = initialProduct?.createdAt ?: now,
                                updatedAt = now
                            )

                            onSaveProduct(product)
                            onDismiss()
                        },
                        modifier = Modifier.testTag("save_product_btn")
                    ) {
                        Text(if (isEditing) "Update Product" else "Save Product")
                    }
                }
            }
        }
    }

    if (showBarcodeScanner) {
        BarcodeScannerDialog(
            onBarcodeScanned = { scanned ->
                barcode = scanned
                showBarcodeScanner = false
            },
            onDismiss = { showBarcodeScanner = false }
        )
    }
}
