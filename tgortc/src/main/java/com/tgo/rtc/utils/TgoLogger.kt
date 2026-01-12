package com.tgo.rtc.utils

import android.util.Log
import com.tgo.rtc.TgoRTC

object TgoLogger {
    private const val TAG = "TgoRTC"

    fun debug(msg: String) {
        if (TgoRTC.instance.options.debug) {
            Log.d(TAG, "[DEBUG] $msg")
        }
    }

    fun info(msg: String) {
        if (TgoRTC.instance.options.debug) {
            Log.i(TAG, "[INFO] $msg")
        }
    }

    fun error(msg: String) {
        if (TgoRTC.instance.options.debug) {
            Log.e(TAG, "[ERROR] $msg")
        }
    }

    fun error(msg: String, throwable: Throwable) {
        if (TgoRTC.instance.options.debug) {
            Log.e(TAG, "[ERROR] $msg", throwable)
        }
    }
}
