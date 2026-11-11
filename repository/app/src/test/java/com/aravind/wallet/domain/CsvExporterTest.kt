package com.aravind.wallet.domain

import com.aravind.wallet.data.Account
import com.aravind.wallet.data.WalletCategory
import com.aravind.wallet.data.WalletRecord
import java.io.IOException
import java.io.StringWriter
import java.io.Writer
import java.time.LocalDateTime
import org.junit.Assert.*
import org.junit.Test

class CsvExporterTest {
    private val time = LocalDateTime.of(2026, 11, 11, 13, 30)
    private val accounts = listOf(Account(1, "Cash", 0), Account(2, "Bank", -100))
    private val categories = listOf(WalletCategory("FOOD", "Food", "utensils", "Peach", true, true))
    private fun entry(id: Long, type: RecordType = RecordType.EXPENSE) = WalletRecord(
        id, type, 25_050, 1, if (type == RecordType.TRANSFER) 2 else null,
        if (type == RecordType.TRANSFER) null else "FOOD", time, "Lunch", "Hidden legacy note",
    )

    @Test fun eachTypeExportsOnlyMatchingRowsAndEmptyMatchesKeepHeader() {
        val records = RecordType.entries.mapIndexed { index, type -> entry(index.toLong() + 1, type) }
        val ledger = Ledger(accounts, records, categories)
        RecordType.entries.forEach { type ->
            val output = StringWriter()
            assertEquals(1, CsvExporter.write(ledger, output, type))
            val rows = output.toString().split("\r\n").filter { it.isNotEmpty() }
            assertEquals(2, rows.size)
            assertEquals(type.name.lowercase(), rows[1].split(',')[3])
            val empty = StringWriter()
            assertEquals(0, CsvExporter.write(ledger.copy(records = records.filter { it.type != type }), empty, type))
            assertEquals(1, empty.toString().count { it == '\n' })
        }
        assertEquals(records, ledger.records)
    }

    @Test fun transferIsOneRowAndArchivedCategoryNamesAreExported() {
        val output = StringWriter()
        val count = CsvExporter.write(Ledger(accounts, listOf(entry(2, RecordType.TRANSFER), entry(1)), categories), output)
        assertEquals(2, count)
        assertEquals("\uFEFFrecord_id,date,time,type,amount,currency,account,to_account,category,description\r\n" +
            "1,2026-11-11,13:30:00,expense,250.50,INR,Cash,,Food,Lunch\r\n" +
            "2,2026-11-11,13:30:00,transfer,250.50,INR,Cash,Bank,,Lunch\r\n", output.toString())
        assertFalse(output.toString().contains("Hidden legacy note"))
    }

    @Test fun quotesUnicodeAndFormulaLikeTextRemainLiteralWithoutChangingData() {
        val record = entry(1).copy(description = "  =SUM(1,2)\n\"दोपहर\"")
        val category = categories.single().copy(name = "+Food")
        val output = StringWriter()
        CsvExporter.write(Ledger(listOf(Account(1, "@Cash", 0)), listOf(record), listOf(category)), output)
        assertTrue(output.toString().contains(",'@Cash,,'+Food,\"'  =SUM(1,2)\n\"\"दोपहर\"\"\"\r\n"))
        assertEquals("  =SUM(1,2)\n\"दोपहर\"", record.description)
        assertEquals("+Food", category.name)
    }

    @Test fun amountDoesNotRoundAndOccurrenceOrderIgnoresInputOrder() {
        val output = StringWriter()
        val later = entry(5).copy(amountMinor = Long.MAX_VALUE, occurredAt = time.plusDays(1))
        CsvExporter.write(Ledger(accounts, listOf(later, entry(3, RecordType.INCOME)), categories), output)
        val rows = output.toString().split("\r\n")
        assertTrue(rows[1].startsWith("3,2026-11-11,"))
        assertTrue(rows[2].contains(",92233720368547758.07,INR,"))
    }

    @Test fun emptyWalletExportsOnlyHeader() {
        val writer = StringWriter()
        assertEquals(0, CsvExporter.write(Ledger(emptyList(), emptyList()), writer))
        assertEquals(1, writer.toString().count { it == '\n' })
    }

    @Test fun optionalDescriptionExportsAnEmptyFinalColumn() {
        val writer = StringWriter()
        CsvExporter.write(Ledger(accounts, listOf(entry(1).copy(description = "")), categories), writer)
        assertTrue(writer.toString().endsWith(",Food,\r\n"))
    }

    @Test fun writeFailureReachesCallerInsteadOfReportingSuccess() {
        val broken = object : Writer() {
            override fun write(value: CharArray, offset: Int, count: Int) { throw IOException("Full") }
            override fun flush() = Unit
            override fun close() = Unit
        }
        assertThrows(IOException::class.java) { CsvExporter.write(Ledger(accounts, listOf(entry(1)), categories), broken) }
    }
}
