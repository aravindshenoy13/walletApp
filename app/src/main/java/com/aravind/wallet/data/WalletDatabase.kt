package com.aravind.wallet.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.TypeConverters
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    // Observe both tables as a single consistent snapshot, including empty accounts.
    @Transaction
    @Query("SELECT * FROM accounts ORDER BY name COLLATE NOCASE, id")
    fun observeLedger(): Flow<List<AccountWithRecords>>

    @Query("SELECT * FROM accounts ORDER BY name COLLATE NOCASE, id")
    suspend fun accounts(): List<Account>

    @Insert suspend fun insertAccount(account: Account): Long
    @Update suspend fun updateAccount(account: Account): Int
    @Insert suspend fun insertRecord(record: WalletRecord): Long
    @Update suspend fun updateRecord(record: WalletRecord): Int
    @Query("DELETE FROM records WHERE id = :id") suspend fun deleteRecord(id: Long): Int
}

@Database(entities = [Account::class, WalletRecord::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class WalletDatabase : RoomDatabase() {
    abstract fun walletDao(): WalletDao

    companion object {
        fun create(context: Context): WalletDatabase = Room.databaseBuilder(
            context.applicationContext, WalletDatabase::class.java, "wallet.db",
        ).build()
    }
}
