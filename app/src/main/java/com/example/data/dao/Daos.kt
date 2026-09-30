package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.entity.AppSettings
import com.example.data.entity.Category
import com.example.data.entity.InventoryMovement
import com.example.data.entity.Payment
import com.example.data.entity.Product
import com.example.data.entity.Sale
import com.example.data.entity.SaleItem
import com.example.data.entity.SaleWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategoriesFlow(): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY name ASC")
    suspend fun getAllCategories(): List<Category>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: Long): Category?

    @Query("SELECT COUNT(*) FROM products WHERE categoryId = :categoryId")
    suspend fun getProductCountForCategory(categoryId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<Category>)

    @Update
    suspend fun update(category: Category)

    @Delete
    suspend fun delete(category: Category)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProductsFlow(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE active = 1 ORDER BY name ASC")
    fun getActiveProductsFlow(): Flow<List<Product>>

    @Query("SELECT * FROM products ORDER BY name ASC")
    suspend fun getAllProducts(): List<Product>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Long): Product?

    @Query("SELECT * FROM products WHERE barcode = :barcode AND active = 1 LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): Product?

    @Query("SELECT * FROM products WHERE sku = :sku AND active = 1 LIMIT 1")
    suspend fun getProductBySku(sku: String): Product?

    @Query("SELECT * FROM products WHERE (name LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%') AND active = 1 ORDER BY name ASC")
    fun searchProductsFlow(query: String): Flow<List<Product>>

    @Query("SELECT COUNT(*) FROM products WHERE active = 1")
    fun getTotalActiveProductsCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM products WHERE active = 1 AND stockQuantity <= minimumStock AND stockQuantity > 0")
    fun getLowStockCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM products WHERE active = 1 AND stockQuantity <= 0")
    fun getOutOfStockCountFlow(): Flow<Int>

    @Query("SELECT * FROM products WHERE active = 1 AND stockQuantity <= minimumStock ORDER BY stockQuantity ASC")
    fun getLowAndOutOfStockProductsFlow(): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: Product): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<Product>)

    @Update
    suspend fun update(product: Product)

    @Query("UPDATE products SET stockQuantity = :newStock, updatedAt = :updatedAt WHERE id = :productId")
    suspend fun updateStock(productId: Long, newStock: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM sale_items WHERE productId = :productId")
    suspend fun getSalesHistoryCount(productId: Long): Int

    @Delete
    suspend fun delete(product: Product)
}

data class BestSellerDto(
    val productId: Long,
    val productName: String,
    val totalQuantitySold: Int,
    val totalRevenuePesewas: Long
)

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY createdAt DESC")
    fun getAllSalesFlow(): Flow<List<Sale>>

    @Transaction
    @Query("SELECT * FROM sales ORDER BY createdAt DESC")
    fun getAllSalesWithItemsFlow(): Flow<List<SaleWithItems>>

    @Transaction
    @Query("SELECT * FROM sales WHERE createdAt >= :start AND createdAt <= :end ORDER BY createdAt DESC")
    fun getSalesWithItemsBetweenFlow(start: Long, end: Long): Flow<List<SaleWithItems>>

    @Transaction
    @Query("SELECT * FROM sales ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentSalesFlow(limit: Int = 10): Flow<List<SaleWithItems>>

    @Transaction
    @Query("SELECT * FROM sales WHERE id = :id LIMIT 1")
    suspend fun getSaleWithItemsById(id: Long): SaleWithItems?

    @Query("SELECT * FROM sales WHERE id = :id LIMIT 1")
    suspend fun getSaleById(id: Long): Sale?

    @Query("SELECT * FROM sales WHERE receiptNumber = :receiptNumber LIMIT 1")
    suspend fun getSaleByReceiptNumber(receiptNumber: String): Sale?

    @Query("SELECT COUNT(*) FROM sales WHERE status = 'COMPLETED' AND createdAt >= :start AND createdAt <= :end")
    fun getCompletedSalesCountFlow(start: Long, end: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(totalPesewas), 0) FROM sales WHERE status = 'COMPLETED' AND createdAt >= :start AND createdAt <= :end")
    fun getTotalRevenueFlow(start: Long, end: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(si.quantity), 0) FROM sale_items si INNER JOIN sales s ON si.saleId = s.id WHERE s.status = 'COMPLETED' AND s.createdAt >= :start AND s.createdAt <= :end")
    fun getItemsSoldCountFlow(start: Long, end: Long): Flow<Int>

    @Query("""
        SELECT si.productId, si.productName, SUM(si.quantity) as totalQuantitySold, SUM(si.subtotalPesewas) as totalRevenuePesewas
        FROM sale_items si
        INNER JOIN sales s ON si.saleId = s.id
        WHERE s.status = 'COMPLETED' AND s.createdAt >= :start AND s.createdAt <= :end
        GROUP BY si.productId, si.productName
        ORDER BY totalQuantitySold DESC
        LIMIT :limit
    """)
    fun getBestSellersFlow(start: Long, end: Long, limit: Int = 10): Flow<List<BestSellerDto>>

    @Query("SELECT COUNT(*) FROM sales WHERE receiptNumber LIKE :prefix || '%'")
    suspend fun countReceiptsWithPrefix(prefix: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: Sale): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sales: List<Sale>)

    @Update
    suspend fun updateSale(sale: Sale)

    @Query("SELECT * FROM sales")
    suspend fun getAllSales(): List<Sale>
}

@Dao
interface SaleItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SaleItem>)

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getItemsForSale(saleId: Long): List<SaleItem>

    @Query("SELECT * FROM sale_items")
    suspend fun getAllSaleItems(): List<SaleItem>
}

@Dao
interface InventoryMovementDao {
    @Query("SELECT * FROM inventory_movements ORDER BY createdAt DESC")
    fun getAllMovementsFlow(): Flow<List<InventoryMovement>>

    @Query("SELECT * FROM inventory_movements WHERE productId = :productId ORDER BY createdAt DESC")
    fun getMovementsForProductFlow(productId: Long): Flow<List<InventoryMovement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(movement: InventoryMovement): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(movements: List<InventoryMovement>)

    @Query("SELECT * FROM inventory_movements")
    suspend fun getAllMovements(): List<InventoryMovement>
}

@Dao
interface PaymentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: Payment): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(payments: List<Payment>)

    @Query("SELECT * FROM payments WHERE saleId = :saleId")
    suspend fun getPaymentsForSale(saleId: Long): List<Payment>

    @Query("SELECT * FROM payments")
    suspend fun getAllPayments(): List<Payment>
}

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: AppSettings)
}
