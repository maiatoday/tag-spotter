package net.maiatoday.tagspotter.feature.gallery.res

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import tagspotter.feature.gallery.generated.resources.Res
import tagspotter.feature.gallery.generated.resources.*

annotation class StringRes

object GalleryRes {
    val string = GalleryStrings
}

object GalleryStrings {
    val delete_selected_spots_title: StringResource = Res.string.delete_selected_spots_title
    val delete_selected_spots_confirm: StringResource = Res.string.delete_selected_spots_confirm
    val delete: StringResource = Res.string.delete
    val cancel: StringResource = Res.string.cancel
    val export_pack: StringResource = Res.string.export_pack
    val get_route_google_maps: StringResource = Res.string.get_route_google_maps
    val share_kml: StringResource = Res.string.share_kml
    val search_placeholder: StringResource = Res.string.search_placeholder
    val search_label: StringResource = Res.string.search_label
    val starred_only: StringResource = Res.string.starred_only
    val starred_limit_reached_title: StringResource = Res.string.starred_limit_reached_title
    val starred_limit_reached_message: StringResource = Res.string.starred_limit_reached_message
    val ok: StringResource = Res.string.ok
    val export_pack_options_title: StringResource = Res.string.export_pack_options_title
    val export: StringResource = Res.string.export
    val show_all_spots: StringResource = Res.string.show_all_spots
    val deleted_successfully: StringResource = Res.string.deleted_successfully
    val spots_starred: StringResource = Res.string.spots_starred
    val spots_unstarred: StringResource = Res.string.spots_unstarred
    val no_app_available_route: StringResource = Res.string.no_app_available_route
    val content_desc_toggle_star: StringResource = Res.string.content_desc_toggle_star
    val content_desc_export: StringResource = Res.string.content_desc_export
    val export_min_rating_help: StringResource = Res.string.export_min_rating_help
    val min_rating_label: StringResource = Res.string.min_rating_label
    val rating_all_photos: StringResource = Res.string.rating_all_photos
    val rating_1_and_above: StringResource = Res.string.rating_1_and_above
    val rating_2_and_above: StringResource = Res.string.rating_2_and_above
    val rating_3_and_above: StringResource = Res.string.rating_3_and_above
    val rating_4_and_above: StringResource = Res.string.rating_4_and_above
    val rating_5_only: StringResource = Res.string.rating_5_only
    val content_desc_star_rating: StringResource = Res.string.content_desc_star_rating
    val no_spots_found: StringResource = Res.string.no_spots_found
    val no_spots_range: StringResource = Res.string.no_spots_range
    val no_starred_spots: StringResource = Res.string.no_starred_spots
    val no_spots_match_query: StringResource = Res.string.no_spots_match_query
    val first_spot_instruction: StringResource = Res.string.first_spot_instruction
    val empty_category_instruction: StringResource = Res.string.empty_category_instruction
    val select_all: StringResource = Res.string.select_all
    val deselect_all: StringResource = Res.string.deselect_all
    val bulk_actions: StringResource = Res.string.bulk_actions
    val star_all_filtered: StringResource = Res.string.star_all_filtered
    val unstar_all_filtered: StringResource = Res.string.unstar_all_filtered
    val export_all_filtered: StringResource = Res.string.export_all_filtered
    val delete_all_filtered: StringResource = Res.string.delete_all_filtered
    val delete_filtered_spots_title: StringResource = Res.string.delete_filtered_spots_title
    val delete_filtered_spots_confirm: StringResource = Res.string.delete_filtered_spots_confirm
}

@Composable
fun stringResource(id: StringResource): String {
    return org.jetbrains.compose.resources.stringResource(id)
}

@Composable
fun stringResource(id: StringResource, vararg formatArgs: Any): String {
    return org.jetbrains.compose.resources.stringResource(id, *formatArgs)
}

interface ToastLauncher {
    fun showToast(message: String)
}

@Composable
expect fun rememberToastLauncher(): ToastLauncher

expect fun formatImageModel(imagePath: String, thumbnailPath: String): Any
