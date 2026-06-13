package net.maiatoday.tagspotter.feature.map.res

import androidx.compose.runtime.Composable

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.LOCAL_VARIABLE)
@Retention(AnnotationRetention.BINARY)
actual annotation class StringRes

actual object R {
    actual val string: RStrings = RStrings
}

actual object RStrings {
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

@Composable
actual fun stringResource(id: Int): String {
    return ""
}

@Composable
actual fun stringResource(id: Int, vararg formatArgs: Any): String {
    return ""
}

actual fun formatImageModel(imagePath: String, thumbnailPath: String): Any {
    return Unit
}
