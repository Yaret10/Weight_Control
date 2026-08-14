package com.example.myapplication.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.myapplication.data.dao.AppDao
import com.example.myapplication.data.model.Cliente
import com.example.myapplication.data.model.Producto
import com.example.myapplication.data.model.Registro

@Database(entities = [Cliente::class, Producto::class, Registro::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pesaje_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
