package com.example.util

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.entity.AppSettings
import com.example.data.entity.SaleWithItems
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReceiptHelper {

    fun generatePlainTextReceipt(saleWithItems: SaleWithItems, settings: AppSettings): String {
        val sale = saleWithItems.sale
        val items = saleWithItems.items
        val currency = settings.currency
        val dateFormat = SimpleDateFormat("dd MMM yyyy  hh:mm a", Locale.US)
        val formattedDate = dateFormat.format(Date(sale.createdAt))

        val sb = StringBuilder()
        sb.appendLine("================================")
        sb.appendLine(centerText(settings.shopName, 32))
        sb.appendLine(centerText(settings.shopAddress, 32))
        sb.appendLine(centerText(settings.phone, 32))
        sb.appendLine("================================")
        sb.appendLine()
        sb.appendLine("Receipt: ${sale.receiptNumber}")
        sb.appendLine("Date:    $formattedDate")
        if (sale.status == "CANCELLED") {
            sb.appendLine("STATUS:  *** VOIDED / CANCELLED ***")
            if (!sale.cancelReason.isNullOrBlank()) {
                sb.appendLine("Reason:  ${sale.cancelReason}")
            }
        }
        sb.appendLine("--------------------------------")
        sb.appendLine(String.format(Locale.US, "%-16s %4s %10s", "Item", "Qty", "Amount"))
        sb.appendLine("--------------------------------")

        for (item in items) {
            val nameLine = if (item.productName.length > 30) item.productName.take(28) + ".." else item.productName
            sb.appendLine(nameLine)
            val sub = CurrencyFormatter.formatPesewas(item.subtotalPesewas, currency)
            val detail = "${item.quantity} x ${CurrencyFormatter.formatPesewas(item.unitPricePesewas, currency)}"
            sb.appendLine(String.format(Locale.US, "  %-18s %11s", detail, sub))
        }

        sb.appendLine("--------------------------------")
        sb.appendLine(formatRow("Subtotal", CurrencyFormatter.formatPesewas(sale.subtotalPesewas, currency)))
        if (sale.discountPesewas > 0) {
            sb.appendLine(formatRow("Discount", "-${CurrencyFormatter.formatPesewas(sale.discountPesewas, currency)}"))
        }
        sb.appendLine(formatRow("TOTAL", CurrencyFormatter.formatPesewas(sale.totalPesewas, currency)))
        sb.appendLine("--------------------------------")
        sb.appendLine(formatRow("Payment", sale.paymentMethod))
        sb.appendLine(formatRow("Received", CurrencyFormatter.formatPesewas(sale.amountReceivedPesewas, currency)))
        if (sale.changeAmountPesewas > 0) {
            sb.appendLine(formatRow("Change", CurrencyFormatter.formatPesewas(sale.changeAmountPesewas, currency)))
        }
        if (!sale.paymentReference.isNullOrBlank()) {
            sb.appendLine(formatRow("Ref #", sale.paymentReference))
        }
        sb.appendLine("--------------------------------")
        for (line in settings.receiptFooter.lines()) {
            sb.appendLine(centerText(line.trim(), 32))
        }
        sb.appendLine("================================")
        return sb.toString()
    }

    fun generateHtmlReceipt(saleWithItems: SaleWithItems, settings: AppSettings): String {
        val sale = saleWithItems.sale
        val items = saleWithItems.items
        val currency = settings.currency
        val dateFormat = SimpleDateFormat("dd MMM yyyy  hh:mm a", Locale.US)
        val formattedDate = dateFormat.format(Date(sale.createdAt))

        val itemsHtml = StringBuilder()
        for (item in items) {
            itemsHtml.append("""
                <tr>
                    <td style="padding: 4px 0; text-align: left;">
                        <div style="font-weight: 600;">${escapeHtml(item.productName)}</div>
                        <div style="font-size: 11px; color: #555;">${item.quantity} &times; ${CurrencyFormatter.formatPesewas(item.unitPricePesewas, currency)}</div>
                    </td>
                    <td style="padding: 4px 0; text-align: right; vertical-align: top; font-weight: 600;">
                        ${CurrencyFormatter.formatPesewas(item.subtotalPesewas, currency)}
                    </td>
                </tr>
            """.trimIndent())
        }

        val cancelledBanner = if (sale.status == "CANCELLED") {
            """<div style="background: #fee2e2; color: #b91c1c; border: 1px dashed #b91c1c; padding: 8px; margin: 8px 0; text-align: center; font-weight: bold; border-radius: 4px;">*** CANCELLED / VOIDED ***<br/><span style="font-size: 11px;">Reason: ${escapeHtml(sale.cancelReason ?: "N/A")}</span></div>"""
        } else ""

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <style>
                    body {
                        font-family: 'Courier New', Courier, monospace, sans-serif;
                        font-size: 12px;
                        color: #111;
                        margin: 0;
                        padding: 16px;
                        max-width: 320px;
                        margin: 0 auto;
                    }
                    .center { text-align: center; }
                    .header h2 { margin: 0 0 4px 0; font-size: 18px; text-transform: uppercase; }
                    .header p { margin: 2px 0; font-size: 12px; color: #333; }
                    .divider { border-top: 1px dashed #444; margin: 8px 0; }
                    .double-divider { border-top: 2px solid #222; margin: 8px 0; }
                    table { width: 100%; border-collapse: collapse; }
                    .totals td { padding: 3px 0; }
                    .total-due { font-size: 15px; font-weight: bold; }
                    .footer { text-align: center; margin-top: 14px; font-size: 11px; color: #444; }
                </style>
            </head>
            <body>
                <div class="header center">
                    <h2>${escapeHtml(settings.shopName)}</h2>
                    <p>${escapeHtml(settings.shopAddress)}</p>
                    <p>Tel: ${escapeHtml(settings.phone)}</p>
                </div>
                
                $cancelledBanner

                <div class="divider"></div>
                <table>
                    <tr><td><strong>Receipt:</strong></td><td style="text-align: right;">${sale.receiptNumber}</td></tr>
                    <tr><td><strong>Date:</strong></td><td style="text-align: right;">$formattedDate</td></tr>
                    <tr><td><strong>Cashier:</strong></td><td style="text-align: right;">${escapeHtml(sale.cashierName)}</td></tr>
                </table>

                <div class="divider"></div>
                <table>
                    <thead>
                        <tr style="border-bottom: 1px dashed #666;">
                            <th style="text-align: left; padding-bottom: 4px;">Item</th>
                            <th style="text-align: right; padding-bottom: 4px;">Total</th>
                        </tr>
                    </thead>
                    <tbody>
                        $itemsHtml
                    </tbody>
                </table>

                <div class="divider"></div>
                <table class="totals">
                    <tr>
                        <td>Subtotal:</td>
                        <td style="text-align: right;">${CurrencyFormatter.formatPesewas(sale.subtotalPesewas, currency)}</td>
                    </tr>
                    ${if (sale.discountPesewas > 0) """
                    <tr>
                        <td>Discount:</td>
                        <td style="text-align: right;">-${CurrencyFormatter.formatPesewas(sale.discountPesewas, currency)}</td>
                    </tr>""" else ""}
                    <tr class="total-due">
                        <td>TOTAL:</td>
                        <td style="text-align: right;">${CurrencyFormatter.formatPesewas(sale.totalPesewas, currency)}</td>
                    </tr>
                </table>

                <div class="divider"></div>
                <table class="totals">
                    <tr><td>Payment Method:</td><td style="text-align: right;">${sale.paymentMethod}</td></tr>
                    <tr><td>Amount Paid:</td><td style="text-align: right;">${CurrencyFormatter.formatPesewas(sale.amountReceivedPesewas, currency)}</td></tr>
                    ${if (sale.changeAmountPesewas > 0) """
                    <tr><td>Change Given:</td><td style="text-align: right;">${CurrencyFormatter.formatPesewas(sale.changeAmountPesewas, currency)}</td></tr>""" else ""}
                    ${if (!sale.paymentReference.isNullOrBlank()) """
                    <tr><td>Ref #:</td><td style="text-align: right;">${escapeHtml(sale.paymentReference)}</td></tr>""" else ""}
                </table>

                <div class="double-divider"></div>
                <div class="footer">
                    ${settings.receiptFooter.replace("\n", "<br/>")}
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    fun printReceipt(context: Context, saleWithItems: SaleWithItems, settings: AppSettings) {
        val webView = WebView(context)
        val htmlContent = generateHtmlReceipt(saleWithItems, settings)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                if (printManager != null && view != null) {
                    val printAdapter = view.createPrintDocumentAdapter("Receipt_${saleWithItems.sale.receiptNumber}")
                    val printAttributes = PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A6)
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build()
                    printManager.print(
                        "Receipt ${saleWithItems.sale.receiptNumber}",
                        printAdapter,
                        printAttributes
                    )
                }
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    fun shareReceipt(context: Context, saleWithItems: SaleWithItems, settings: AppSettings) {
        try {
            val pdfFile = PdfReceiptHelper.generateSaleReceiptPdf(context, saleWithItems, settings)
            PdfReceiptHelper.sharePdfFile(context, pdfFile, "Receipt ${saleWithItems.sale.receiptNumber}")
        } catch (e: Exception) {
            // Fallback to plain text sharing if PDF generation encounters any error
            val text = generatePlainTextReceipt(saleWithItems, settings)
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                putExtra(Intent.EXTRA_SUBJECT, "Receipt ${saleWithItems.sale.receiptNumber}")
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Share Receipt")
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(shareIntent)
        }
    }

    fun sharePdfReceipt(context: Context, saleWithItems: SaleWithItems, settings: AppSettings) {
        val pdfFile = PdfReceiptHelper.generateSaleReceiptPdf(context, saleWithItems, settings)
        PdfReceiptHelper.sharePdfFile(context, pdfFile, "Receipt ${saleWithItems.sale.receiptNumber}")
    }

    private fun centerText(text: String, width: Int): String {
        if (text.length >= width) return text
        val leftPadding = (width - text.length) / 2
        return " ".repeat(leftPadding) + text
    }

    private fun formatRow(label: String, value: String, width: Int = 32): String {
        val spaces = width - label.length - value.length
        return if (spaces > 0) label + " ".repeat(spaces) + value else "$label $value"
    }

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}
