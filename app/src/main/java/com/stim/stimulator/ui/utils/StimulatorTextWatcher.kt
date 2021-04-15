package com.stim.stimulator.ui.utils

import android.text.Editable
import android.text.TextWatcher
import android.util.Log

class StimulatorTextWatcher(
    private val numberListener: ((number: Int) -> Unit)
) : TextWatcher {

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        Log.d(StimulatorTextWatcher::class.java.simpleName, "Before text change was invoked")
    }

    override fun afterTextChanged(s: Editable) {
        Log.d(StimulatorTextWatcher::class.java.simpleName, "After text change was invoked")
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        try {
            numberListener.invoke(s.toString().toInt())
        } catch (e: Exception) {
//            e.printStackTrace()
        }
    }
}