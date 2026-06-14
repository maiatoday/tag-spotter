package net.maiatoday.tagspotter.feature.map.res

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import tagspotter.feature.map.generated.resources.Res
import tagspotter.feature.map.generated.resources.*

annotation class StringRes

object MapRes {
    val string = MapStrings
}

object MapStrings {
    val filter_all: StringResource = Res.string.filter_all
    val content_desc_clear_category_filter: StringResource = Res.string.content_desc_clear_category_filter
    val search_query_chip_label: StringResource = Res.string.search_query_chip_label
    val content_desc_clear_search_query: StringResource = Res.string.content_desc_clear_search_query
    val content_desc_clear_source_filter: StringResource = Res.string.content_desc_clear_source_filter
    val content_desc_clear_location_filter: StringResource = Res.string.content_desc_clear_location_filter
    val starred_only_filter_label: StringResource = Res.string.starred_only_filter_label
    val content_desc_clear_starred_filter: StringResource = Res.string.content_desc_clear_starred_filter
    val clear_all_filters: StringResource = Res.string.clear_all_filters
    val add_filters_btn: StringResource = Res.string.add_filters_btn
    val content_desc_starred_marker: StringResource = Res.string.content_desc_starred_marker
    val no_description_added: StringResource = Res.string.no_description_added
    val view_history_notes: StringResource = Res.string.view_history_notes
    val content_desc_close_details_overlay: StringResource = Res.string.content_desc_close_details_overlay
}

@Composable
fun stringResource(id: StringResource): String {
    return org.jetbrains.compose.resources.stringResource(id)
}

@Composable
fun stringResource(id: StringResource, vararg formatArgs: Any): String {
    return org.jetbrains.compose.resources.stringResource(id, *formatArgs)
}

expect fun formatImageModel(imagePath: String, thumbnailPath: String): Any
