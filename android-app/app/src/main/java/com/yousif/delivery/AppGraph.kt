package com.yousif.delivery

import android.content.Context
import androidx.room.Room
import com.yousif.delivery.data.AppDatabase
import com.yousif.delivery.data.GoodDao
import com.yousif.delivery.data.MIGRATION_1_2
import com.yousif.delivery.data.MIGRATION_2_3
import com.yousif.delivery.data.CustomerRepository
import com.yousif.delivery.data.remote.ApiClient
import com.yousif.delivery.session.SessionStore

/** Tiny manual DI container shared by the ViewModel and the sync worker. */
object AppGraph {

    @Volatile
    private var sessionStore: SessionStore? = null

    @Volatile
    private var customerRepository: CustomerRepository? = null

    @Volatile
    private var database: AppDatabase? = null

    fun session(context: Context): SessionStore {
        return sessionStore ?: synchronized(this) {
            sessionStore ?: SessionStore(context.applicationContext).also { sessionStore = it }
        }
    }

    fun repository(context: Context): CustomerRepository {
        return customerRepository ?: synchronized(this) {
            customerRepository ?: build(context.applicationContext).also { customerRepository = it }
        }
    }

    fun database(context: Context): AppDatabase {
        return database ?: synchronized(this) {
            database ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "yousif-delivery.db",
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()
                .also { database = it }
        }
    }

    fun goods(context: Context): GoodDao = database(context).goodDao()

    private fun build(context: Context): CustomerRepository {
        return CustomerRepository(
            context = context,
            dao = database(context).customerDao(),
            session = session(context),
            api = ApiClient(),
        )
    }
}
