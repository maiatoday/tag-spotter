package net.maiatoday.tagspotter.feature.gallery.res

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.LOCAL_VARIABLE)
@Retention(AnnotationRetention.BINARY)
actual annotation class StringRes

actual object R {
    actual object string {
        actual val delete_selected_spots_title: Int = 1
        actual val delete_selected_spots_confirm: Int = 2
        actual val delete: Int = 3
        actual val cancel: Int = 4
        actual val export_pack: Int = 5
        actual val get_route_google_maps: Int = 6
        actual val share_kml: Int = 7
        actual val search_placeholder: Int = 8
        actual val search_label: Int = 9
        actual val starred_only: Int = 10
        actual val starred_limit_reached_title: Int = 11
        actual val starred_limit_reached_message: Int = 12
        actual val ok: Int = 13
        actual val export_pack_options_title: Int = 14
        actual val export: Int = 15
        actual val show_all_spots: Int = 16
        actual val deleted_successfully: Int = 17
        actual val spots_starred: Int = 18
        actual val spots_unstarred: Int = 19
        actual val no_app_available_route: Int = 20
        actual val content_desc_toggle_star: Int = 21
        actual val content_desc_export: Int = 22
        actual val export_min_rating_help: Int = 23
        actual val min_rating_label: Int = 24
        actual val rating_all_photos: Int = 25
        actual val rating_1_and_above: Int = 26
        actual val rating_2_and_above: Int = 27
        actual val rating_3_and_above: Int = 28
        actual val rating_4_and_above: Int = 29
        actual val rating_5_only: Int = 30
        actual val content_desc_star_rating: Int = 31
        actual val no_spots_found: Int = 32
        actual val no_spots_range: Int = 33
        actual val no_starred_spots: Int = 34
        actual val no_spots_match_query: Int = 35
        actual val first_spot_instruction: Int = 36
        actual val empty_category_instruction: Int = 37
    }
}

private val stringMap = mapOf(
    1 to "Delete Selected Spots",
    2 to "Are you sure you want to delete the %1\$d selected spot(s)? This action cannot be undone and will delete all associated images and notes.",
    3 to "Delete",
    4 to "Cancel",
    5 to "Export TagSpotter Pack (.ts_pack)",
    6 to "Get Route in Google Maps",
    7 to "Share KML for Google My Maps",
    8 to "Search tags, artists, photographers…",
    9 to "Search: \u201c%1\$s\u201d",
    10 to "Starred Only",
    11 to "Starred Limit Reached",
    12 to "Adding these spots would exceed the limit of 100 starred spots. Please unstar some spots first.",
    13 to "OK",
    14 to "Export Pack Options",
    15 to "Export",
    16 to "Show All Spots",
    17 to "Deleted successfully",
    18 to "Spots starred!",
    19 to "Spots unstarred!",
    20 to "No app available to open route.",
    21 to "Toggle Star",
    22 to "Export",
    23 to "Filter images by minimum rating. Images below this rating will be excluded from the pack (the main hero image is always included).",
    24 to "Min Rating: ",
    25 to "All photos (no filter)",
    26 to "1 Star and above",
    27 to "2 Stars and above",
    28 to "3 Stars and above",
    29 to "4 Stars and above",
    30 to "5 Stars only",
    31 to "Star %1\$d",
    32 to "No Spots Found",
    33 to "No spots found within range of %1\$s.",
    34 to "No starred spots found.",
    35 to "No spots match \u2018%1\$s\u2019.",
    36 to "Document your city walks! Tap the \u2018Capture\u2019 tab below to photograph your first spot.",
    37 to "You haven\u2019t tagged any items in the \u2018%1\$s\u2019 category yet."
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

@Composable
actual fun rememberToastLauncher(): ToastLauncher {
    return remember {
        object : ToastLauncher {
            override fun showToast(message: String) {
                println("Toast: $message")
            }
        }
    }
}

actual fun formatImageModel(imagePath: String, thumbnailPath: String): Any {
    return thumbnailPath.ifEmpty { imagePath }
}
