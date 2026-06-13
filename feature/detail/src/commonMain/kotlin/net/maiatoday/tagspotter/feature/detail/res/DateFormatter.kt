package net.maiatoday.tagspotter.feature.detail.res

expect object DateFormatter {
    fun formatDate(timestamp: Long, pattern: String): String
}
