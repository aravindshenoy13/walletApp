package com.aravind.wallet.data

import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Frozen V2 seed values: never derive a past migration from a mutable UI catalog. */
object V2CategorySeeds {
    val rows = listOf(
        WalletCategory("FOOD", "Food", "utensils", "Peach", true),
        WalletCategory("GROCERIES", "Groceries", "shopping-basket", "Mint", true),
        WalletCategory("RENT", "Rent", "house", "Sky", true),
        WalletCategory("TRANSPORT", "Transport", "tag", "Seafoam", true),
        WalletCategory("UTILITIES", "Utilities", "house", "Butter", true),
        WalletCategory("HEALTH", "Health", "tag", "Rose", true),
        WalletCategory("SHOPPING", "Shopping", "shopping-basket", "Blush", true),
        WalletCategory("ENTERTAINMENT", "Entertainment", "tag", "Lilac", true),
        WalletCategory("EDUCATION", "Education", "book-open", "Sky", true),
        WalletCategory("SALARY", "Salary", "briefcase", "Butter", true),
        WalletCategory("GIFTS", "Gifts", "tag", "Blush", true),
        WalletCategory("OTHER", "Other", "tag", "Lilac", true),
    )

    fun insert(db: SupportSQLiteDatabase) {
        rows.forEach { category ->
            db.execSQL("INSERT INTO categories (id,name,iconKey,backgroundColorKey,isBuiltIn,isArchived) VALUES (?,?,?,?,1,0)",
                arrayOf(category.id, category.name, category.iconKey, category.backgroundColorKey))
        }
    }

    val callback = object : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) { insert(db) }
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val sequence = db.query("SELECT seq FROM sqlite_sequence WHERE name='records'").use {
            if (it.moveToFirst()) it.getLong(0) else 0L
        }
        db.execSQL("CREATE TABLE IF NOT EXISTS categories (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, iconKey TEXT NOT NULL, backgroundColorKey TEXT NOT NULL, isBuiltIn INTEGER NOT NULL, isArchived INTEGER NOT NULL)")
        V2CategorySeeds.insert(db)
        db.execSQL("""CREATE TABLE records_v2 (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, type TEXT NOT NULL,
            amountMinor INTEGER NOT NULL, accountId INTEGER NOT NULL, destinationAccountId INTEGER,
            category TEXT, occurredAt TEXT NOT NULL, description TEXT NOT NULL, note TEXT NOT NULL,
            FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
            FOREIGN KEY(destinationAccountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
            FOREIGN KEY(category) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE RESTRICT
        )""".trimIndent())
        db.execSQL("INSERT INTO records_v2 (id,type,amountMinor,accountId,destinationAccountId,category,occurredAt,description,note) SELECT id,type,amountMinor,accountId,destinationAccountId,category,occurredAt,description,note FROM records")
        db.execSQL("DROP TABLE records")
        db.execSQL("ALTER TABLE records_v2 RENAME TO records")
        db.execSQL("UPDATE sqlite_sequence SET seq=MAX(seq,?) WHERE name='records'", arrayOf(sequence))
        listOf("accountId", "destinationAccountId", "occurredAt", "category").forEach {
            db.execSQL("CREATE INDEX index_records_$it ON records($it)")
        }
    }
}
