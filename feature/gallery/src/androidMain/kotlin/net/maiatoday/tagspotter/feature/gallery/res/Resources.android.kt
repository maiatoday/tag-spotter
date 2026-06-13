package net.maiatoday.tagspotter.feature.gallery.res

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import java.io.File

actual typealias StringRes = androidx.annotation.StringRes

actual object GalleryStrings {
    actual val delete_selected_spots_title: Int = net.maiatoday.tagspotter.feature.gallery.R.string.delete_selected_spots_title
    actual val delete_selected_spots_confirm: Int = net.maiatoday.tagspotter.feature.gallery.R.string.delete_selected_spots_confirm
    actual val delete: Int = net.maiatoday.tagspotter.feature.gallery.R.string.delete
    actual val cancel: Int = net.maiatoday.tagspotter.feature.gallery.R.string.cancel
    actual val export_pack: Int = net.maiatoday.tagspotter.feature.gallery.R.string.export_pack
    actual val get_route_google_maps: Int = net.maiatoday.tagspotter.feature.gallery.R.string.get_route_google_maps
    actual val share_kml: Int = net.maiatoday.tagspotter.feature.gallery.R.string.share_kml
    actual val search_placeholder: Int = net.maiatoday.tagspotter.feature.gallery.R.string.search_placeholder
    actual val search_label: Int = net.maiatoday.tagspotter.feature.gallery.R.string.search_label
    actual val starred_only: Int = net.maiatoday.tagspotter.feature.gallery.R.string.starred_only
    actual val starred_limit_reached_title: Int = net.maiatoday.tagspotter.feature.gallery.R.string.starred_limit_reached_title
    actual val starred_limit_reached_message: Int = net.maiatoday.tagspotter.feature.gallery.R.string.starred_limit_reached_message
    actual val ok: Int = net.maiatoday.tagspotter.feature.gallery.R.string.ok
    actual val export_pack_options_title: Int = net.maiatoday.tagspotter.feature.gallery.R.string.export_pack_options_title
    actual val export: Int = net.maiatoday.tagspotter.feature.gallery.R.string.export
    actual val show_all_spots: Int = net.maiatoday.tagspotter.feature.gallery.R.string.show_all_spots
    actual val deleted_successfully: Int = net.maiatoday.tagspotter.feature.gallery.R.string.deleted_successfully
    actual val spots_starred: Int = net.maiatoday.tagspotter.feature.gallery.R.string.spots_starred
    actual val spots_unstarred: Int = net.maiatoday.tagspotter.feature.gallery.R.string.spots_unstarred
    actual val no_app_available_route: Int = net.maiatoday.tagspotter.feature.gallery.R.string.no_app_available_route
    actual val content_desc_toggle_star: Int = net.maiatoday.tagspotter.feature.gallery.R.string.content_desc_toggle_star
    actual val content_desc_export: Int = net.maiatoday.tagspotter.feature.gallery.R.string.content_desc_export
    actual val export_min_rating_help: Int = net.maiatoday.tagspotter.feature.gallery.R.string.export_min_rating_help
    actual val min_rating_label: Int = net.maiatoday.tagspotter.feature.gallery.R.string.min_rating_label
    actual val rating_all_photos: Int = net.maiatoday.tagspotter.feature.gallery.R.string.rating_all_photos
    actual val rating_1_and_above: Int = net.maiatoday.tagspotter.feature.gallery.R.string.rating_1_and_above
    actual val rating_2_and_above: Int = net.maiatoday.tagspotter.feature.gallery.R.string.rating_2_and_above
    actual val rating_3_and_above: Int = net.maiatoday.tagspotter.feature.gallery.R.string.rating_3_and_above
    actual val rating_4_and_above: Int = net.maiatoday.tagspotter.feature.gallery.R.string.rating_4_and_above
    actual val rating_5_only: Int = net.maiatoday.tagspotter.feature.gallery.R.string.rating_5_only
    actual val content_desc_star_rating: Int = net.maiatoday.tagspotter.feature.gallery.R.string.content_desc_star_rating
    actual val no_spots_found: Int = net.maiatoday.tagspotter.feature.gallery.R.string.no_spots_found
    actual val no_spots_range: Int = net.maiatoday.tagspotter.feature.gallery.R.string.no_spots_range
    actual val no_starred_spots: Int = net.maiatoday.tagspotter.feature.gallery.R.string.no_starred_spots
    actual val no_spots_match_query: Int = net.maiatoday.tagspotter.feature.gallery.R.string.no_spots_match_query
    actual val first_spot_instruction: Int = net.maiatoday.tagspotter.feature.gallery.R.string.first_spot_instruction
    actual val empty_category_instruction: Int = net.maiatoday.tagspotter.feature.gallery.R.string.empty_category_instruction
}

@Composable
actual fun stringResource(id: Int): String {
    return androidx.compose.ui.res.stringResource(id)
}

@Composable
actual fun stringResource(id: Int, vararg formatArgs: Any): String {
    return androidx.compose.ui.res.stringResource(id, *formatArgs)
}

@Composable
actual fun rememberToastLauncher(): ToastLauncher {
    val context = LocalContext.current
    return remember(context) {
        object : ToastLauncher {
            override fun showToast(message: String) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
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
