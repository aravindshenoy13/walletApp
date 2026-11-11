package com.aravind.wallet.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.Update

@Dao
interface WalletDao {
    @Query("SELECT * FROM accounts ORDER BY name COLLATE NOCASE, id")
    suspend fun accounts(): List<Account>

    @Query("SELECT * FROM records ORDER BY occurredAt DESC, id DESC")
    suspend fun records(): List<WalletRecord>
    @Query("SELECT * FROM categories ORDER BY isArchived, name COLLATE NOCASE, id")
    suspend fun categories(): List<WalletCategory>
    @Query("SELECT * FROM records WHERE id=:id") suspend fun record(id: Long): WalletRecord?
    @Query("SELECT * FROM categories WHERE id=:id") suspend fun category(id: String): WalletCategory?
    @Insert suspend fun insertCategory(category: WalletCategory)
    @Update suspend fun updateCategory(category: WalletCategory): Int

    @Insert suspend fun insertAccount(account: Account): Long
    @Update suspend fun updateAccount(account: Account): Int
    @Insert suspend fun insertRecord(record: WalletRecord): Long
    @Update suspend fun updateRecord(record: WalletRecord): Int
    @Query("DELETE FROM records WHERE id = :id") suspend fun deleteRecord(id: Long): Int
}

@Database(entities = [Account::class, WalletRecord::class, WalletCategory::class], version = 2, exportSchema = true)
@TypeConverters(Converters::class)
abstract class WalletDatabase : RoomDatabase() {
    abstract fun walletDao(): WalletDao

    companion object {
        fun create(context: Context): WalletDatabase = Room.databaseBuilder(
            context.applicationContext, WalletDatabase::class.java, "wallet.db",
        ).addMigrations(MIGRATION_1_2).addCallback(V2CategorySeeds.callback).build()
    }
}
