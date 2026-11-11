package com.aravind.wallet

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.aravind.wallet.data.WalletDatabase
import com.aravind.wallet.data.WalletRepository

class WalletApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
    private val database by lazy { WalletDatabase.create(this) }
    val repository by lazy { WalletRepository(database) }
}
