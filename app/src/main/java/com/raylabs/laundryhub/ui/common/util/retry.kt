package com.raylabs.laundryhub.ui.common.util

import android.util.Log
import kotlinx.coroutines.delay

suspend fun <T> retry(
    times: Int = 3,
    initialDelay: Long = 1000,
    maxDelay: Long = 5000,
    factor: Double = 2.0,
    onRetry: ((Int) -> Unit)? = null,
    block: suspend () -> T
): T? {
    var currentDelay = initialDelay
    repeat(times - 1) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            Log.e("retry", "retry: ${e.message}")
            println("⚠️ Error occurred, retrying in $currentDelay ms... (Attempt ${attempt + 1})")
            onRetry?.invoke(attempt + 1)
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
    }
    return try {
        block()
    } catch (e: Exception) {
        Log.e("retry", "retry: ${e.message}")
        null
    }
}
