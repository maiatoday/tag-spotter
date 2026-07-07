package net.maiatoday.tagspotter.core.sync

actual fun platformLog(tag: String, message: String) {
    println("[$tag] $message")
}

actual fun platformLogError(tag: String, message: String, throwable: Throwable?) {
    println("[$tag] ERROR: $message")
    throwable?.printStackTrace()
}
