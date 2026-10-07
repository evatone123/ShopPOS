package com.example.data.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["sku"], unique = false),
        Index(value = ["barcode"], unique = false),
        Index(value = ["categoryId"])
    ]
)
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val sku: String,
    val barcode: String? = null,
    val categoryId: Long? = null,
    val costPricePesewas: Long = 0L,
    val sellingPricePesewas: Long = 0L,
    val stockQuantity: Int = 0,
    val minimumStock: Int = 10,
    val unit: String = "Pcs",
    val imageUri: String? = null,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getStockStatus(threshold: Int = minimumStock): StockStatus {
        return when {
            stockQuantity <= 0 -> StockStatus.OUT_OF_STOCK
            stockQuantity <= threshold -> StockStatus.LOW_STOCK
            else -> StockStatus.IN_STOCK
        }
    }
}

enum class StockStatus(val label: String) {
    IN_STOCK("In Stock"),
    LOW_STOCK("Low Stock"),
    OUT_OF_STOCK("Out of Stock")
}

@Entity(
    tableName = "sales",
    indices = [
        Index(value = ["receiptNumber"], unique = true),
        Index(value = ["createdAt"])
    ]
)
data class Sale(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val receiptNumber: String,
    val subtotalPesewas: Long,
    val discountPesewas: Long = 0L,
    val totalPesewas: Long,
    val paymentMethod: String, // "Cash", "Mobile Money", "Card", "Other"
    val amountReceivedPesewas: Long,
    val changeAmountPesewas: Long,
    val status: String = "COMPLETED", // "COMPLETED", "CANCELLED"
    val cancelReason: String? = null,
    val cancelledAt: Long? = null,
    val cashierName: String = "Cashier",
    val paymentReference: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "sale_items",
    indices = [
        Index(value = ["saleId"]),
        Index(value = ["productId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Sale::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SaleItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val saleId: Long = 0,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val unitPricePesewas: Long,
    val subtotalPesewas: Long
)

data class SaleWithItems(
    @Embedded
    val sale: Sale,
    @Relation(
        parentColumn = "id",
        entityColumn = "saleId"
    )
    val items: List<SaleItem>
)

@Entity(
    tableName = "inventory_movements",
    indices = [
        Index(value = ["productId"]),
        Index(value = ["createdAt"])
    ]
)
data class InventoryMovement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val type: String, // "SALE", "SALE_RETURN", "ADD_STOCK", "REMOVE_STOCK", "CORRECTION", "DAMAGED", "EXPIRED", "INITIAL"
    val quantity: Int,
    val previousStock: Int,
    val newStock: Int,
    val reason: String? = null,
    val reference: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "payments",
    indices = [Index(value = ["saleId"])]
)
data class Payment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val saleId: Long,
    val method: String,
    val amountPesewas: Long,
    val reference: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey
    val id: Long = 1,
    val shopName: String = "My Shop",
    val shopAddress: String = "Accra, Ghana",
    val phone: String = "050 000 0000",
    val email: String = "info@myshop.com",
    val currency: String = "GHS",
    val receiptFooter: String = "THANK YOU!\nPLEASE COME AGAIN",
    val lowStockThreshold: Int = 10,
    val logoUri: String? = null,
    val securityPin: String = "1234",
    val pinLockEnabled: Boolean = false,
    val themeMode: String = "SYSTEM" // "SYSTEM", "LIGHT", "DARK"
)
