package com.example.ui.sales

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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.SaleWithItems
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ReceiptDialog
import com.example.ui.components.SearchInputBar
import com.example.ui.theme.AppThemeColors
import com.example.ui.theme.CategoryStyleHelper
import com.example.ui.theme.LocalIsDarkTheme
import com.example.util.CurrencyFormatter
import com.example.util.ReceiptHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(
    viewModel: SalesViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDark = LocalIsDarkTheme.current
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedSaleForReceipt by remember { mutableStateOf<SaleWithItems?>(null) }
    var saleToVoid by remember { mutableStateOf<SaleWithItems?>(null) }
    var voidReason by remember { mutableStateOf("Customer returned goods") }

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
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search bar
            SearchInputBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChanged,
                placeholder = "Search receipt #, date, product...",
                modifier = Modifier.testTag("sales_search_bar")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filters: Payment Methods
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val methods = listOf("ALL", "Cash", "Mobile Money", "Card", "Other")
                items(methods) { method ->
                    val isSelected = uiState.selectedPaymentMethod.equals(method, ignoreCase = true)
                    val style = if (method == "ALL") null else CategoryStyleHelper.getPaymentStyle(method)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onPaymentMethodFilterChanged(method) },
                        label = { Text(if (method == "ALL") "All Payments" else method, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = {
                            if (style != null) {
                                Icon(
                                    imageVector = style.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else style.color,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Icon(
                                    Icons.Default.Payment,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Filters: Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val statuses = listOf("ALL" to "All Status", "COMPLETED" to "Completed", "CANCELLED" to "Voided")
                    statuses.forEach { (statusCode, label) ->
                        FilterChip(
                            selected = uiState.selectedStatus == statusCode,
                            onClick = { viewModel.onStatusFilterChanged(statusCode) },
                            label = { Text(label) }
                        )
                    }
                }

                Text(
                    text = "${uiState.filteredSales.size} sales",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.filteredSales.isEmpty()) {
                EmptyStateView(
                    title = "No Sales Found",
                    message = if (uiState.searchQuery.isNotBlank()) "No sales matched '${uiState.searchQuery}'" else "No sales have been recorded yet.",
                    icon = Icons.Default.ReceiptLong,
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(uiState.filteredSales, key = { it.sale.id }) { saleWithItems ->
                        val sale = saleWithItems.sale
                        val isCancelled = sale.status == "CANCELLED"
                        val dateFormat = SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.US)
                        val formattedDate = dateFormat.format(Date(sale.createdAt))
                        val paymentStyle = CategoryStyleHelper.getPaymentStyle(sale.paymentMethod)

                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedSaleForReceipt = saleWithItems },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = if (isCancelled) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(paymentStyle.containerColor, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = paymentStyle.icon,
                                                contentDescription = null,
                                                tint = paymentStyle.color,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = sale.receiptNumber,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = formattedDate,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Surface(
                                        color = if (isCancelled) MaterialTheme.colorScheme.errorContainer else Color(0xFFDCFCE7),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isCancelled) Icons.Default.Cancel else Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = if (isCancelled) MaterialTheme.colorScheme.onErrorContainer else AppThemeColors.successText,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isCancelled) "VOIDED" else "COMPLETED",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCancelled) MaterialTheme.colorScheme.onErrorContainer else AppThemeColors.successText
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Items summary
                                val itemsSummary = saleWithItems.items.joinToString(", ") { "${it.quantity}x ${it.productName}" }
                                Text(
                                    text = itemsSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = CurrencyFormatter.formatPesewas(sale.totalPesewas, uiState.settings.currency),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black,
                                            color = if (isCancelled) MaterialTheme.colorScheme.error else AppThemeColors.successText
                                        )
                                        Text(
                                            text = "Method: ${sale.paymentMethod} • Cashier: ${sale.cashierName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(
                                            onClick = { ReceiptHelper.printReceipt(context, saleWithItems, uiState.settings) },
                                            shape = CircleShape,
                                            color = Color(0xFFEEF2FF),
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Print,
                                                    contentDescription = "Print",
                                                    tint = Color(0xFF4F46E5),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        Surface(
                                            onClick = { ReceiptHelper.shareReceipt(context, saleWithItems, uiState.settings) },
                                            shape = CircleShape,
                                            color = Color(0xFFCCFBF1),
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Share,
                                                    contentDescription = "Share",
                                                    tint = Color(0xFF0D9488),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        if (!isCancelled) {
                                            OutlinedButton(
                                                onClick = { saleToVoid = saleWithItems },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text("Void", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Receipt Dialog
    selectedSaleForReceipt?.let { saleWithItems ->
        ReceiptDialog(
            saleWithItems = saleWithItems,
            settings = uiState.settings,
            onDismiss = { selectedSaleForReceipt = null }
        )
    }

    // Void Sale Confirmation Dialog
    saleToVoid?.let { saleWithItems ->
        AlertDialog(
            onDismissRequest = { saleToVoid = null },
            title = { Text("Void / Cancel Sale?") },
            text = {
                Column {
                    Text(
                        text = "Are you sure you want to cancel receipt '${saleWithItems.sale.receiptNumber}'?\n\nThis will mark the sale as CANCELLED and automatically restore all items back to stock inventory."
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = voidReason,
                        onValueChange = { voidReason = it },
                        label = { Text("Cancellation Reason") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.voidSale(saleWithItems.sale.id, voidReason.trim())
                        saleToVoid = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Void")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { saleToVoid = null }) {
                    Text("Keep Sale")
                }
            }
        )
    }
}
