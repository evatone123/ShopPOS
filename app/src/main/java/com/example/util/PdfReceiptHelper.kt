package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.entity.AppSettings
import com.example.data.entity.SaleWithItems
import com.example.data.repository.StockReceivingResult
import com.example.ui.reports.ReportsUiState
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReceiptHelper {

    /**
     * Generates a PDF receipt file for a single transaction/sale.
     * Page dimensions: Standard 58/80mm receipt format (300 x dynamic height pt, min 600 pt).
     */
    fun generateSaleReceiptPdf(
        context: Context,
        saleWithItems: SaleWithItems,
        settings: AppSettings
    ): File {
        val sale = saleWithItems.sale
        val items = saleWithItems.items
        val currency = settings.currency
        val dateFormat = SimpleDateFormat("dd MMM yyyy  hh:mm a", Locale.US)
        val formattedDate = dateFormat.format(Date(sale.createdAt))

        // Estimate height needed: header (120) + items (items.size * 32) + totals (120) + footer (80)
        val pageWidth = 300
        val estimatedHeight = (320 + items.size * 34 + (settings.receiptFooter.lines().size * 16)).coerceAtLeast(540)
        val pageHeight = estimatedHeight

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        // Background
        canvas.drawColor(Color.WHITE)

        // Paints
        val regularPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 10f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            isAntiAlias = true
        }

        val boldPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 10.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
        }

        val titlePaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 14f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val centerPaint = Paint().apply {
            color = Color.rgb(71, 85, 105)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val linePaint = Paint().apply {
            color = Color.rgb(148, 163, 184)
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        val dashedLinePaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        var y = 28f
        val centerX = pageWidth / 2f
        val leftMargin = 16f
        val rightMargin = pageWidth - 16f
        val contentWidth = rightMargin - leftMargin

        // Header: Store info
        canvas.drawText(settings.shopName.uppercase(), centerX, y, titlePaint)
        y += 15f

        if (settings.shopAddress.isNotBlank()) {
            canvas.drawText(settings.shopAddress, centerX, y, centerPaint)
            y += 13f
        }
        if (settings.phone.isNotBlank()) {
            canvas.drawText("Tel: ${settings.phone}", centerX, y, centerPaint)
            y += 13f
        }

        // Status banner if cancelled
        if (sale.status == "CANCELLED") {
            y += 4f
            val bannerPaint = Paint().apply {
                color = Color.rgb(254, 226, 226)
                style = Paint.Style.FILL
            }
            val bannerBorderPaint = Paint().apply {
                color = Color.rgb(220, 38, 38)
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            val bannerTextPaint = Paint().apply {
                color = Color.rgb(185, 28, 28)
                textSize = 10f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawRect(leftMargin, y, rightMargin, y + 26f, bannerPaint)
            canvas.drawRect(leftMargin, y, rightMargin, y + 26f, bannerBorderPaint)
            canvas.drawText("*** VOIDED / CANCELLED ***", centerX, y + 16f, bannerTextPaint)
            y += 34f
        }

        // Top double divider
        y += 6f
        canvas.drawLine(leftMargin, y, rightMargin, y, linePaint)
        y += 2f
        canvas.drawLine(leftMargin, y, rightMargin, y, linePaint)
        y += 14f

        // Receipt metadata
        drawRow(canvas, "Receipt #:", sale.receiptNumber, leftMargin, rightMargin, y, boldPaint, boldPaint)
        y += 13f
        drawRow(canvas, "Date:", formattedDate, leftMargin, rightMargin, y, regularPaint, regularPaint)
        y += 13f
        drawRow(canvas, "Cashier:", sale.cashierName, leftMargin, rightMargin, y, regularPaint, regularPaint)
        y += 14f

        // Items Header
        canvas.drawLine(leftMargin, y, rightMargin, y, dashedLinePaint)
        y += 13f
        drawRow(canvas, "ITEM / DETAILS", "TOTAL", leftMargin, rightMargin, y, boldPaint, boldPaint)
        y += 5f
        canvas.drawLine(leftMargin, y, rightMargin, y, dashedLinePaint)
        y += 14f

        // Line Items
        for (item in items) {
            val name = if (item.productName.length > 24) item.productName.take(22) + ".." else item.productName
            val lineTotal = CurrencyFormatter.formatPesewas(item.subtotalPesewas, currency)
            drawRow(canvas, name, lineTotal, leftMargin, rightMargin, y, boldPaint, boldPaint)
            y += 12f

            val qtyUnit = "${item.quantity} x ${CurrencyFormatter.formatPesewas(item.unitPricePesewas, currency)}"
            val detailPaint = Paint(regularPaint).apply {
                color = Color.rgb(100, 116, 139)
                textSize = 9f
            }
            canvas.drawText("  $qtyUnit", leftMargin, y, detailPaint)
            y += 14f
        }

        // Totals separator
        canvas.drawLine(leftMargin, y, rightMargin, y, dashedLinePaint)
        y += 14f

        // Totals
        val subtotalStr = CurrencyFormatter.formatPesewas(sale.subtotalPesewas, currency)
        drawRow(canvas, "Subtotal:", subtotalStr, leftMargin, rightMargin, y, regularPaint, regularPaint)
        y += 13f

        if (sale.discountPesewas > 0) {
            val discStr = "-${CurrencyFormatter.formatPesewas(sale.discountPesewas, currency)}"
            drawRow(canvas, "Discount:", discStr, leftMargin, rightMargin, y, regularPaint, regularPaint)
            y += 13f
        }

        // Grand Total Box / highlight
        y += 2f
        canvas.drawLine(leftMargin, y, rightMargin, y, linePaint)
        y += 15f
        val grandTotalPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 12f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
        }
        val totalStr = CurrencyFormatter.formatPesewas(sale.totalPesewas, currency)
        drawRow(canvas, "TOTAL DUE:", totalStr, leftMargin, rightMargin, y, grandTotalPaint, grandTotalPaint)
        y += 6f
        canvas.drawLine(leftMargin, y, rightMargin, y, linePaint)
        y += 15f

        // Payment Info
        drawRow(canvas, "Payment Method:", sale.paymentMethod, leftMargin, rightMargin, y, regularPaint, boldPaint)
        y += 13f
        drawRow(canvas, "Amount Paid:", CurrencyFormatter.formatPesewas(sale.amountReceivedPesewas, currency), leftMargin, rightMargin, y, regularPaint, regularPaint)
        y += 13f

        if (sale.changeAmountPesewas > 0) {
            drawRow(canvas, "Change Given:", CurrencyFormatter.formatPesewas(sale.changeAmountPesewas, currency), leftMargin, rightMargin, y, regularPaint, regularPaint)
            y += 13f
        }

        if (!sale.paymentReference.isNullOrBlank()) {
            drawRow(canvas, "Ref / Notes:", sale.paymentReference, leftMargin, rightMargin, y, regularPaint, regularPaint)
            y += 13f
        }

        // Footer lines
        y += 8f
        canvas.drawLine(leftMargin, y, rightMargin, y, dashedLinePaint)
        y += 16f

        for (line in settings.receiptFooter.lines()) {
            val trimmed = line.trim()
            if (trimmed.isNotBlank()) {
                canvas.drawText(trimmed, centerX, y, centerPaint)
                y += 13f
            }
        }

        document.finishPage(page)

        // Save PDF to cache directory
        val receiptsDir = File(context.cacheDir, "receipts").apply { mkdirs() }
        val sanitizedReceipt = sale.receiptNumber.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val pdfFile = File(receiptsDir, "Receipt_${sanitizedReceipt}.pdf")

        FileOutputStream(pdfFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        return pdfFile
    }

    /**
     * Generates a comprehensive Transaction Summary Report PDF for a reporting period.
     * Page dimensions: Standard A4 (595 x 842 pt).
     */
    fun generateTransactionSummaryPdf(
        context: Context,
        reportsUiState: ReportsUiState
    ): File {
        val settings = reportsUiState.settings
        val currency = settings.currency
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
        val generatedDate = dateFormat.format(Date())

        val pageWidth = 595
        val pageHeight = 842

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        canvas.drawColor(Color.WHITE)

        val headerBgPaint = Paint().apply {
            color = Color.rgb(241, 245, 249) // Slate 100
            style = Paint.Style.FILL
        }

        val primaryBarPaint = Paint().apply {
            color = Color.rgb(79, 70, 229) // Indigo 600
            style = Paint.Style.FILL
        }

        val titlePaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val sectionTitlePaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            color = Color.rgb(51, 65, 85)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val bodyBoldPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val cardBgPaint = Paint().apply {
            color = Color.rgb(248, 250, 252)
            style = Paint.Style.FILL
        }

        val cardBorderPaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        val leftMargin = 36f
        val rightMargin = pageWidth - 36f
        var y = 36f

        // Top accent bar
        canvas.drawRect(leftMargin, y, rightMargin, y + 4f, primaryBarPaint)
        y += 18f

        // Document Header
        canvas.drawText(settings.shopName, leftMargin, y, titlePaint)
        y += 14f
        canvas.drawText("Transaction Summary & Sales Report — Period: ${reportsUiState.period.label}", leftMargin, y, subtitlePaint)
        y += 12f
        canvas.drawText("Generated on $generatedDate • Currency: $currency", leftMargin, y, subtitlePaint)
        y += 20f

        // Divider
        canvas.drawLine(leftMargin, y, rightMargin, y, cardBorderPaint)
        y += 16f

        // Key Metrics Summary Cards (2x2 grid)
        canvas.drawText("KEY PERFORMANCE METRICS", leftMargin, y, sectionTitlePaint)
        y += 12f

        val cardWidth = (rightMargin - leftMargin - 16f) / 2f
        val cardHeight = 52f

        // Card 1: Total Sales
        drawMetricCard(canvas, leftMargin, y, cardWidth, cardHeight, "Total Revenue", CurrencyFormatter.formatPesewas(reportsUiState.totalSalesPesewas, currency), cardBgPaint, cardBorderPaint, bodyPaint, titlePaint)
        // Card 2: Transactions
        drawMetricCard(canvas, leftMargin + cardWidth + 16f, y, cardWidth, cardHeight, "Transactions Count", "${reportsUiState.transactionsCount} orders", cardBgPaint, cardBorderPaint, bodyPaint, titlePaint)
        y += cardHeight + 10f

        // Card 3: Items Sold
        drawMetricCard(canvas, leftMargin, y, cardWidth, cardHeight, "Items Sold", "${reportsUiState.itemsSoldCount} units", cardBgPaint, cardBorderPaint, bodyPaint, titlePaint)
        // Card 4: Average Ticket
        drawMetricCard(canvas, leftMargin + cardWidth + 16f, y, cardWidth, cardHeight, "Average Transaction", CurrencyFormatter.formatPesewas(reportsUiState.averageTransactionPesewas, currency), cardBgPaint, cardBorderPaint, bodyPaint, titlePaint)
        y += cardHeight + 24f

        // Payment Method Breakdown
        canvas.drawText("PAYMENT METHOD BREAKDOWN", leftMargin, y, sectionTitlePaint)
        y += 12f

        // Table Header
        canvas.drawRect(leftMargin, y, rightMargin, y + 20f, headerBgPaint)
        canvas.drawText("Method", leftMargin + 8f, y + 14f, bodyBoldPaint)
        canvas.drawText("Transactions", leftMargin + 200f, y + 14f, bodyBoldPaint)
        canvas.drawText("Percentage", leftMargin + 320f, y + 14f, bodyBoldPaint)
        val rightAlignPaint = Paint(bodyBoldPaint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("Total Revenue", rightMargin - 8f, y + 14f, rightAlignPaint)
        y += 24f

        val rightBodyPaint = Paint(bodyPaint).apply { textAlign = Paint.Align.RIGHT }
        if (reportsUiState.paymentBreakdowns.isEmpty()) {
            canvas.drawText("No transactions recorded for this period.", leftMargin + 8f, y + 12f, bodyPaint)
            y += 24f
        } else {
            for (p in reportsUiState.paymentBreakdowns) {
                canvas.drawText(p.method, leftMargin + 8f, y + 12f, bodyPaint)
                canvas.drawText("${p.count} txns", leftMargin + 200f, y + 12f, bodyPaint)
                canvas.drawText(String.format(Locale.US, "%.1f%%", p.percentage), leftMargin + 320f, y + 12f, bodyPaint)
                canvas.drawText(CurrencyFormatter.formatPesewas(p.totalPesewas, currency), rightMargin - 8f, y + 12f, rightBodyPaint)
                y += 18f
                canvas.drawLine(leftMargin, y, rightMargin, y, cardBorderPaint)
                y += 4f
            }
        }
        y += 16f

        // Top Selling Products Section
        canvas.drawText("TOP SELLING PRODUCTS (RANKING)", leftMargin, y, sectionTitlePaint)
        y += 12f

        canvas.drawRect(leftMargin, y, rightMargin, y + 20f, headerBgPaint)
        canvas.drawText("#", leftMargin + 8f, y + 14f, bodyBoldPaint)
        canvas.drawText("Product Name", leftMargin + 36f, y + 14f, bodyBoldPaint)
        canvas.drawText("Qty Sold", leftMargin + 320f, y + 14f, bodyBoldPaint)
        canvas.drawText("Revenue", rightMargin - 8f, y + 14f, rightAlignPaint)
        y += 24f

        if (reportsUiState.bestSellers.isEmpty()) {
            canvas.drawText("No product sales data available for this period.", leftMargin + 8f, y + 12f, bodyPaint)
            y += 24f
        } else {
            reportsUiState.bestSellers.take(8).forEachIndexed { index, item ->
                canvas.drawText("${index + 1}", leftMargin + 8f, y + 12f, bodyBoldPaint)
                val prodName = if (item.productName.length > 38) item.productName.take(35) + "..." else item.productName
                canvas.drawText(prodName, leftMargin + 36f, y + 12f, bodyPaint)
                canvas.drawText("${item.totalQuantitySold}", leftMargin + 320f, y + 12f, bodyPaint)
                canvas.drawText(CurrencyFormatter.formatPesewas(item.totalRevenuePesewas, currency), rightMargin - 8f, y + 12f, rightBodyPaint)
                y += 18f
                canvas.drawLine(leftMargin, y, rightMargin, y, cardBorderPaint)
                y += 4f
            }
        }
        y += 16f

        // Inventory Stock Valuation
        canvas.drawText("CURRENT INVENTORY VALUATION", leftMargin, y, sectionTitlePaint)
        y += 12f
        drawMetricCard(canvas, leftMargin, y, cardWidth, cardHeight, "Stock at Cost Value", CurrencyFormatter.formatPesewas(reportsUiState.totalInventoryStockValueCostPesewas, currency), cardBgPaint, cardBorderPaint, bodyPaint, titlePaint)
        drawMetricCard(canvas, leftMargin + cardWidth + 16f, y, cardWidth, cardHeight, "Stock at Retail Value", CurrencyFormatter.formatPesewas(reportsUiState.totalInventoryStockValueRetailPesewas, currency), cardBgPaint, cardBorderPaint, bodyPaint, titlePaint)
        y += cardHeight + 24f

        // Footer note
        val footerPaint = Paint().apply {
            color = Color.rgb(148, 163, 184)
            textSize = 9f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("Report generated automatically by ${settings.shopName} • Point of Sale & Inventory System", pageWidth / 2f, pageHeight - 24f, footerPaint)

        document.finishPage(page)

        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val periodTag = reportsUiState.period.name.lowercase(Locale.US)
        val pdfFile = File(reportsDir, "Transaction_Summary_${periodTag}.pdf")

        FileOutputStream(pdfFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        return pdfFile
    }

    /**
     * Generates a PDF Goods Received Note / Stock Delivery Receipt.
     */
    fun generateStockReceivingPdf(
        context: Context,
        result: StockReceivingResult,
        settings: AppSettings
    ): File {
        val currency = settings.currency
        val dateFormat = SimpleDateFormat("dd MMM yyyy  hh:mm a", Locale.US)
        val formattedDate = dateFormat.format(Date(result.timestamp))

        val pageWidth = 340
        val estimatedHeight = (360 + result.items.size * 38).coerceAtLeast(540)
        val pageHeight = estimatedHeight

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        canvas.drawColor(Color.WHITE)

        val boldPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 10f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
        }

        val regularPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            isAntiAlias = true
        }

        val titlePaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 14f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val centerPaint = Paint().apply {
            color = Color.rgb(71, 85, 105)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val linePaint = Paint().apply {
            color = Color.rgb(148, 163, 184)
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        var y = 28f
        val centerX = pageWidth / 2f
        val leftMargin = 16f
        val rightMargin = pageWidth - 16f

        canvas.drawText(settings.shopName.uppercase(), centerX, y, titlePaint)
        y += 15f
        if (settings.shopAddress.isNotBlank()) {
            canvas.drawText(settings.shopAddress, centerX, y, centerPaint)
            y += 13f
        }
        if (settings.phone.isNotBlank()) {
            canvas.drawText("Tel: ${settings.phone}", centerX, y, centerPaint)
            y += 15f
        }

        canvas.drawLine(leftMargin, y, rightMargin, y, linePaint)
        y += 16f

        val docTitlePaint = Paint(titlePaint).apply {
            textSize = 12f
            color = Color.rgb(5, 150, 105)
        }
        canvas.drawText("STOCK RECEIVING NOTE", centerX, y, docTitlePaint)
        y += 16f

        drawRow(canvas, "Reference:", result.referenceNumber, leftMargin, rightMargin, y, regularPaint, boldPaint)
        y += 13f
        drawRow(canvas, "Date & Time:", formattedDate, leftMargin, rightMargin, y, regularPaint, regularPaint)
        y += 13f
        if (result.supplierName.isNotBlank()) {
            drawRow(canvas, "Supplier:", result.supplierName, leftMargin, rightMargin, y, regularPaint, boldPaint)
            y += 13f
        }
        if (result.notes.isNotBlank()) {
            drawRow(canvas, "Notes:", result.notes, leftMargin, rightMargin, y, regularPaint, regularPaint)
            y += 13f
        }

        y += 4f
        canvas.drawLine(leftMargin, y, rightMargin, y, linePaint)
        y += 14f

        canvas.drawText("PRODUCT / SKU", leftMargin, y, boldPaint)
        val rightAlignBold = Paint(boldPaint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("QTY / COST", rightMargin, y, rightAlignBold)
        y += 8f
        canvas.drawLine(leftMargin, y, rightMargin, y, linePaint)
        y += 14f

        for (item in result.items) {
            val nameText = if (item.product.name.length > 22) item.product.name.take(20) + ".." else item.product.name
            canvas.drawText(nameText, leftMargin, y, boldPaint)
            val subtotalStr = CurrencyFormatter.formatPesewas(item.subtotalCostPesewas, currency)
            val rightAlignReg = Paint(regularPaint).apply { textAlign = Paint.Align.RIGHT }
            canvas.drawText(subtotalStr, rightMargin, y, rightAlignReg)
            y += 12f

            val detailText = "Stock: ${item.previousStock} → ${item.newStockQuantity} (${item.product.unit})"
            canvas.drawText(detailText, leftMargin, y, regularPaint)

            val unitCostFormatted = CurrencyFormatter.formatPesewas(item.unitCostPesewas, currency)
            val qtyText = "+${item.quantityToAdd} @ $unitCostFormatted"
            canvas.drawText(qtyText, rightMargin, y, rightAlignReg)
            y += 15f
        }

        canvas.drawLine(leftMargin, y, rightMargin, y, linePaint)
        y += 14f

        drawRow(canvas, "Distinct Products:", "${result.items.size}", leftMargin, rightMargin, y, regularPaint, regularPaint)
        y += 13f
        drawRow(canvas, "Total Units Received:", "${result.totalUnitsReceived}", leftMargin, rightMargin, y, boldPaint, boldPaint)
        y += 14f
        val totalCostFormatted = CurrencyFormatter.formatPesewas(result.totalCostPesewas, currency)
        val largeBold = Paint(boldPaint).apply { textSize = 11.5f }
        drawRow(canvas, "TOTAL RECEIVING COST:", totalCostFormatted, leftMargin, rightMargin, y, largeBold, largeBold)
        y += 20f

        canvas.drawLine(leftMargin, y, rightMargin, y, linePaint)
        y += 16f
        canvas.drawText("RECEIVED & VERIFIED INTO INVENTORY", centerX, y, centerPaint)
        y += 12f
        canvas.drawText("ShopPOS Inventory Management", centerX, y, centerPaint)

        document.finishPage(page)

        val receiptsDir = File(context.cacheDir, "receiving").apply { mkdirs() }
        val pdfFile = File(receiptsDir, "Receiving_${result.referenceNumber}.pdf")

        FileOutputStream(pdfFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        return pdfFile
    }

    /**
     * Shares any generated PDF file via the standard Android ACTION_SEND Intent with FileProvider.
     */
    fun sharePdfFile(context: Context, pdfFile: File, title: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "$title - PDF Receipt attached.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Share or Print PDF Receipt")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    private fun drawRow(
        canvas: android.graphics.Canvas,
        leftText: String,
        rightText: String,
        leftX: Float,
        rightX: Float,
        y: Float,
        leftPaint: Paint,
        rightPaint: Paint
    ) {
        canvas.drawText(leftText, leftX, y, leftPaint)
        val alignRightPaint = Paint(rightPaint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText(rightText, rightX, y, alignRightPaint)
    }

    private fun drawMetricCard(
        canvas: android.graphics.Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        title: String,
        value: String,
        bgPaint: Paint,
        borderPaint: Paint,
        titlePaint: Paint,
        valuePaint: Paint
    ) {
        canvas.drawRoundRect(x, y, x + width, y + height, 8f, 8f, bgPaint)
        canvas.drawRoundRect(x, y, x + width, y + height, 8f, 8f, borderPaint)
        canvas.drawText(title, x + 12f, y + 18f, titlePaint)
        val metricPaint = Paint(valuePaint).apply {
            textSize = 13f
        }
        canvas.drawText(value, x + 12f, y + 38f, metricPaint)
    }
}
