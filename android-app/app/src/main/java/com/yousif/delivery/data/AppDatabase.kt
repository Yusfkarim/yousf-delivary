package com.yousif.delivery.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v2 adds the `kind` column so shops live beside customers in the same table.
 * The migration is additive, so an upgrade never wipes the local phonebook.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE customers ADD COLUMN kind TEXT NOT NULL DEFAULT 'customer'")
    }
}

/** v3 adds «کاڵای کڕیار» — a local-only list of goods, never sent anywhere. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS goods (
                id TEXT NOT NULL PRIMARY KEY,
                title TEXT NOT NULL,
                quantity INTEGER NOT NULL,
                note TEXT NOT NULL,
                customerId TEXT,
                customerName TEXT,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }
}

@Database(entities = [CustomerEntity::class, GoodEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun goodDao(): GoodDao
}
