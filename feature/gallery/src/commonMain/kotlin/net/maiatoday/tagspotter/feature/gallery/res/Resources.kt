package net.maiatoday.tagspotter.feature.gallery.res

import androidx.compose.runtime.Composable

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.LOCAL_VARIABLE)
@Retention(AnnotationRetention.BINARY)
expect annotation class StringRes()

expect object R {
    object string {
        val delete_selected_spots_title: Int
        val delete_selected_spots_confirm: Int
        val delete: Int
        val cancel: Int
        val export_pack: Int
        val get_route_google_maps: Int
        val share_kml: Int
        val search_placeholder: Int
        val search_label: Int
        val starred_only: Int
        val starred_limit_reached_title: Int
        val starred_limit_reached_message: Int
        val ok: Int
        val export_pack_options_title: Int
        val export: Int
        val show_all_spots: Int
        val deleted_successfully: Int
        val spots_starred: Int
        val spots_unstarred: Int
        val no_app_available_route: Int
        val content_desc_toggle_star: Int
        val content_desc_export: Int
        val export_min_rating_help: Int
        val min_rating_label: Int
        val rating_all_photos: Int
        val rating_1_and_above: Int
        val rating_2_and_above: Int
        val rating_3_and_above: Int
        val rating_4_and_above: Int
        val rating_5_only: Int
        val content_desc_star_rating: Int
        val no_spots_found: Int
        val no_spots_range: Int
        val no_starred_spots: Int
        val no_spots_match_query: Int
        val first_spot_instruction: Int
        val empty_category_instruction: Int
    }
}

@Composable
expect fun stringResource(id: Int): String

@Composable
expect fun stringResource(id: Int, vararg formatArgs: Any): String

interface ToastLauncher {
    fun showToast(message: String)
}

@Composable
expect fun rememberToastLauncher(): ToastLauncher

expect fun formatImageModel(imagePath: String, thumbnailPath: String): Any
