package com.aravind.wallet

import android.app.Application
import com.aravind.wallet.data.WalletDatabase
import com.aravind.wallet.data.WalletRepository

class WalletApplication : Application() {
    private val database by lazy { WalletDatabase.create(this) }
    val repository by lazy { WalletRepository(database) }
}
