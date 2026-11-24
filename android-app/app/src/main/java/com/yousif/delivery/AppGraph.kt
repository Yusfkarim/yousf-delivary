package com.yousif.delivery

import android.content.Context
import androidx.room.Room
import com.yousif.delivery.data.AppDatabase
import com.yousif.delivery.data.CustomerRepository
import com.yousif.delivery.data.remote.ApiClient
import com.yousif.delivery.session.SessionStore

/** Tiny manual DI container shared by the ViewModel and the sync worker. */
object AppGraph {

    @Volatile
    private var sessionStore: SessionStore? = null

    @Volatile
    private var customerRepository: CustomerRepository? = null

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

    private fun build(context: Context): CustomerRepository {
        val database = Room.databaseBuilder(context, AppDatabase::class.java, "yousif-delivery.db")
            .fallbackToDestructiveMigration()
            .build()
        return CustomerRepository(
            context = context,
            dao = database.customerDao(),
            session = session(context),
            api = ApiClient(),
        )
    }
}
