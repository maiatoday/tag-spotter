package net.maiatoday.tagspotter.feature.map.res

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.LOCAL_VARIABLE)
@Retention(AnnotationRetention.BINARY)
actual annotation class StringRes

actual object R {
    actual object string {
        actual val filter_all: Int = 1
        actual val content_desc_clear_category_filter: Int = 2
        actual val search_query_chip_label: Int = 3
        actual val content_desc_clear_search_query: Int = 4
        actual val content_desc_clear_source_filter: Int = 5
        actual val content_desc_clear_location_filter: Int = 6
        actual val starred_only_filter_label: Int = 7
        actual val content_desc_clear_starred_filter: Int = 8
        actual val clear_all_filters: Int = 9
        actual val add_filters_btn: Int = 10
        actual val content_desc_starred_marker: Int = 11
        actual val no_description_added: Int = 12
        actual val view_history_notes: Int = 13
        actual val content_desc_close_details_overlay: Int = 14
    }
}

private val stringMap = mapOf(
    1 to "All",
    2 to "Clear category filter",
    3 to "Search: \u201c%1\$s\u201d",
    4 to "Clear search query",
    5 to "Clear source filter",
    6 to "Clear location filter",
    7 to "Starred Only",
    8 to "Clear starred filter",
    9 to "Clear All",
    10 to "+ Add Filters",
    11 to "Starred",
    12 to "No description added.",
    13 to "View History & Notes",
    14 to "Close details overlay"
)

@Composable
actual fun stringResource(id: Int): String {
    return stringMap[id] ?: "Unknown String"
}

@Composable
actual fun stringResource(id: Int, vararg formatArgs: Any): String {
    val template = stringMap[id] ?: return "Unknown String"
    return remember(template, formatArgs) {
        var result = template
        formatArgs.forEachIndexed { index, arg ->
            result = result.replace("%${index + 1}\$d", arg.toString())
                           .replace("%${index + 1}\$s", arg.toString())
        }
        result
    }
}

actual fun formatImageModel(imagePath: String, thumbnailPath: String): Any {
    return thumbnailPath.ifEmpty { imagePath }
}
