package com.example.ui.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.AppSettings
import com.example.ui.theme.CategoryStyleHelper
import com.example.util.CurrencyFormatter

data class PaymentOption(
    val name: String,
    val icon: ImageVector
)

@Composable
fun CheckoutDialog(
    subtotalPesewas: Long,
    discountPesewas: Long,
    totalPesewas: Long,
    settings: AppSettings,
    onCompleteSale: (paymentMethod: String, amountReceivedPesewas: Long, reference: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val paymentOptions = listOf(
        PaymentOption("Cash", Icons.Default.Money),
        PaymentOption("Mobile Money", Icons.Default.PhoneAndroid),
        PaymentOption("Card", Icons.Default.CreditCard),
        PaymentOption("Other", Icons.Default.Payment)
    )

    var selectedMethod by remember { mutableStateOf("Cash") }
    var receivedAmountStr by remember {
        mutableStateOf(CurrencyFormatter.pesewasToEditableString(totalPesewas))
    }
    var transactionRef by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val amountReceivedPesewas = CurrencyFormatter.toPesewas(receivedAmountStr)
    val changeAmountPesewas = (amountReceivedPesewas - totalPesewas).coerceAtLeast(0L)
    val isInsufficient = selectedMethod == "Cash" && amountReceivedPesewas < totalPesewas

    val quickAddValues = listOf(500L, 1000L, 2000L, 5000L, 10000L) // in pesewas (5, 10, 20, 50, 100)

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
                        text = "Checkout & Payment",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Order summary card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal:", style = MaterialTheme.typography.bodyMedium)
                            Text(CurrencyFormatter.formatPesewas(subtotalPesewas, settings.currency), style = MaterialTheme.typography.bodyMedium)
                        }
                        if (discountPesewas > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Discount:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                                Text("-${CurrencyFormatter.formatPesewas(discountPesewas, settings.currency)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("TOTAL TO PAY:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = CurrencyFormatter.formatPesewas(totalPesewas, settings.currency),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Select Payment Method",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Payment method selector chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    paymentOptions.forEach { option ->
                        val isSelected = selectedMethod == option.name
                        val style = CategoryStyleHelper.getPaymentStyle(option.name)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    selectedMethod = option.name
                                    if (option.name != "Cash") {
                                        receivedAmountStr = CurrencyFormatter.pesewasToEditableString(totalPesewas)
                                    }
                                },
                            color = if (isSelected) style.color else style.containerColor.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = style.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else style.color,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = option.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else style.color
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedMethod == "Cash") {
                    Text(
                        text = "Amount Received (${settings.currency})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = receivedAmountStr,
                        onValueChange = {
                            receivedAmountStr = it
                            errorMsg = null
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("checkout_received_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick Cash Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.clickable {
                                    receivedAmountStr = CurrencyFormatter.pesewasToEditableString(totalPesewas)
                                }
                            ) {
                                Text(
                                    text = "Exact",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                        items(quickAddValues) { addPesewas ->
                            val addCedis = addPesewas / 100
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable {
                                    val current = CurrencyFormatter.toPesewas(receivedAmountStr)
                                    val updated = current + addPesewas
                                    receivedAmountStr = CurrencyFormatter.pesewasToEditableString(updated)
                                }
                            ) {
                                Text(
                                    text = "+$addCedis",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Change Calculation Display
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = if (isInsufficient) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isInsufficient) "Shortfall:" else "Change to Return:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            val displayChange = if (isInsufficient) {
                                CurrencyFormatter.formatPesewas(totalPesewas - amountReceivedPesewas, settings.currency)
                            } else {
                                CurrencyFormatter.formatPesewas(changeAmountPesewas, settings.currency)
                            }
                            Text(
                                text = displayChange,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isInsufficient) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                } else {
                    // Mobile Money / Card Transaction Reference
                    Text(
                        text = "Transaction Reference / ID (Optional)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = transactionRef,
                        onValueChange = { transactionRef = it },
                        placeholder = { Text("e.g. MOM-12345 or Auth Code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMsg ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", maxLines = 1, softWrap = false)
                    }

                    Button(
                        onClick = {
                            if (selectedMethod == "Cash" && isInsufficient) {
                                errorMsg = "Amount received is less than the total."
                                return@Button
                            }
                            val finalReceived = if (selectedMethod == "Cash") amountReceivedPesewas else totalPesewas
                            onCompleteSale(selectedMethod, finalReceived, transactionRef.takeIf { it.isNotBlank() })
                        },
                        enabled = !isInsufficient,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("checkout_complete_sale_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Complete Sale", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }
    }
}
