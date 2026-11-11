package com.aravind.wallet.ui

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.appcompat.widget.AppCompatEditText

class RecordsSearchField(context: Context, attrs: AttributeSet) : AppCompatEditText(context, attrs) {
    var dismiss: (() -> Unit)? = null
    override fun onKeyPreIme(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.action == KeyEvent.ACTION_UP) dismiss?.invoke()
            return true
        }
        return super.onKeyPreIme(keyCode, event)
    }
}
