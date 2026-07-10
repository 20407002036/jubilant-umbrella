package com.financialassistant.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.financialassistant.data.dao.CategoryDao
import com.financialassistant.data.dao.RecipientDao
import com.financialassistant.data.dao.TransactionDao
import com.financialassistant.data.entities.CategoryEntity
import com.financialassistant.data.entities.RecipientEntity
import com.financialassistant.data.entities.TransactionEntity

@Database(
    entities = [TransactionEntity::class, CategoryEntity::class, RecipientEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun recipientDao(): RecipientDao

    companion object {
        const val DATABASE_NAME = "financial_assistant_db"

        val PREPOPULATE_CALLBACK = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Direct SQL prepopulation of standard categories
                db.execSQL("INSERT OR IGNORE INTO categories (name, iconName, colorHex) VALUES ('Groceries', 'shopping_cart', '#4CAF50')")
                db.execSQL("INSERT OR IGNORE INTO categories (name, iconName, colorHex) VALUES ('Food', 'restaurant', '#FF9800')")
                db.execSQL("INSERT OR IGNORE INTO categories (name, iconName, colorHex) VALUES ('Transport', 'directions_car', '#2196F3')")
                db.execSQL("INSERT OR IGNORE INTO categories (name, iconName, colorHex) VALUES ('Utilities', 'bolt', '#9C27B0')")
                db.execSQL("INSERT OR IGNORE INTO categories (name, iconName, colorHex) VALUES ('Airtime', 'phone_android', '#00BCD4')")
                db.execSQL("INSERT OR IGNORE INTO categories (name, iconName, colorHex) VALUES ('Income', 'payments', '#E91E63')")
                db.execSQL("INSERT OR IGNORE INTO categories (name, iconName, colorHex) VALUES ('Savings', 'savings', '#009688')")
                db.execSQL("INSERT OR IGNORE INTO categories (name, iconName, colorHex) VALUES ('Entertainment', 'sports_esports', '#3F51B5')")
                db.execSQL("INSERT OR IGNORE INTO categories (name, iconName, colorHex) VALUES ('Uncategorized', 'category', '#757575')")
            }
        }
    }
}
