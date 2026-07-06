package net.maiatoday.tagspotter.feature.detail.res

import platform.Foundation.*

actual object DateFormatter {
    actual fun formatDate(timestamp: Long, pattern: String): String {
        val date = NSDate.dateWithTimeIntervalSince1970(timestamp / 1000.0)
        val formatter = NSDateFormatter().apply {
            dateFormat = pattern
            locale = NSLocale.currentLocale
        }
        return formatter.stringFromDate(date)
    }
}
