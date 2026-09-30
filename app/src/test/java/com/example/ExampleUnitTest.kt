package com.example

import com.example.data.entity.AppSettings
import com.example.data.entity.Sale
import com.example.data.entity.SaleItem
import com.example.data.entity.SaleWithItems
import com.example.util.CurrencyFormatter
import com.example.util.ReceiptHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testCurrencyFormatting() {
        assertEquals("GHS 10.50", CurrencyFormatter.formatPesewas(1050L, "GHS"))
        assertEquals("GHS 1,250.00", CurrencyFormatter.formatPesewas(125000L, "GHS"))
        assertEquals("GHS 0.00", CurrencyFormatter.formatPesewas(0L, "GHS"))

        assertEquals(1050L, CurrencyFormatter.toPesewas("10.50"))
        assertEquals(1050L, CurrencyFormatter.toPesewas("10,50"))
        assertEquals(700L, CurrencyFormatter.toPesewas("7.00"))
        assertEquals(700L, CurrencyFormatter.toPesewas("7"))
    }

    @Test
    fun testReceiptGeneration() {
        val sale = Sale(
            id = 1L,
            receiptNumber = "POS-20260930-0001",
            subtotalPesewas = 2200L,
            discountPesewas = 0L,
            totalPesewas = 2200L,
            paymentMethod = "Cash",
            amountReceivedPesewas = 2500L,
            changeAmountPesewas = 300L,
            status = "COMPLETED",
            cashierName = "Admin",
            createdAt = 1775039700000L
        )

        val items = listOf(
            SaleItem(
                id = 1L,
                saleId = 1L,
                productId = 10L,
                productName = "Coca Cola 500ml",
                quantity = 2,
                unitPricePesewas = 700L,
                subtotalPesewas = 1400L
            ),
            SaleItem(
                id = 2L,
                saleId = 1L,
                productId = 11L,
                productName = "Bread",
                quantity = 1,
                unitPricePesewas = 800L,
                subtotalPesewas = 800L
            )
        )

        val settings = AppSettings(
            shopName = "MY SHOP",
            shopAddress = "Accra, Ghana",
            phone = "050 000 0000",
            currency = "GHS"
        )

        val receiptText = ReceiptHelper.generatePlainTextReceipt(SaleWithItems(sale, items), settings)

        assertTrue(receiptText.contains("MY SHOP"))
        assertTrue(receiptText.contains("POS-20260930-0001"))
        assertTrue(receiptText.contains("Coca Cola 500ml"))
        assertTrue(receiptText.contains("GHS 22.00"))
        assertTrue(receiptText.contains("Change"))
        assertTrue(receiptText.contains("GHS 3.00"))
    }
}
