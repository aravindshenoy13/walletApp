package com.aravind.wallet.domain
import com.aravind.wallet.data.WalletRecord
import java.time.LocalDateTime
import org.junit.Test
import org.junit.Assert.*
class RecordsFilterTest {
 private val expense=WalletRecord(id=1,type=RecordType.EXPENSE,amountMinor=100,accountId=1,categoryId="FOOD",occurredAt=LocalDateTime.of(2026,11,13,12,0),description="Lunch")
 private val transfer=expense.copy(id=2,type=RecordType.TRANSFER,categoryId=null,destinationAccountId=2)
 private val uncategorised=expense.copy(id=3,categoryId=null,accountId=2)
 private val records=listOf(expense,transfer,uncategorised)
 @Test fun combinedFiltersAndTransferEndpoints() {
  assertEquals(listOf(transfer,uncategorised),records.filterRecords(null,null,2))
  assertEquals(listOf(expense),records.filterRecords("2026-11-13","FOOD",1))
  assertEquals(listOf(uncategorised),records.filterRecords(null,"",0))
  assertTrue(records.filterRecords("2026-11-14",null,0).isEmpty())
  assertEquals(records,records.filterRecords(null,null,0))
 }
}
