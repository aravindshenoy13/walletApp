package com.aravind.wallet.domain

import com.aravind.wallet.data.WalletRecord

/** Account scope includes either end of a transfer. Empty category means Uncategorised. */
fun List<WalletRecord>.filterRecords(day: String?, category: String?, account: Long): List<WalletRecord> = filter { record ->
    (day == null || record.occurredAt.toLocalDate().toString() == day) &&
    (category == null || (record.type != RecordType.TRANSFER && record.categoryId.orEmpty() == category)) &&
    (account == 0L || record.accountId == account || record.destinationAccountId == account)
}
