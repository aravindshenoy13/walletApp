package com.aravind.wallet.data

import com.aravind.wallet.domain.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime

/** Versioned JSON; minor units and IDs are strings to retain full 64-bit precision. */
object WalletBackup {
    fun encode(ledger: Ledger): String = JSONObject().apply {
        put("format", "wallet-full-backup"); put("version", 1); put("currency", "INR")
        put("accounts", JSONArray().apply { ledger.accounts.forEach { a -> put(JSONObject().apply {
            put("id", a.id.toString()); put("name", a.name); put("opening", a.openingBalanceMinor.toString())
        }) } })
        put("categories", JSONArray().apply { ledger.categories.forEach { c -> put(JSONObject().apply {
            put("id", c.id); put("name", c.name); put("icon", c.iconKey); put("color", c.backgroundColorKey)
            put("builtIn", c.isBuiltIn); put("archived", c.isArchived)
        }) } })
        put("records", JSONArray().apply { ledger.records.forEach { r -> put(JSONObject().apply {
            put("id", r.id.toString()); put("type", r.type.name); put("amount", r.amountMinor.toString())
            put("account", r.accountId.toString()); put("destination", r.destinationAccountId?.toString() ?: JSONObject.NULL)
            put("category", r.categoryId ?: JSONObject.NULL); put("occurredAt", r.occurredAt.toString())
            put("description", r.description); put("note", r.note)
        }) } })
    }.toString(2)

    fun decode(text: String): Ledger {
        val root = JSONObject(text)
        require(root.getString("format") == "wallet-full-backup" && root.getInt("version") == 1 && root.getString("currency") == "INR") { "Unsupported backup format." }
        fun rows(key: String): List<JSONObject> = root.getJSONArray(key).let { a -> (0 until a.length()).map(a::getJSONObject) }
        val accounts = rows("accounts").map { Account(it.getString("id").toLong(), it.getString("name"), it.getString("opening").toLong()) }
        val categories = rows("categories").map { WalletCategory(it.getString("id"), it.getString("name"), it.getString("icon"), it.getString("color"), it.getBoolean("builtIn"), it.getBoolean("archived")) }
        val records = rows("records").map { WalletRecord(it.getString("id").toLong(), RecordType.valueOf(it.getString("type")), it.getString("amount").toLong(), it.getString("account").toLong(),
            if (it.isNull("destination")) null else it.getString("destination").toLong(), if (it.isNull("category")) null else it.getString("category"),
            LocalDateTime.parse(it.getString("occurredAt")), it.getString("description"), it.getString("note")) }
        require(accounts.all { it.id > 0 && it.name.isNotBlank() && it.name.length <= 80 }) { "Invalid account." }
        require(accounts.map { it.id }.distinct().size == accounts.size && accounts.map { it.name.lowercase(java.util.Locale.ROOT) }.distinct().size == accounts.size) { "Duplicate accounts." }
        require(categories.isNotEmpty() && categories.any { !it.isArchived }) { "Backup must contain active categories." }
        require(categories.all { c -> c.id.isNotBlank() && c.name.isNotBlank() && c.name.length <= 80 && CategoryCatalog.icons.any { it.key == c.iconKey } && CategoryCatalog.colors.any { it.key == c.backgroundColorKey } }) { "Invalid category." }
        require(categories.map { it.id }.distinct().size == categories.size && categories.map { it.name.lowercase(java.util.Locale.ROOT) }.distinct().size == categories.size) { "Duplicate categories." }
        require(records.map { it.id }.distinct().size == records.size) { "Duplicate record IDs." }
        val accountIds = accounts.map { it.id }.toSet(); val categoryIds = categories.map { it.id }.toSet()
        require(records.all { r -> r.id > 0 && r.amountMinor > 0 && r.accountId in accountIds && r.occurredAt.year in 1..9999 && r.description.length <= 120 && r.note.length <= 1000 &&
            if (r.type == RecordType.TRANSFER) r.destinationAccountId in accountIds && r.destinationAccountId != r.accountId && r.categoryId == null
            else r.destinationAccountId == null && r.categoryId in categoryIds }) { "Invalid transaction or missing relationship." }
        return Ledger(accounts, records, categories)
    }
}
