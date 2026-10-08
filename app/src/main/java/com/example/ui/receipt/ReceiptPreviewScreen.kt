package com.example.ui.receipt

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.AppSettings
import com.example.data.entity.SaleWithItems
import com.example.util.CurrencyFormatter
import com.example.util.PdfReceiptHelper
import com.example.util.ReceiptHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Receipt Preview screen that displays a transaction summary in a realistic,
 * high-fidelity layout suitable for thermal or standard printing.
 *
 * Includes dedicated 'Share/Print' actions with direct system print service
 * and PDF sharing intents.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptPreviewScreen(
    saleWithItems: SaleWithItems,
    settings: AppSettings,
    allSales: List<SaleWithItems> = emptyList(),
    onSelectSale: ((SaleWithItems) -> Unit)? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentSale by remember(saleWithItems) { mutableStateOf(saleWithItems) }
    var showActionSheet by remember { mutableStateOf(false) }
    var showSalePickerMenu by remember { mutableStateOf(false) }
    var isThermalMode by remember { mutableStateOf(true) }

    val sale = currentSale.sale
    val items = currentSale.items
    val currency = settings.currency
    val isCancelled = sale.status == "CANCELLED"

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US) }
    val formattedDate = remember(sale.createdAt) { dateFormat.format(Date(sale.createdAt)) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("receipt_preview_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Receipt Preview",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${sale.receiptNumber} • ${if (isThermalMode) "80mm Thermal" else "Standard Invoice"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("receipt_preview_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Toggle thermal / standard format layout
                    IconButton(
                        onClick = { isThermalMode = !isThermalMode },
                        modifier = Modifier.testTag("toggle_format_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = if (isThermalMode) "Switch to Standard Format" else "Switch to Thermal Format",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Transaction switcher if multiple sales provided
                    if (allSales.size > 1 && onSelectSale != null) {
                        Box {
                            IconButton(onClick = { showSalePickerMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "Select another transaction"
                                )
                            }
                            DropdownMenu(
                                expanded = showSalePickerMenu,
                                onDismissRequest = { showSalePickerMenu = false }
                            ) {
                                allSales.take(10).forEach { item ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    item.sale.receiptNumber,
                                                    fontWeight = if (item.sale.id == currentSale.sale.id) FontWeight.Bold else FontWeight.Normal
                                                )
                                                Text(
                                                    "${CurrencyFormatter.formatPesewas(item.sale.totalPesewas, currency)} • ${item.items.size} items",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            currentSale = item
                                            onSelectSale(item)
                                            showSalePickerMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            ReceiptPreviewBottomBar(
                onSharePrint = { showActionSheet = true },
                onDirectPrint = { ReceiptHelper.printReceipt(context, currentSale, settings) },
                onDirectSharePdf = { ReceiptHelper.shareReceipt(context, currentSale, settings) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Printable Paper Receipt Card
                PrintableReceiptCard(
                    saleWithItems = currentSale,
                    settings = settings,
                    isCancelled = isCancelled,
                    formattedDate = formattedDate,
                    isThermalMode = isThermalMode,
                    modifier = Modifier.widthIn(max = 440.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Helpful printing tips banner
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 440.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Print & Export Ready",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Use 'Share/Print' below to send directly to any connected Bluetooth/Wi-Fi printer, save as PDF, or share via WhatsApp/Email.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // Modal Bottom Sheet with Comprehensive Share / Print Options
    if (showActionSheet) {
        ModalBottomSheet(
            onDismissRequest = { showActionSheet = false },
            sheetState = sheetState
        ) {
            SharePrintActionSheetContent(
                context = context,
                currentSale = currentSale,
                settings = settings,
                onDismiss = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showActionSheet = false
                    }
                }
            )
        }
    }
}

/**
 * Realistic printable paper receipt surface with true black-on-white print fidelity.
 */
@Composable
private fun PrintableReceiptCard(
    saleWithItems: SaleWithItems,
    settings: AppSettings,
    isCancelled: Boolean,
    formattedDate: String,
    isThermalMode: Boolean,
    modifier: Modifier = Modifier
) {
    val sale = saleWithItems.sale
    val items = saleWithItems.items
    val currency = settings.currency

    // Paper styling: pure white background and authentic receipt ink colors for true WYSIWYG
    val paperBg = Color(0xFFFFFFFF)
    val inkPrimary = Color(0xFF0F172A)
    val inkSecondary = Color(0xFF475569)
    val inkMuted = Color(0xFF64748B)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .testTag("printable_receipt_paper"),
        color = paperBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Serrated / Perforated top tear indicator
            ReceiptPerforationLine(color = Color(0xFFCBD5E1))

            Spacer(modifier = Modifier.height(16.dp))

            // Shop Logo / Icon
            Surface(
                shape = CircleShape,
                color = Color(0xFFF1F5F9),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        tint = inkPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Shop Header
            Text(
                text = settings.shopName.ifBlank { "RETAIL POS" }.uppercase(),
                color = inkPrimary,
                fontSize = if (isThermalMode) 18.sp else 20.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )

            if (settings.shopAddress.isNotBlank()) {
                Text(
                    text = settings.shopAddress,
                    color = inkSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
            }

            if (settings.phone.isNotBlank()) {
                Text(
                    text = "Tel: ${settings.phone}",
                    color = inkSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Voided Banner if cancelled
            if (isCancelled) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFFEE2E2),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "*** VOIDED / CANCELLED ***",
                            color = Color(0xFFB91C1C),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        if (!sale.cancelReason.isNullOrBlank()) {
                            Text(
                                text = "Reason: ${sale.cancelReason}",
                                color = Color(0xFFB91C1C),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Dashed Divider
            ReceiptDashedDivider()

            Spacer(modifier = Modifier.height(8.dp))

            // Metadata: Receipt number, date, cashier
            ReceiptKeyValueRow(label = "RECEIPT #", value = sale.receiptNumber, isBold = true)
            ReceiptKeyValueRow(label = "DATE", value = formattedDate)
            ReceiptKeyValueRow(label = "CASHIER", value = sale.cashierName.ifBlank { "Staff" })
            ReceiptKeyValueRow(
                label = "STATUS",
                value = if (isCancelled) "VOIDED" else "PAID",
                valueColor = if (isCancelled) Color(0xFFDC2626) else Color(0xFF16A34A),
                isBold = true
            )

            Spacer(modifier = Modifier.height(8.dp))
            ReceiptDashedDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // Table Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ITEM / QTY",
                    color = inkSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1.5f)
                )
                Text(
                    text = "PRICE",
                    color = inkSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(0.9f)
                )
                Text(
                    text = "TOTAL",
                    color = inkSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            ReceiptSolidLine()
            Spacer(modifier = Modifier.height(6.dp))

            // Itemized Rows
            items.forEach { item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1.5f)) {
                            Text(
                                text = item.productName,
                                color = inkPrimary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${item.quantity} x ${CurrencyFormatter.formatPesewas(item.unitPricePesewas, currency)}",
                                color = inkMuted,
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = CurrencyFormatter.formatPesewas(item.unitPricePesewas, currency),
                            color = inkSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(0.9f)
                        )
                        Text(
                            text = CurrencyFormatter.formatPesewas(item.subtotalPesewas, currency),
                            color = inkPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            ReceiptSolidLine()
            Spacer(modifier = Modifier.height(8.dp))

            // Financial Calculations
            ReceiptKeyValueRow(
                label = "SUBTOTAL",
                value = CurrencyFormatter.formatPesewas(sale.subtotalPesewas, currency)
            )

            if (sale.discountPesewas > 0) {
                ReceiptKeyValueRow(
                    label = "DISCOUNT",
                    value = "-${CurrencyFormatter.formatPesewas(sale.discountPesewas, currency)}",
                    valueColor = Color(0xFFDC2626)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            ReceiptDashedDivider()
            Spacer(modifier = Modifier.height(6.dp))

            // Grand Total
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GRAND TOTAL",
                    color = inkPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = CurrencyFormatter.formatPesewas(sale.totalPesewas, currency),
                    color = inkPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            ReceiptDashedDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // Payment Details
            ReceiptKeyValueRow(label = "PAYMENT METHOD", value = sale.paymentMethod, isBold = true)
            ReceiptKeyValueRow(
                label = "AMOUNT PAID",
                value = CurrencyFormatter.formatPesewas(sale.amountReceivedPesewas, currency)
            )
            if (sale.changeAmountPesewas > 0) {
                ReceiptKeyValueRow(
                    label = "CHANGE RETURNED",
                    value = CurrencyFormatter.formatPesewas(sale.changeAmountPesewas, currency),
                    valueColor = Color(0xFF16A34A)
                )
            }
            if (!sale.paymentReference.isNullOrBlank()) {
                ReceiptKeyValueRow(label = "REFERENCE #", value = sale.paymentReference)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Simulated Realistic Barcode
            ReceiptBarcode(
                code = sale.receiptNumber,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(44.dp)
            )

            Text(
                text = sale.receiptNumber,
                color = inkSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Footer Message
            if (settings.receiptFooter.isNotBlank()) {
                ReceiptDashedDivider()
                Spacer(modifier = Modifier.height(10.dp))
                settings.receiptFooter.lines().forEach { line ->
                    if (line.isNotBlank()) {
                        Text(
                            text = line.trim(),
                            color = inkSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Serrated / Perforated bottom tear line
            ReceiptPerforationLine(color = Color(0xFFCBD5E1))
        }
    }
}

/**
 * Bottom action bar with primary 'Share/Print' button and instant access buttons.
 */
@Composable
private fun ReceiptPreviewBottomBar(
    onSharePrint: () -> Unit,
    onDirectPrint: () -> Unit,
    onDirectSharePdf: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Direct Quick Print Outlined Button
            OutlinedButton(
                onClick = onDirectPrint,
                modifier = Modifier
                    .weight(0.9f)
                    .testTag("quick_print_button"),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Print,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Print",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Primary Consolidated 'Share/Print' Button as requested
            Button(
                onClick = onSharePrint,
                modifier = Modifier
                    .weight(1.3f)
                    .testTag("share_print_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Share / Print",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Action Sheet with printing and sharing options (PDF, Print Service, Plain Text).
 */
@Composable
private fun SharePrintActionSheetContent(
    context: Context,
    currentSale: SaleWithItems,
    settings: AppSettings,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("share_print_sheet")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Share or Print Receipt",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Receipt ${currentSale.sale.receiptNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Option 1: Print Document via Android Print Framework
        ActionSheetItem(
            icon = Icons.Default.Print,
            title = "Print Receipt",
            subtitle = "Send to connected thermal or office printer via Android Print Service",
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
            onClick = {
                onDismiss()
                ReceiptHelper.printReceipt(context, currentSale, settings)
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Option 2: Share formatted PDF Receipt
        ActionSheetItem(
            icon = Icons.Default.PictureAsPdf,
            title = "Share PDF Receipt",
            subtitle = "Generate official 80mm PDF document for WhatsApp, Email or Google Drive",
            containerColor = Color(0xFFDC2626).copy(alpha = 0.12f),
            iconTint = Color(0xFFDC2626),
            onClick = {
                onDismiss()
                ReceiptHelper.shareReceipt(context, currentSale, settings)
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Option 3: Share Plain Text Summary
        ActionSheetItem(
            icon = Icons.Default.Description,
            title = "Share Plain Text Summary",
            subtitle = "Compact text format formatted with ASCII dividers for instant SMS",
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
            onClick = {
                onDismiss()
                val text = ReceiptHelper.generatePlainTextReceipt(currentSale, settings)
                val intent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    putExtra(android.content.Intent.EXTRA_TEXT, text)
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Receipt ${currentSale.sale.receiptNumber}")
                    type = "text/plain"
                }
                val chooser = android.content.Intent.createChooser(intent, "Share Plain Text Receipt")
                chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            }
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ActionSheetItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    containerColor: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = containerColor,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ReceiptKeyValueRow(
    label: String,
    value: String,
    valueColor: Color = Color(0xFF0F172A),
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color(0xFF64748B),
            fontSize = 11.5.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun ReceiptDashedDivider() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        drawLine(
            color = Color(0xFFCBD5E1),
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f),
            strokeWidth = 1.2f
        )
    }
}

@Composable
private fun ReceiptSolidLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFFE2E8F0))
    )
}

@Composable
private fun ReceiptPerforationLine(color: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
    ) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f),
            strokeWidth = 2f
        )
    }
}

/**
 * Visual barcode canvas that renders stylized vertical bars representing the receipt code.
 */
@Composable
private fun ReceiptBarcode(
    code: String,
    modifier: Modifier = Modifier
) {
    val barColor = Color(0xFF0F172A)
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Deterministic pseudo-barcode based on code string characters
        val numBars = 50
        val baseStep = width / numBars

        var currentX = 0f
        val seed = code.hashCode().toLong()

        for (i in 0 until numBars) {
            val isSpace = ((seed ushr (i % 31)) and 1L) == 1L && (i % 3 == 0)
            val barWidth = when {
                i % 7 == 0 -> baseStep * 1.8f
                i % 4 == 0 -> baseStep * 1.3f
                else -> baseStep * 0.8f
            }

            if (!isSpace && currentX + barWidth <= width) {
                drawRect(
                    color = barColor,
                    topLeft = Offset(currentX, 0f),
                    size = androidx.compose.ui.geometry.Size(barWidth, height)
                )
            }
            currentX += barWidth + (baseStep * 0.2f)
            if (currentX >= width) break
        }
    }
}
