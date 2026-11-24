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

@Database(entities = [CustomerEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
}
