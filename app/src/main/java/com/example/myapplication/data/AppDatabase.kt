package com.example.myapplication.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.myapplication.data.dao.AppDao
import com.example.myapplication.data.model.Cliente
import com.example.myapplication.data.model.Producto
import com.example.myapplication.data.model.Registro
import com.example.myapplication.data.model.Operador
import com.example.myapplication.data.model.TicketSequence
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Cliente::class, Producto::class, Operador::class, Registro::class, TicketSequence::class], version = 4, exportSchema = false)
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
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `operadores` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `nombre` TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `ticket_sequences` (`year` INTEGER NOT NULL, `lastNumber` INTEGER NOT NULL, PRIMARY KEY(`year`))")
                db.execSQL("CREATE TABLE `registros_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `clienteId` INTEGER NOT NULL, `productoId` INTEGER NOT NULL, `operadorId` INTEGER DEFAULT NULL, `codigoTicket` TEXT NOT NULL, `placaVehiculo` TEXT NOT NULL, `conductor` TEXT NOT NULL, `peso` REAL NOT NULL, `fecha` INTEGER NOT NULL, FOREIGN KEY(`clienteId`) REFERENCES `clientes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`productoId`) REFERENCES `productos`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`operadorId`) REFERENCES `operadores`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL)")
                db.execSQL("INSERT INTO `registros_new` (`id`, `clienteId`, `productoId`, `operadorId`, `codigoTicket`, `placaVehiculo`, `conductor`, `peso`, `fecha`) SELECT `id`, `clienteId`, `productoId`, NULL, '2026-' || printf('%05d', `id`), '', '', `peso`, `fecha` FROM `registros`")
                db.execSQL("DROP TABLE `registros`")
                db.execSQL("ALTER TABLE `registros_new` RENAME TO `registros`")
                db.execSQL("INSERT INTO `ticket_sequences` (`year`, `lastNumber`) SELECT 2026, COALESCE(MAX(`id`), 0) FROM `registros`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_registros_clienteId` ON `registros` (`clienteId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_registros_productoId` ON `registros` (`productoId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_registros_operadorId` ON `registros` (`operadorId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_registros_codigoTicket` ON `registros` (`codigoTicket`)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `registros` ADD COLUMN `pesoBruto` REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `registros` ADD COLUMN `pesoTara` REAL NOT NULL DEFAULT 0")
                db.execSQL("UPDATE `registros` SET `pesoBruto` = `peso`")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `registros` ADD COLUMN `observacion` TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
