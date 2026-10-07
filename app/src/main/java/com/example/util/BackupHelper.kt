package com.example.util

import android.content.Context
import android.net.Uri
import com.example.data.entity.AppSettings
import com.example.data.entity.Category
import com.example.data.entity.InventoryMovement
import com.example.data.entity.Payment
import com.example.data.entity.Product
import com.example.data.entity.Sale
import com.example.data.entity.SaleItem
import com.example.data.repository.PosRepository
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

data class BackupData(
    val categories: List<Category>,
    val products: List<Product>,
    val sales: List<Sale>,
    val saleItems: List<SaleItem>,
    val movements: List<InventoryMovement>,
    val payments: List<Payment>,
    val settings: AppSettings?
)

object BackupHelper {

    suspend fun exportToJson(repository: PosRepository): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())
        root.put("app", "ShopPOS")

        // Settings
        val settings = repository.getAppSettings()
        val settingsObj = JSONObject().apply {
            put("id", settings.id)
            put("shopName", settings.shopName)
            put("shopAddress", settings.shopAddress)
            put("phone", settings.phone)
            put("email", settings.email)
            put("currency", settings.currency)
            put("receiptFooter", settings.receiptFooter)
            put("lowStockThreshold", settings.lowStockThreshold)
            put("logoUri", settings.logoUri ?: "")
            put("securityPin", settings.securityPin)
            put("pinLockEnabled", settings.pinLockEnabled)
            put("themeMode", settings.themeMode)
        }
        root.put("settings", settingsObj)

        // Categories
        val categories = repository.getAllCategoriesRaw()
        val catArray = JSONArray()
        for (c in categories) {
            val obj = JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("description", c.description ?: "")
                put("createdAt", c.createdAt)
            }
            catArray.put(obj)
        }
        root.put("categories", catArray)

        // Products
        val products = repository.getAllProductsRaw()
        val prodArray = JSONArray()
        for (p in products) {
            val obj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("sku", p.sku)
                put("barcode", p.barcode ?: "")
                put("categoryId", p.categoryId ?: -1L)
                put("costPricePesewas", p.costPricePesewas)
                put("sellingPricePesewas", p.sellingPricePesewas)
                put("stockQuantity", p.stockQuantity)
                put("minimumStock", p.minimumStock)
                put("unit", p.unit)
                put("imageUri", p.imageUri ?: "")
                put("active", p.active)
                put("createdAt", p.createdAt)
                put("updatedAt", p.updatedAt)
            }
            prodArray.put(obj)
        }
        root.put("products", prodArray)

        // Sales
        val sales = repository.getAllSalesRaw()
        val salesArray = JSONArray()
        for (s in sales) {
            val obj = JSONObject().apply {
                put("id", s.id)
                put("receiptNumber", s.receiptNumber)
                put("subtotalPesewas", s.subtotalPesewas)
                put("discountPesewas", s.discountPesewas)
                put("totalPesewas", s.totalPesewas)
                put("paymentMethod", s.paymentMethod)
                put("amountReceivedPesewas", s.amountReceivedPesewas)
                put("changeAmountPesewas", s.changeAmountPesewas)
                put("status", s.status)
                put("cancelReason", s.cancelReason ?: "")
                put("cancelledAt", s.cancelledAt ?: 0L)
                put("cashierName", s.cashierName)
                put("paymentReference", s.paymentReference ?: "")
                put("createdAt", s.createdAt)
            }
            salesArray.put(obj)
        }
        root.put("sales", salesArray)

        // Sale Items
        val saleItems = repository.getAllSaleItemsRaw()
        val itemsArray = JSONArray()
        for (si in saleItems) {
            val obj = JSONObject().apply {
                put("id", si.id)
                put("saleId", si.saleId)
                put("productId", si.productId)
                put("productName", si.productName)
                put("quantity", si.quantity)
                put("unitPricePesewas", si.unitPricePesewas)
                put("subtotalPesewas", si.subtotalPesewas)
            }
            itemsArray.put(obj)
        }
        root.put("saleItems", itemsArray)

        // Inventory Movements
        val movements = repository.getAllMovementsRaw()
        val movArray = JSONArray()
        for (m in movements) {
            val obj = JSONObject().apply {
                put("id", m.id)
                put("productId", m.productId)
                put("type", m.type)
                put("quantity", m.quantity)
                put("previousStock", m.previousStock)
                put("newStock", m.newStock)
                put("reason", m.reason ?: "")
                put("reference", m.reference ?: "")
                put("createdAt", m.createdAt)
            }
            movArray.put(obj)
        }
        root.put("movements", movArray)

        // Payments
        val payments = repository.getAllPaymentsRaw()
        val payArray = JSONArray()
        for (p in payments) {
            val obj = JSONObject().apply {
                put("id", p.id)
                put("saleId", p.saleId)
                put("method", p.method)
                put("amountPesewas", p.amountPesewas)
                put("reference", p.reference ?: "")
                put("createdAt", p.createdAt)
            }
            payArray.put(obj)
        }
        root.put("payments", payArray)

        return root.toString(2)
    }

    fun parseJsonBackup(jsonString: String): BackupData {
        val root = JSONObject(jsonString)

        // Settings
        val settings = if (root.has("settings")) {
            val so = root.getJSONObject("settings")
            AppSettings(
                id = so.optLong("id", 1L),
                shopName = so.optString("shopName", "My Shop"),
                shopAddress = so.optString("shopAddress", "Accra, Ghana"),
                phone = so.optString("phone", "050 000 0000"),
                email = so.optString("email", "info@myshop.com"),
                currency = so.optString("currency", "GHS"),
                receiptFooter = so.optString("receiptFooter", "THANK YOU!\nPLEASE COME AGAIN"),
                lowStockThreshold = so.optInt("lowStockThreshold", 10),
                logoUri = so.optString("logoUri").takeIf { it.isNotBlank() },
                securityPin = so.optString("securityPin", "1234"),
                pinLockEnabled = so.optBoolean("pinLockEnabled", false),
                themeMode = so.optString("themeMode", "SYSTEM")
            )
        } else null

        // Categories
        val categories = mutableListOf<Category>()
        if (root.has("categories")) {
            val ca = root.getJSONArray("categories")
            for (i in 0 until ca.length()) {
                val co = ca.getJSONObject(i)
                categories.add(
                    Category(
                        id = co.optLong("id", 0L),
                        name = co.getString("name"),
                        description = co.optString("description").takeIf { it.isNotBlank() },
                        createdAt = co.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        // Products
        val products = mutableListOf<Product>()
        if (root.has("products")) {
            val pa = root.getJSONArray("products")
            for (i in 0 until pa.length()) {
                val po = pa.getJSONObject(i)
                val catId = po.optLong("categoryId", -1L).takeIf { it > 0 }
                products.add(
                    Product(
                        id = po.optLong("id", 0L),
                        name = po.getString("name"),
                        sku = po.optString("sku", ""),
                        barcode = po.optString("barcode").takeIf { it.isNotBlank() },
                        categoryId = catId,
                        costPricePesewas = po.optLong("costPricePesewas", 0L),
                        sellingPricePesewas = po.optLong("sellingPricePesewas", 0L),
                        stockQuantity = po.optInt("stockQuantity", 0),
                        minimumStock = po.optInt("minimumStock", 10),
                        unit = po.optString("unit", "Pcs"),
                        imageUri = po.optString("imageUri").takeIf { it.isNotBlank() },
                        active = po.optBoolean("active", true),
                        createdAt = po.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = po.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
        }

        // Sales
        val sales = mutableListOf<Sale>()
        if (root.has("sales")) {
            val sa = root.getJSONArray("sales")
            for (i in 0 until sa.length()) {
                val so = sa.getJSONObject(i)
                sales.add(
                    Sale(
                        id = so.optLong("id", 0L),
                        receiptNumber = so.getString("receiptNumber"),
                        subtotalPesewas = so.optLong("subtotalPesewas", 0L),
                        discountPesewas = so.optLong("discountPesewas", 0L),
                        totalPesewas = so.optLong("totalPesewas", 0L),
                        paymentMethod = so.optString("paymentMethod", "Cash"),
                        amountReceivedPesewas = so.optLong("amountReceivedPesewas", 0L),
                        changeAmountPesewas = so.optLong("changeAmountPesewas", 0L),
                        status = so.optString("status", "COMPLETED"),
                        cancelReason = so.optString("cancelReason").takeIf { it.isNotBlank() },
                        cancelledAt = so.optLong("cancelledAt").takeIf { it > 0 },
                        cashierName = so.optString("cashierName", "Cashier"),
                        paymentReference = so.optString("paymentReference").takeIf { it.isNotBlank() },
                        createdAt = so.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        // Sale items
        val saleItems = mutableListOf<SaleItem>()
        if (root.has("saleItems")) {
            val sia = root.getJSONArray("saleItems")
            for (i in 0 until sia.length()) {
                val sio = sia.getJSONObject(i)
                saleItems.add(
                    SaleItem(
                        id = sio.optLong("id", 0L),
                        saleId = sio.getLong("saleId"),
                        productId = sio.getLong("productId"),
                        productName = sio.getString("productName"),
                        quantity = sio.getInt("quantity"),
                        unitPricePesewas = sio.getLong("unitPricePesewas"),
                        subtotalPesewas = sio.getLong("subtotalPesewas")
                    )
                )
            }
        }

        // Inventory movements
        val movements = mutableListOf<InventoryMovement>()
        if (root.has("movements")) {
            val ma = root.getJSONArray("movements")
            for (i in 0 until ma.length()) {
                val mo = ma.getJSONObject(i)
                movements.add(
                    InventoryMovement(
                        id = mo.optLong("id", 0L),
                        productId = mo.getLong("productId"),
                        type = mo.getString("type"),
                        quantity = mo.getInt("quantity"),
                        previousStock = mo.getInt("previousStock"),
                        newStock = mo.getInt("newStock"),
                        reason = mo.optString("reason").takeIf { it.isNotBlank() },
                        reference = mo.optString("reference").takeIf { it.isNotBlank() },
                        createdAt = mo.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        // Payments
        val payments = mutableListOf<Payment>()
        if (root.has("payments")) {
            val pya = root.getJSONArray("payments")
            for (i in 0 until pya.length()) {
                val pyo = pya.getJSONObject(i)
                payments.add(
                    Payment(
                        id = pyo.optLong("id", 0L),
                        saleId = pyo.getLong("saleId"),
                        method = pyo.getString("method"),
                        amountPesewas = pyo.getLong("amountPesewas"),
                        reference = pyo.optString("reference").takeIf { it.isNotBlank() },
                        createdAt = pyo.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        return BackupData(categories, products, sales, saleItems, movements, payments, settings)
    }

    fun writeToUri(context: Context, uri: Uri, content: String): Result<Unit> {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(content)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun readFromUri(context: Context, uri: Uri): Result<String> {
        return try {
            val sb = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        sb.append(line).append("\n")
                        line = reader.readLine()
                    }
                }
            }
            Result.success(sb.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
