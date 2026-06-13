package net.maiatoday.tagspotter.feature.map.res

import androidx.compose.runtime.Composable

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.LOCAL_VARIABLE)
@Retention(AnnotationRetention.BINARY)
expect annotation class StringRes()

expect object R {
    object string {
        val filter_all: Int
        val content_desc_clear_category_filter: Int
        val search_query_chip_label: Int
        val content_desc_clear_search_query: Int
        val content_desc_clear_source_filter: Int
        val content_desc_clear_location_filter: Int
        val starred_only_filter_label: Int
        val content_desc_clear_starred_filter: Int
        val clear_all_filters: Int
        val add_filters_btn: Int
        val content_desc_starred_marker: Int
        val no_description_added: Int
        val view_history_notes: Int
        val content_desc_close_details_overlay: Int
    }
}

@Composable
expect fun stringResource(id: Int): String

@Composable
expect fun stringResource(id: Int, vararg formatArgs: Any): String

expect fun formatImageModel(imagePath: String, thumbnailPath: String): Any
