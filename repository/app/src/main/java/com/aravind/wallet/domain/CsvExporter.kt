package com.aravind.wallet.domain

import java.io.Writer
import java.time.format.DateTimeFormatter

/** Export only: a single row per movement, with exact amounts and literal spreadsheet text. */
object CsvExporter {
    private val time = DateTimeFormatter.ofPattern("HH:mm:ss")
    fun write(ledger: Ledger, writer: Writer, type: RecordType? = null, accountIds: Set<Long>? = null): Int {
        writer.write("\uFEFFrecord_id,date,time,type,amount,currency,account,to_account,category,description\r\n")
        val records = ledger.records.filter { (type == null || it.type == type) && (accountIds == null || it.accountId in accountIds || it.destinationAccountId in accountIds) }
            .sortedWith(compareBy({ it.occurredAt }, { it.id }))
        records.forEach { record ->
            val fields = listOf(
                record.id.toString(), record.occurredAt.toLocalDate().toString(), record.occurredAt.format(time),
                record.type.name.lowercase(java.util.Locale.ROOT), Money.input(record.amountMinor), "INR",
                literal(ledger.accountsById.getValue(record.accountId).name),
                literal(record.destinationAccountId?.let { ledger.accountsById.getValue(it).name }.orEmpty()),
                literal(record.categoryId?.let { ledger.categoriesById.getValue(it).name }.orEmpty()),
                literal(record.description),
            )
            writer.write(fields.joinToString(",", transform = ::quote))
            writer.write("\r\n")
        }
        return records.size
    }

    private fun literal(value: String): String {
        val first = value.dropWhile(Char::isWhitespace).firstOrNull()
        return if (first in listOf('=', '+', '-', '@') || value.firstOrNull() in listOf('\t', '\r', '\n')) "'$value" else value
    }
    private fun quote(value: String): String = if (value.any { it == ',' || it == '"' || it == '\r' || it == '\n' })
        "\"${value.replace("\"", "\"\"")}\"" else value
}
