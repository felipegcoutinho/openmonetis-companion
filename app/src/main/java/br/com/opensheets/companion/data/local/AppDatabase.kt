package br.com.opensheets.companion.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import br.com.opensheets.companion.data.local.dao.AppConfigDao
import br.com.opensheets.companion.data.local.dao.KeywordsSettingsDao
import br.com.opensheets.companion.data.local.dao.NotificationDao
import br.com.opensheets.companion.data.local.dao.SyncLogDao
import br.com.opensheets.companion.data.local.entities.AppConfigEntity
import br.com.opensheets.companion.data.local.entities.KeywordsSettingsEntity
import br.com.opensheets.companion.data.local.entities.NotificationEntity
import br.com.opensheets.companion.data.local.entities.SyncLogEntity

@Database(
    entities = [
        NotificationEntity::class,
        AppConfigEntity::class,
        KeywordsSettingsEntity::class,
        SyncLogEntity::class
    ],
    version = 5,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
    abstract fun appConfigDao(): AppConfigDao
    abstract fun keywordsSettingsDao(): KeywordsSettingsDao
    abstract fun syncLogDao(): SyncLogDao

    companion object {
        // Hardcoded defaults for migrations (constants were removed from entity)
        private const val DEFAULT_TRIGGER = "compra,R\$,pix,transferência,débito,crédito,saque,pagamento,boleto,fatura"
        private const val DEFAULT_EXPENSE = "compra,débito,pagamento,saque,transferência enviada,pix enviado,boleto,fatura,cobrança"
        private const val DEFAULT_INCOME = "recebido,recebeu,depósito,transferência recebida,pix recebido,crédito,estorno,cashback"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS keywords_settings (
                        id INTEGER PRIMARY KEY NOT NULL,
                        trigger_keywords TEXT NOT NULL,
                        expense_keywords TEXT NOT NULL,
                        income_keywords TEXT NOT NULL
                    )
                """.trimIndent())
                
                db.execSQL("""
                    INSERT OR IGNORE INTO keywords_settings (id, trigger_keywords, expense_keywords, income_keywords) 
                    VALUES (1, '$DEFAULT_TRIGGER', '$DEFAULT_EXPENSE', '$DEFAULT_INCOME')
                """.trimIndent())
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add trigger_keywords column to existing table
                db.execSQL("""
                    ALTER TABLE keywords_settings 
                    ADD COLUMN trigger_keywords TEXT NOT NULL DEFAULT '$DEFAULT_TRIGGER'
                """.trimIndent())
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create sync_logs table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS sync_logs (
                        id TEXT PRIMARY KEY NOT NULL,
                        timestamp INTEGER NOT NULL,
                        type TEXT NOT NULL,
                        message TEXT NOT NULL,
                        notification_id TEXT,
                        details TEXT
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Remove expense/income keywords columns and parsed_transaction_type
                // SQLite doesn't support DROP COLUMN directly, recreate table
                db.execSQL("""
                    CREATE TABLE keywords_settings_new (
                        id INTEGER PRIMARY KEY NOT NULL,
                        trigger_keywords TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO keywords_settings_new (id, trigger_keywords)
                    SELECT id, trigger_keywords FROM keywords_settings
                """.trimIndent())
                db.execSQL("DROP TABLE keywords_settings")
                db.execSQL("ALTER TABLE keywords_settings_new RENAME TO keywords_settings")

                // Remove parsed_transaction_type from notifications
                db.execSQL("""
                    CREATE TABLE notifications_new (
                        id TEXT PRIMARY KEY NOT NULL,
                        package_name TEXT NOT NULL,
                        app_name TEXT NOT NULL,
                        title TEXT,
                        text TEXT NOT NULL,
                        notification_timestamp INTEGER NOT NULL,
                        capture_timestamp INTEGER NOT NULL,
                        parsed_amount REAL,
                        parsed_merchant_name TEXT,
                        parsed_card_last_digits TEXT,
                        sync_status TEXT NOT NULL,
                        sync_timestamp INTEGER,
                        sync_error TEXT
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO notifications_new (id, package_name, app_name, title, text, 
                        notification_timestamp, capture_timestamp, parsed_amount, parsed_merchant_name,
                        parsed_card_last_digits, sync_status, sync_timestamp, sync_error)
                    SELECT id, package_name, app_name, title, text, notification_timestamp, 
                        capture_timestamp, parsed_amount, parsed_merchant_name, parsed_card_last_digits,
                        sync_status, sync_timestamp, sync_error FROM notifications
                """.trimIndent())
                db.execSQL("DROP TABLE notifications")
                db.execSQL("ALTER TABLE notifications_new RENAME TO notifications")
            }
        }
    }
}
