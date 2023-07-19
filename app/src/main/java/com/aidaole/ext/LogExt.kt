package com.aidaole.ext

import android.util.Log

class LogExt

const val LOG_PREFIX = "OPUS_"

fun String.logi(tag: String) {
    Log.i("$LOG_PREFIX$tag", this)
}