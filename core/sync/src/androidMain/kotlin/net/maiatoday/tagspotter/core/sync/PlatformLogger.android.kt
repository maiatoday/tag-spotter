package net.maiatoday.tagspotter.core.sync

import android.util.Log

actual fun platformLog(tag: String, message: String) {
    Log.d(tag, message)
}

actual fun platformLogError(tag: String, message: String, throwable: Throwable?) {
    Log.e(tag, message, throwable)
}
