package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.dao.BestSellerDto
import com.example.data.database.AppDatabase
import com.example.data.entity.AppSettings
import com.example.data.entity.Category
import com.example.data.entity.InventoryMovement
import com.example.data.entity.Payment
import com.example.data.entity.Product
import com.example.data.entity.Sale
import com.example.data.entity.SaleItem
import com.example.data.entity.SaleWithItems
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CartItem(
    val product: Product,
    val quantity: Int
) {
    val subtotalPesewas: Long get() = product.sellingPricePesewas * quantity
}

class PosRepository(private val db: AppDatabase) {
    private val productDao = db.productDao()
    private val categoryDao = db.categoryDao()
    private val saleDao = db.saleDao()
    private val saleItemDao = db.saleItemDao()
    private val inventoryMovementDao = db.inventoryMovementDao()
    private val paymentDao = db.paymentDao()
    private val appSettingsDao = db.appSettingsDao()

    // --- Products ---
    val allProductsFlow: Flow<List<Product>> = productDao.getAllProductsFlow()
    val activeProductsFlow: Flow<List<Product>> = productDao.getActiveProductsFlow()
    val lowStockCountFlow: Flow<Int> = productDao.getLowStockCountFlow()
    val outOfStockCountFlow: Flow<Int> = productDao.getOutOfStockCountFlow()
    val lowAndOutOfStockProductsFlow: Flow<List<Product>> = productDao.getLowAndOutOfStockProductsFlow()
    val totalActiveProductsCountFlow: Flow<Int> = productDao.getTotalActiveProductsCountFlow()

    suspend fun getProductById(id: Long): Product? = productDao.getProductById(id)
    suspend fun findProductByBarcode(barcode: String): Product? = productDao.getProductByBarcode(barcode.trim())
    suspend fun findProductBySku(sku: String): Product? = productDao.getProductBySku(sku.trim())

    fun searchProductsFlow(query: String): Flow<List<Product>> =
        if (query.isBlank()) productDao.getActiveProductsFlow()
        else productDao.searchProductsFlow(query.trim())

    suspend fun saveProduct(product: Product, initialStockReason: String? = null): Long {
        return db.withTransaction {
            val isNew = product.id == 0L
            val previousProduct = if (!isNew) productDao.getProductById(product.id) else null
            val id = productDao.insert(product)

            if (isNew && product.stockQuantity > 0) {
                inventoryMovementDao.insert(
                    InventoryMovement(
                        productId = id,
                        type = "INITIAL",
                        quantity = product.stockQuantity,
                        previousStock = 0,
                        newStock = product.stockQuantity,
                        reason = initialStockReason ?: "Initial inventory setup"
                    )
                )
            } else if (previousProduct != null && previousProduct.stockQuantity != product.stockQuantity) {
                val diff = product.stockQuantity - previousProduct.stockQuantity
                val type = if (diff > 0) "CORRECTION_ADD" else "CORRECTION_SUB"
                inventoryMovementDao.insert(
                    InventoryMovement(
                        productId = id,
                        type = type,
                        quantity = kotlin.math.abs(diff),
                        previousStock = previousProduct.stockQuantity,
                        newStock = product.stockQuantity,
                        reason = "Product details edited"
                    )
                )
            }
            id
        }
    }

    suspend fun updateProduct(product: Product) {
        db.withTransaction {
            val old = productDao.getProductById(product.id)
            productDao.update(product)
            if (old != null && old.stockQuantity != product.stockQuantity) {
                val diff = product.stockQuantity - old.stockQuantity
                inventoryMovementDao.insert(
                    InventoryMovement(
                        productId = product.id,
                        type = "CORRECTION",
                        quantity = kotlin.math.abs(diff),
                        previousStock = old.stockQuantity,
                        newStock = product.stockQuantity,
                        reason = "Manual stock update via product edit"
                    )
                )
            }
        }
    }

    suspend fun deleteProduct(product: Product): Result<Unit> {
        val salesCount = productDao.getSalesHistoryCount(product.id)
        return if (salesCount > 0) {
            Result.failure(IllegalStateException("Cannot delete product with existing sales history. Deactivate product instead."))
        } else {
            productDao.delete(product)
            Result.success(Unit)
        }
    }

    // --- Categories ---
    val allCategoriesFlow: Flow<List<Category>> = categoryDao.getAllCategoriesFlow()
    suspend fun getAllCategories(): List<Category> = categoryDao.getAllCategories()
    suspend fun saveCategory(category: Category): Long = categoryDao.insert(category)
    suspend fun updateCategory(category: Category) = categoryDao.update(category)
    suspend fun deleteCategory(category: Category): Result<Unit> {
        val count = categoryDao.getProductCountForCategory(category.id)
        return if (count > 0) {
            Result.failure(IllegalStateException("Cannot delete category containing $count product(s). Reassign products first."))
        } else {
            categoryDao.delete(category)
            Result.success(Unit)
        }
    }

    // --- Stock Adjustments ---
    suspend fun adjustStock(
        productId: Long,
        type: String, // "ADD_STOCK", "REMOVE_STOCK", "CORRECTION", "DAMAGED", "EXPIRED", "OTHER"
        quantity: Int,
        reason: String
    ): Result<Int> {
        return db.withTransaction {
            val product = productDao.getProductById(productId)
                ?: return@withTransaction Result.failure(IllegalArgumentException("Product not found"))

            val previousStock = product.stockQuantity
            val newStock = when (type) {
                "ADD_STOCK" -> previousStock + quantity
                "REMOVE_STOCK", "DAMAGED", "EXPIRED" -> {
                    if (previousStock < quantity) {
                        return@withTransaction Result.failure(
                            IllegalStateException("Cannot remove $quantity items. Available stock is only $previousStock.")
                        )
                    }
                    previousStock - quantity
                }
                "CORRECTION" -> quantity // in correction, quantity represents target new stock
                else -> previousStock + quantity
            }

            productDao.updateStock(productId, newStock)
            inventoryMovementDao.insert(
                InventoryMovement(
                    productId = productId,
                    type = type,
                    quantity = if (type == "CORRECTION") kotlin.math.abs(newStock - previousStock) else quantity,
                    previousStock = previousStock,
                    newStock = newStock,
                    reason = reason
                )
            )
            Result.success(newStock)
        }
    }

    val allInventoryMovementsFlow: Flow<List<InventoryMovement>> = inventoryMovementDao.getAllMovementsFlow()
    fun getProductMovementsFlow(productId: Long): Flow<List<InventoryMovement>> =
        inventoryMovementDao.getMovementsForProductFlow(productId)

    // --- Sales & Transactions ---
    val allSalesFlow: Flow<List<Sale>> = saleDao.getAllSalesFlow()
    val allSalesWithItemsFlow: Flow<List<SaleWithItems>> = saleDao.getAllSalesWithItemsFlow()
    val recentSalesFlow: Flow<List<SaleWithItems>> = saleDao.getRecentSalesFlow(8)

    suspend fun getSaleWithItemsById(id: Long): SaleWithItems? = saleDao.getSaleWithItemsById(id)

    suspend fun generateNextReceiptNumber(): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
        val todayStr = dateFormat.format(Date())
        val prefix = "POS-$todayStr-"
        val countToday = saleDao.countReceiptsWithPrefix(prefix)
        val nextSeq = countToday + 1
        return "$prefix${String.format(Locale.US, "%04d", nextSeq)}"
    }

    suspend fun completeSale(
        cartItems: List<CartItem>,
        subtotalPesewas: Long,
        discountPesewas: Long,
        totalPesewas: Long,
        paymentMethod: String,
        amountReceivedPesewas: Long,
        changeAmountPesewas: Long,
        cashierName: String = "Cashier",
        paymentReference: String? = null
    ): Result<SaleWithItems> {
        if (cartItems.isEmpty()) {
            return Result.failure(IllegalArgumentException("Cart is empty"))
        }

        return db.withTransaction {
            // 1. Verify stock availability for all items
            for (item in cartItems) {
                val currentProduct = productDao.getProductById(item.product.id)
                    ?: return@withTransaction Result.failure(
                        IllegalArgumentException("Product '${item.product.name}' was not found.")
                    )
                if (currentProduct.stockQuantity < item.quantity) {
                    return@withTransaction Result.failure(
                        IllegalStateException(
                            "Not enough stock for '${currentProduct.name}'. Available: ${currentProduct.stockQuantity}, Requested: ${item.quantity}"
                        )
                    )
                }
            }

            // 2. Generate Receipt Number
            val receiptNumber = generateNextReceiptNumber()

            // 3. Create Sale record
            val sale = Sale(
                receiptNumber = receiptNumber,
                subtotalPesewas = subtotalPesewas,
                discountPesewas = discountPesewas,
                totalPesewas = totalPesewas,
                paymentMethod = paymentMethod,
                amountReceivedPesewas = amountReceivedPesewas,
                changeAmountPesewas = changeAmountPesewas,
                status = "COMPLETED",
                cashierName = cashierName,
                paymentReference = paymentReference,
                createdAt = System.currentTimeMillis()
            )
            val saleId = saleDao.insertSale(sale)

            // 4. Create SaleItems & update inventory
            val saleItems = mutableListOf<SaleItem>()
            for (item in cartItems) {
                val product = productDao.getProductById(item.product.id)!!
                val prevStock = product.stockQuantity
                val newStock = prevStock - item.quantity

                saleItems.add(
                    SaleItem(
                        saleId = saleId,
                        productId = product.id,
                        productName = product.name,
                        quantity = item.quantity,
                        unitPricePesewas = product.sellingPricePesewas,
                        subtotalPesewas = item.subtotalPesewas
                    )
                )

                // Update product stock
                productDao.updateStock(product.id, newStock)

                // Log movement
                inventoryMovementDao.insert(
                    InventoryMovement(
                        productId = product.id,
                        type = "SALE",
                        quantity = item.quantity,
                        previousStock = prevStock,
                        newStock = newStock,
                        reason = "Sale $receiptNumber",
                        reference = receiptNumber
                    )
                )
            }
            saleItemDao.insertAll(saleItems)

            // 5. Create Payment record
            paymentDao.insert(
                Payment(
                    saleId = saleId,
                    method = paymentMethod,
                    amountPesewas = totalPesewas,
                    reference = paymentReference
                )
            )

            val createdSaleWithItems = saleDao.getSaleWithItemsById(saleId)
                ?: return@withTransaction Result.failure(IllegalStateException("Failed to retrieve created sale"))

            Result.success(createdSaleWithItems)
        }
    }

    suspend fun voidSale(saleId: Long, reason: String): Result<SaleWithItems> {
        return db.withTransaction {
            val saleWithItems = saleDao.getSaleWithItemsById(saleId)
                ?: return@withTransaction Result.failure(IllegalArgumentException("Sale not found"))

            if (saleWithItems.sale.status == "CANCELLED") {
                return@withTransaction Result.failure(IllegalStateException("Sale is already cancelled"))
            }

            // Update sale status
            val updatedSale = saleWithItems.sale.copy(
                status = "CANCELLED",
                cancelReason = reason,
                cancelledAt = System.currentTimeMillis()
            )
            saleDao.updateSale(updatedSale)

            // Restore inventory for each item
            for (item in saleWithItems.items) {
                val product = productDao.getProductById(item.productId)
                if (product != null) {
                    val prevStock = product.stockQuantity
                    val newStock = prevStock + item.quantity
                    productDao.updateStock(product.id, newStock)

                    inventoryMovementDao.insert(
                        InventoryMovement(
                            productId = product.id,
                            type = "SALE_RETURN",
                            quantity = item.quantity,
                            previousStock = prevStock,
                            newStock = newStock,
                            reason = "Voided Sale: $reason",
                            reference = saleWithItems.sale.receiptNumber
                        )
                    )
                }
            }

            val result = saleDao.getSaleWithItemsById(saleId)
                ?: return@withTransaction Result.failure(IllegalStateException("Sale not found"))
            Result.success(result)
        }
    }

    // --- Reports ---
    fun getSalesCountFlow(start: Long, end: Long): Flow<Int> =
        saleDao.getCompletedSalesCountFlow(start, end)

    fun getTotalRevenueFlow(start: Long, end: Long): Flow<Long> =
        saleDao.getTotalRevenueFlow(start, end)

    fun getItemsSoldCountFlow(start: Long, end: Long): Flow<Int> =
        saleDao.getItemsSoldCountFlow(start, end)

    fun getSalesWithItemsBetweenFlow(start: Long, end: Long): Flow<List<SaleWithItems>> =
        saleDao.getSalesWithItemsBetweenFlow(start, end)

    fun getBestSellersFlow(start: Long, end: Long, limit: Int = 10): Flow<List<BestSellerDto>> =
        saleDao.getBestSellersFlow(start, end, limit)

    // --- Settings ---
    val appSettingsFlow: Flow<AppSettings?> = appSettingsDao.getSettingsFlow()
    suspend fun getAppSettings(): AppSettings = appSettingsDao.getSettings() ?: AppSettings()
    suspend fun updateAppSettings(settings: AppSettings) = appSettingsDao.insertOrUpdate(settings)

    // --- Demo Data ---
    suspend fun loadDemoData() {
        db.withTransaction {
            val categories = listOf(
                Category(name = "Drinks", description = "Beverages, juices, water"),
                Category(name = "Food", description = "Groceries, snacks, staples"),
                Category(name = "Toiletries", description = "Personal hygiene, soap, shampoo"),
                Category(name = "Household", description = "Cleaning supplies, tissue"),
                Category(name = "Electronics", description = "Accessories, chargers, cables"),
                Category(name = "Stationery", description = "Pens, books, papers"),
                Category(name = "Other", description = "Miscellaneous general store items")
            )
            val catIds = categories.map { categoryDao.insert(it) }
            val drinksId = catIds[0]
            val foodId = catIds[1]
            val toiletriesId = catIds[2]
            val householdId = catIds[3]

            val demoProducts = listOf(
                Product(
                    name = "Coca Cola 500ml",
                    sku = "COKE500",
                    barcode = "5449000000996",
                    categoryId = drinksId,
                    costPricePesewas = 500L,
                    sellingPricePesewas = 700L,
                    stockQuantity = 50,
                    minimumStock = 10,
                    unit = "Bottle"
                ),
                Product(
                    name = "Mineral Water 750ml",
                    sku = "WATER750",
                    barcode = "6001234567890",
                    categoryId = drinksId,
                    costPricePesewas = 250L,
                    sellingPricePesewas = 400L,
                    stockQuantity = 80,
                    minimumStock = 15,
                    unit = "Bottle"
                ),
                Product(
                    name = "Golden Butter Bread",
                    sku = "BREAD01",
                    barcode = "5449000012345",
                    categoryId = foodId,
                    costPricePesewas = 900L,
                    sellingPricePesewas = 1200L,
                    stockQuantity = 7,
                    minimumStock = 10,
                    unit = "Loaf"
                ),
                Product(
                    name = "Cowbell Milk Sachet",
                    sku = "MILK01",
                    barcode = "5449000023456",
                    categoryId = foodId,
                    costPricePesewas = 250L,
                    sellingPricePesewas = 350L,
                    stockQuantity = 35,
                    minimumStock = 10,
                    unit = "Sachet"
                ),
                Product(
                    name = "Milo 400g Tin",
                    sku = "MILO400",
                    barcode = "7613035345678",
                    categoryId = foodId,
                    costPricePesewas = 2600L,
                    sellingPricePesewas = 3200L,
                    stockQuantity = 15,
                    minimumStock = 5,
                    unit = "Tin"
                ),
                Product(
                    name = "St. Louis Sugar 500g",
                    sku = "SUGAR01",
                    barcode = "3017620422003",
                    categoryId = foodId,
                    costPricePesewas = 1100L,
                    sellingPricePesewas = 1400L,
                    stockQuantity = 22,
                    minimumStock = 8,
                    unit = "Box"
                ),
                Product(
                    name = "Royal Feast Jasmine Rice 5kg",
                    sku = "RICE5KG",
                    barcode = "8850123456789",
                    categoryId = foodId,
                    costPricePesewas = 8000L,
                    sellingPricePesewas = 9500L,
                    stockQuantity = 12,
                    minimumStock = 5,
                    unit = "Bag"
                ),
                Product(
                    name = "Frytol Cooking Oil 1L",
                    sku = "OIL1L",
                    barcode = "6009876543210",
                    categoryId = householdId,
                    costPricePesewas = 3200L,
                    sellingPricePesewas = 3800L,
                    stockQuantity = 0, // Out of stock
                    minimumStock = 5,
                    unit = "Bottle"
                ),
                Product(
                    name = "Geisha Soap 175g",
                    sku = "SOAP01",
                    barcode = "8712561234567",
                    categoryId = toiletriesId,
                    costPricePesewas = 480L,
                    sellingPricePesewas = 650L,
                    stockQuantity = 40,
                    minimumStock = 10,
                    unit = "Bar"
                ),
                Product(
                    name = "Bel-Aqua Tissue Pack",
                    sku = "TISSUE01",
                    barcode = "6001112223334",
                    categoryId = householdId,
                    costPricePesewas = 1400L,
                    sellingPricePesewas = 1800L,
                    stockQuantity = 20,
                    minimumStock = 6,
                    unit = "Pack"
                ),
                Product(
                    name = "Digestives Biscuits",
                    sku = "BISC01",
                    barcode = "5000168001001",
                    categoryId = foodId,
                    costPricePesewas = 600L,
                    sellingPricePesewas = 800L,
                    stockQuantity = 30,
                    minimumStock = 8,
                    unit = "Roll"
                )
            )

            for (p in demoProducts) {
                val pId = productDao.insert(p)
                if (p.stockQuantity > 0) {
                    inventoryMovementDao.insert(
                        InventoryMovement(
                            productId = pId,
                            type = "INITIAL",
                            quantity = p.stockQuantity,
                            previousStock = 0,
                            newStock = p.stockQuantity,
                            reason = "Demo store initialization"
                        )
                    )
                }
            }

            // Create a few sample completed sales for today so dashboard has live stats
            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance()
            cal.timeInMillis = now

            val coke = productDao.getProductBySku("COKE500")
            val bread = productDao.getProductBySku("BREAD01")
            val water = productDao.getProductBySku("WATER750")

            if (coke != null && bread != null && water != null) {
                // Sale 1
                val r1 = "POS-${SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())}-0001"
                val s1Id = saleDao.insertSale(
                    Sale(
                        receiptNumber = r1,
                        subtotalPesewas = 1900L,
                        discountPesewas = 0L,
                        totalPesewas = 1900L,
                        paymentMethod = "Cash",
                        amountReceivedPesewas = 2000L,
                        changeAmountPesewas = 100L,
                        status = "COMPLETED",
                        createdAt = now - (35 * 60 * 1000)
                    )
                )
                saleItemDao.insertAll(
                    listOf(
                        SaleItem(saleId = s1Id, productId = coke.id, productName = coke.name, quantity = 1, unitPricePesewas = 700L, subtotalPesewas = 700L),
                        SaleItem(saleId = s1Id, productId = bread.id, productName = bread.name, quantity = 1, unitPricePesewas = 1200L, subtotalPesewas = 1200L)
                    )
                )
                paymentDao.insert(Payment(saleId = s1Id, method = "Cash", amountPesewas = 1900L))

                // Sale 2
                val r2 = "POS-${SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())}-0002"
                val s2Id = saleDao.insertSale(
                    Sale(
                        receiptNumber = r2,
                        subtotalPesewas = 1400L,
                        discountPesewas = 0L,
                        totalPesewas = 1400L,
                        paymentMethod = "Mobile Money",
                        amountReceivedPesewas = 1400L,
                        changeAmountPesewas = 0L,
                        status = "COMPLETED",
                        paymentReference = "MOM-98214",
                        createdAt = now - (15 * 60 * 1000)
                    )
                )
                saleItemDao.insertAll(
                    listOf(
                        SaleItem(saleId = s2Id, productId = coke.id, productName = coke.name, quantity = 2, unitPricePesewas = 700L, subtotalPesewas = 1400L)
                    )
                )
                paymentDao.insert(Payment(saleId = s2Id, method = "Mobile Money", amountPesewas = 1400L, reference = "MOM-98214"))
            }
        }
    }

    // --- Raw Data for Backup/Restore ---
    suspend fun getAllCategoriesRaw(): List<Category> = categoryDao.getAllCategories()
    suspend fun getAllProductsRaw(): List<Product> = productDao.getAllProducts()
    suspend fun getAllSalesRaw(): List<Sale> = saleDao.getAllSales()
    suspend fun getAllSaleItemsRaw(): List<SaleItem> = saleItemDao.getAllSaleItems()
    suspend fun getAllMovementsRaw(): List<InventoryMovement> = inventoryMovementDao.getAllMovements()
    suspend fun getAllPaymentsRaw(): List<Payment> = paymentDao.getAllPayments()

    suspend fun restoreData(
        categories: List<Category>,
        products: List<Product>,
        sales: List<Sale>,
        saleItems: List<SaleItem>,
        movements: List<InventoryMovement>,
        payments: List<Payment>,
        settings: AppSettings?,
        replaceExisting: Boolean
    ) {
        db.withTransaction {
            if (replaceExisting) {
                db.clearAllTables()
            }
            if (settings != null) {
                appSettingsDao.insertOrUpdate(settings)
            }
            categoryDao.insertAll(categories)
            productDao.insertAll(products)
            saleDao.insertAll(sales)
            saleItemDao.insertAll(saleItems)
            inventoryMovementDao.insertAll(movements)
            paymentDao.insertAll(payments)
        }
    }
}
