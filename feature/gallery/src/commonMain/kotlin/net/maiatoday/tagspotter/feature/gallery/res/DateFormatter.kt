package net.maiatoday.tagspotter.feature.gallery.res

expect object DateFormatter {
    fun formatDate(timestamp: Long, pattern: String): String
}
