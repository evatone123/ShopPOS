package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.AppSettingsDao
import com.example.data.dao.CategoryDao
import com.example.data.dao.InventoryMovementDao
import com.example.data.dao.PaymentDao
import com.example.data.dao.ProductDao
import com.example.data.dao.SaleDao
import com.example.data.dao.SaleItemDao
import com.example.data.entity.AppSettings
import com.example.data.entity.Category
import com.example.data.entity.InventoryMovement
import com.example.data.entity.Payment
import com.example.data.entity.Product
import com.example.data.entity.Sale
import com.example.data.entity.SaleItem

@Database(
    entities = [
        Category::class,
        Product::class,
        Sale::class,
        SaleItem::class,
        InventoryMovement::class,
        Payment::class,
        AppSettings::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
    abstract fun saleItemDao(): SaleItemDao
    abstract fun inventoryMovementDao(): InventoryMovementDao
    abstract fun paymentDao(): PaymentDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN themeMode TEXT NOT NULL DEFAULT 'SYSTEM'")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shoppos_database.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Initialize default app settings and categories directly in SQLite without coroutine deadlocks
                            db.execSQL("""
                                INSERT OR IGNORE INTO app_settings (id, shopName, shopAddress, phone, email, currency, receiptFooter, lowStockThreshold, pinLockEnabled, securityPin, themeMode)
                                VALUES (1, 'My Shop', 'Accra, Ghana', '050 000 0000', 'info@myshop.com', 'GHS', 'THANK YOU!\nPLEASE COME AGAIN', 10, 0, '1234', 'SYSTEM')
                            """)
                            val now = System.currentTimeMillis()
                            db.execSQL("INSERT OR IGNORE INTO categories (id, name, description, createdAt) VALUES (1, 'Drinks', 'Beverages, juices, water', $now)")
                            db.execSQL("INSERT OR IGNORE INTO categories (id, name, description, createdAt) VALUES (2, 'Food', 'Groceries, snacks, staples', $now)")
                            db.execSQL("INSERT OR IGNORE INTO categories (id, name, description, createdAt) VALUES (3, 'Toiletries', 'Personal hygiene, soap, shampoo', $now)")
                            db.execSQL("INSERT OR IGNORE INTO categories (id, name, description, createdAt) VALUES (4, 'Household', 'Cleaning supplies, tissue', $now)")
                            db.execSQL("INSERT OR IGNORE INTO categories (id, name, description, createdAt) VALUES (5, 'Electronics', 'Accessories, chargers, cables', $now)")
                            db.execSQL("INSERT OR IGNORE INTO categories (id, name, description, createdAt) VALUES (6, 'Stationery', 'Pens, books, papers', $now)")
                            db.execSQL("INSERT OR IGNORE INTO categories (id, name, description, createdAt) VALUES (7, 'Other', 'General store items', $now)")
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
