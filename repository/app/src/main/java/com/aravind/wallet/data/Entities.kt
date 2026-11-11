package com.aravind.wallet.data

import androidx.room.Embedded
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import androidx.room.TypeConverter
import com.aravind.wallet.domain.RecordType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val openingBalanceMinor: Long,
)

@Entity(tableName = "categories")
data class WalletCategory(
    @PrimaryKey val id: String,
    val name: String,
    val iconKey: String,
    val backgroundColorKey: String,
    val isBuiltIn: Boolean = false,
    val isArchived: Boolean = false,
) {
    override fun toString() = name
}

@Entity(
    tableName = "records",
    foreignKeys = [
        ForeignKey(entity = Account::class, parentColumns = ["id"], childColumns = ["accountId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = Account::class, parentColumns = ["id"], childColumns = ["destinationAccountId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = WalletCategory::class, parentColumns = ["id"], childColumns = ["category"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("accountId"), Index("destinationAccountId"), Index("occurredAt"), Index("category")],
)
data class WalletRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: RecordType,
    val amountMinor: Long,
    val accountId: Long,
    val destinationAccountId: Long? = null,
    @ColumnInfo(name = "category") val categoryId: String? = null,
    // A local wall-clock date/time preserves exactly the occurrence entered by the user.
    val occurredAt: LocalDateTime,
    val description: String,
    val note: String = "",
)

data class AccountWithRecords(
    @Embedded val account: Account,
    @Relation(parentColumn = "id", entityColumn = "accountId") val records: List<WalletRecord>,
)

class Converters {
    private val timestamp = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss")
    @TypeConverter fun timeToText(value: LocalDateTime): String = value.format(timestamp)
    @TypeConverter fun textToTime(value: String): LocalDateTime = LocalDateTime.parse(value, timestamp)
    @TypeConverter fun typeToText(value: RecordType): String = value.name
    @TypeConverter fun textToType(value: String): RecordType = RecordType.valueOf(value)
}
