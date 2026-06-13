package net.maiatoday.tagspotter.feature.map.res

import androidx.compose.runtime.Composable
import androidx.core.net.toUri
import java.io.File

actual typealias StringRes = androidx.annotation.StringRes

actual object R {
    actual object string {
        actual val filter_all: Int = net.maiatoday.tagspotter.feature.map.R.string.filter_all
        actual val content_desc_clear_category_filter: Int = net.maiatoday.tagspotter.feature.map.R.string.content_desc_clear_category_filter
        actual val search_query_chip_label: Int = net.maiatoday.tagspotter.feature.map.R.string.search_query_chip_label
        actual val content_desc_clear_search_query: Int = net.maiatoday.tagspotter.feature.map.R.string.content_desc_clear_search_query
        actual val content_desc_clear_source_filter: Int = net.maiatoday.tagspotter.feature.map.R.string.content_desc_clear_source_filter
        actual val content_desc_clear_location_filter: Int = net.maiatoday.tagspotter.feature.map.R.string.content_desc_clear_location_filter
        actual val starred_only_filter_label: Int = net.maiatoday.tagspotter.feature.map.R.string.starred_only_filter_label
        actual val content_desc_clear_starred_filter: Int = net.maiatoday.tagspotter.feature.map.R.string.content_desc_clear_starred_filter
        actual val clear_all_filters: Int = net.maiatoday.tagspotter.feature.map.R.string.clear_all_filters
        actual val add_filters_btn: Int = net.maiatoday.tagspotter.feature.map.R.string.add_filters_btn
        actual val content_desc_starred_marker: Int = net.maiatoday.tagspotter.feature.map.R.string.content_desc_starred_marker
        actual val no_description_added: Int = net.maiatoday.tagspotter.feature.map.R.string.no_description_added
        actual val view_history_notes: Int = net.maiatoday.tagspotter.feature.map.R.string.view_history_notes
        actual val content_desc_close_details_overlay: Int = net.maiatoday.tagspotter.feature.map.R.string.content_desc_close_details_overlay
    }
}

@Composable
actual fun stringResource(id: Int): String {
    return androidx.compose.ui.res.stringResource(id)
}

@Composable
actual fun stringResource(id: Int, vararg formatArgs: Any): String {
    return androidx.compose.ui.res.stringResource(id, *formatArgs)
}

actual fun formatImageModel(imagePath: String, thumbnailPath: String): Any {
    return if (thumbnailPath.isNotEmpty() && !thumbnailPath.startsWith("android.resource://") && !thumbnailPath.startsWith("http")) {
        File(thumbnailPath)
    } else if (thumbnailPath.isNotEmpty() && (thumbnailPath.startsWith("android.resource://") || thumbnailPath.startsWith("http"))) {
        thumbnailPath.toUri()
    } else if (imagePath.startsWith("content://") || imagePath.startsWith("android.resource://") || imagePath.startsWith("http")) {
        imagePath.toUri()
    } else {
        File(imagePath)
    }
}
