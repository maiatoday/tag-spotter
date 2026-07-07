package net.maiatoday.tagspotter.core.sync

expect fun platformLog(tag: String, message: String)
expect fun platformLogError(tag: String, message: String, throwable: Throwable? = null)
