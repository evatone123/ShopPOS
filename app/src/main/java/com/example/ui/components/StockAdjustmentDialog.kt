package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockAdjustmentDialog(
    product: Product,
    onConfirm: (type: String, quantity: Int, reason: String) -> Unit,
    onDismiss: () -> Unit
) {
    val adjustmentTypes = listOf(
        "ADD_STOCK" to "Add Stock (New shipment/restock)",
        "REMOVE_STOCK" to "Remove Stock (General reduction)",
        "CORRECTION" to "Stock Correction (Set absolute stock)",
        "DAMAGED" to "Damaged Goods (Write-off)",
        "EXPIRED" to "Expired Goods (Write-off)",
        "OTHER" to "Other Adjustment"
    )

    var selectedType by remember { mutableStateOf(adjustmentTypes[0].first) }
    var expandedTypeMenu by remember { mutableStateOf(false) }
    var quantityText by remember { mutableStateOf("") }
    var reasonText by remember { mutableStateOf("New shipment") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Inventory,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Adjust Stock",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = product.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Current stock indicator
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Current Stock Level:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${product.stockQuantity} ${product.unit}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Adjustment Type Selector
                ExposedDropdownMenuBox(
                    expanded = expandedTypeMenu,
                    onExpandedChange = { expandedTypeMenu = it }
                ) {
                    OutlinedTextField(
                        value = adjustmentTypes.find { it.first == selectedType }?.second ?: selectedType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Adjustment Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTypeMenu) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedTypeMenu,
                        onDismissRequest = { expandedTypeMenu = false }
                    ) {
                        adjustmentTypes.forEach { (typeKey, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    selectedType = typeKey
                                    expandedTypeMenu = false
                                    // Default preset reasons
                                    reasonText = when (typeKey) {
                                        "ADD_STOCK" -> "New shipment"
                                        "REMOVE_STOCK" -> "Stock discrepancy removal"
                                        "CORRECTION" -> "Physical inventory count"
                                        "DAMAGED" -> "Damaged goods write-off"
                                        "EXPIRED" -> "Expired goods disposal"
                                        else -> "Manual stock adjustment"
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val qtyLabel = if (selectedType == "CORRECTION") "New Absolute Stock Count" else "Quantity"
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = {
                        quantityText = it.filter { ch -> ch.isDigit() }
                        errorMessage = null
                    },
                    label = { Text(qtyLabel) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = reasonText,
                    onValueChange = { reasonText = it },
                    label = { Text("Reason / Note") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

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
                            val qty = quantityText.toIntOrNull()
                            if (qty == null || qty < 0) {
                                errorMessage = "Please enter a valid positive quantity"
                                return@Button
                            }
                            if (selectedType != "CORRECTION" && qty == 0) {
                                errorMessage = "Quantity must be greater than 0"
                                return@Button
                            }
                            if ((selectedType == "REMOVE_STOCK" || selectedType == "DAMAGED" || selectedType == "EXPIRED") && qty > product.stockQuantity) {
                                errorMessage = "Cannot remove $qty. Available stock is only ${product.stockQuantity}."
                                return@Button
                            }
                            onConfirm(selectedType, qty, reasonText.trim())
                        }
                    ) {
                        Text("Save Adjustment")
                    }
                }
            }
        }
    }
}
