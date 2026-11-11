package com.aravind.wallet.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aravind.wallet.domain.RecordType
import java.math.BigInteger
import java.time.LocalDateTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @Test fun populatedV1UpgradePreservesDataAndDeletedIdSequence() = runBlocking {
        val name = "wallet-v1-v2-migration-test.db"
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(name)
        val schema = InstrumentationRegistry.getInstrumentation().context.assets
            .open("com.aravind.wallet.data.WalletDatabase/1.json").bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        val helper = FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name).callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    val entities = schema.getJSONArray("entities")
                    for (index in 0 until entities.length()) {
                        val entity = entities.getJSONObject(index)
                        db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
                        val indices = entity.optJSONArray("indices")
                        for (position in 0 until (indices?.length() ?: 0)) {
                            db.execSQL(indices!!.getJSONObject(position).getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
                        }
                    }
                    val setup = schema.getJSONArray("setupQueries")
                    for (index in 0 until setup.length()) db.execSQL(setup.getString(index))
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = error("V1 fixture only")
            }).build())
        helper.writableDatabase.apply {
            execSQL("INSERT INTO accounts (id,name,openingBalanceMinor) VALUES (1,'Cash',10000),(2,'Bank',-5000)")
            execSQL("INSERT INTO records (id,type,amountMinor,accountId,destinationAccountId,category,occurredAt,description,note) VALUES (1,'EXPENSE',1250,1,NULL,'FOOD','2026-11-10T08:45:00','Lunch','Legacy note'),(2,'INCOME',100,1,NULL,'FOOD','2026-11-10T09:00:00','Refund',''),(3,'TRANSFER',2500,1,2,NULL,'2026-11-10T10:00:00','ATM',''),(100,'EXPENSE',1,1,NULL,'OTHER','2026-11-10T11:00:00','Deleted','')")
            execSQL("DELETE FROM records WHERE id=100")
        }
        helper.close()
        // Opening the actual V2 database runs the migration and Room's generated schema validation.
        val database = Room.databaseBuilder(context, WalletDatabase::class.java, name)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3).addCallback(V2CategorySeeds.callback).build()
        try {
            val repository = WalletRepository(database)
            val ledger = repository.snapshot()
            assertEquals(12, ledger.categories.size)
            assertEquals(setOf(1L, 2L, 3L), ledger.records.map { it.id }.toSet())
            assertEquals(BigInteger.valueOf(6350), ledger.balances[1])
            assertEquals(BigInteger.valueOf(-2500), ledger.balances[2])
            val original = ledger.records.single { it.id == 1L }
            assertEquals("Legacy note", original.note)
            assertEquals(LocalDateTime.of(2026, 11, 10, 8, 45), original.occurredAt)
            assertEquals("FOOD", original.categoryId)
            assertEquals(null, ledger.records.single { it.id == 3L }.categoryId)
            val id = repository.saveRecord(original.copy(id = 0, type = RecordType.INCOME, amountMinor = 1))
            assertTrue("Deleted AUTOINCREMENT IDs must not be reused", id > 100)
            assertEquals(BigInteger.valueOf(6351), repository.snapshot().balances[1])
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }
    @Test fun populatedV2UpgradeDefaultsIconsAndPreservesAllData() = runBlocking {
        val name = "wallet-v2-v3-migration-test.db"
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(name)
        val schema = InstrumentationRegistry.getInstrumentation().context.assets
            .open("com.aravind.wallet.data.WalletDatabase/2.json").bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
        val helper = FrameworkSQLiteOpenHelperFactory().create(SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name).callback(object : SupportSQLiteOpenHelper.Callback(2) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    val entities = schema.getJSONArray("entities")
                    for (index in 0 until entities.length()) {
                        val entity = entities.getJSONObject(index)
                        db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
                        val indices = entity.optJSONArray("indices")
                        for (position in 0 until (indices?.length() ?: 0)) {
                            db.execSQL(indices!!.getJSONObject(position).getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
                        }
                    }
                    val setup = schema.getJSONArray("setupQueries")
                    for (index in 0 until setup.length()) db.execSQL(setup.getString(index))
                }
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = error("V2 fixture only")
            }).build())
        helper.writableDatabase.apply {
            V2CategorySeeds.insert(this)
            execSQL("INSERT INTO accounts (id,name,openingBalanceMinor) VALUES (1,'Cash',10000),(2,'Bank',-5000)")
            execSQL("INSERT INTO records (id,type,amountMinor,accountId,destinationAccountId,category,occurredAt,description,note) VALUES (1,'EXPENSE',1250,1,NULL,'FOOD','2026-11-10T08:45:00','Lunch','Legacy note'),(2,'INCOME',100,1,NULL,'FOOD','2026-11-10T09:00:00','Refund',''),(3,'TRANSFER',2500,1,2,NULL,'2026-11-10T10:00:00','ATM',''),(100,'EXPENSE',1,1,NULL,'OTHER','2026-11-10T11:00:00','Deleted','')")
            execSQL("DELETE FROM records WHERE id=100")
        }
        helper.close()
        // Opening the actual V2 database runs the migration and Room's generated schema validation.
        val database = Room.databaseBuilder(context, WalletDatabase::class.java, name)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3).addCallback(V2CategorySeeds.callback).build()
        try {
            val repository = WalletRepository(database)
            val ledger = repository.snapshot()
            assertEquals(12, ledger.categories.size)
            assertTrue(ledger.accounts.all { it.iconKey == "bank" })
            for (icon in listOf("bank", "cash", "card")) {
                repository.saveAccount(ledger.accounts.first().copy(iconKey = icon))
                assertEquals(icon, repository.snapshot().accounts.first { it.id == ledger.accounts.first().id }.iconKey)
            }
            assertEquals(setOf(1L, 2L, 3L), ledger.records.map { it.id }.toSet())
            assertEquals(BigInteger.valueOf(6350), ledger.balances[1])
            assertEquals(BigInteger.valueOf(-2500), ledger.balances[2])
            val original = ledger.records.single { it.id == 1L }
            assertEquals("Legacy note", original.note)
            assertEquals(LocalDateTime.of(2026, 11, 10, 8, 45), original.occurredAt)
            assertEquals("FOOD", original.categoryId)
            assertEquals(null, ledger.records.single { it.id == 3L }.categoryId)
            val id = repository.saveRecord(original.copy(id = 0, type = RecordType.INCOME, amountMinor = 1))
            assertTrue("Deleted AUTOINCREMENT IDs must not be reused", id > 100)
            assertEquals(BigInteger.valueOf(6351), repository.snapshot().balances[1])
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }
}
