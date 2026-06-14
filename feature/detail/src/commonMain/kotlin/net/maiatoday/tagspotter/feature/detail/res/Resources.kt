package net.maiatoday.tagspotter.feature.detail.res

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import tagspotter.feature.detail.generated.resources.Res
import tagspotter.feature.detail.generated.resources.*

annotation class StringRes

object DetailRes {
    val string = DetailStrings
}

object DetailStrings {
    val field_notes_header: StringResource = Res.string.field_notes_header
    val content_desc_wiki_ai_search: StringResource = Res.string.content_desc_wiki_ai_search
    val no_notes_written: StringResource = Res.string.no_notes_written
    val write_note_label: StringResource = Res.string.write_note_label
    val write_note_placeholder: StringResource = Res.string.write_note_placeholder
    val cancel: StringResource = Res.string.cancel
    val save: StringResource = Res.string.save
    val recognizer_prompt: StringResource = Res.string.recognizer_prompt
    val content_desc_voice_input: StringResource = Res.string.content_desc_voice_input
    val content_desc_send_note: StringResource = Res.string.content_desc_send_note
    val speech_recognition_unsupported: StringResource = Res.string.speech_recognition_unsupported
    val content_desc_edit_note: StringResource = Res.string.content_desc_edit_note
    val content_desc_delete_note: StringResource = Res.string.content_desc_delete_note
    val new_spot: StringResource = Res.string.new_spot
    val mark_active: StringResource = Res.string.mark_active
    val title_header: StringResource = Res.string.title_header
    val content_desc_save_title: StringResource = Res.string.content_desc_save_title
    val content_desc_cancel_edit: StringResource = Res.string.content_desc_cancel_edit
    val title_label: StringResource = Res.string.title_label
    val title_placeholder: StringResource = Res.string.title_placeholder
    val content_desc_edit_title: StringResource = Res.string.content_desc_edit_title
    val no_title_logged: StringResource = Res.string.no_title_logged
    val content_desc_search_lens: StringResource = Res.string.content_desc_search_lens
    val content_desc_identify_ai: StringResource = Res.string.content_desc_identify_ai
    val content_desc_save_artists: StringResource = Res.string.content_desc_save_artists
    val content_desc_add_artist: StringResource = Res.string.content_desc_add_artist
    val content_desc_remove_artist: StringResource = Res.string.content_desc_remove_artist
    val content_desc_edit_artists: StringResource = Res.string.content_desc_edit_artists
    val photographer_header: StringResource = Res.string.photographer_header
    val content_desc_save_photographer: StringResource = Res.string.content_desc_save_photographer
    val photographer_label: StringResource = Res.string.photographer_label
    val photographer_placeholder_name: StringResource = Res.string.photographer_placeholder_name
    val content_desc_edit_photographer: StringResource = Res.string.content_desc_edit_photographer
    val not_set: StringResource = Res.string.not_set
    val content_desc_save_artwork_date: StringResource = Res.string.content_desc_save_artwork_date
    val content_desc_edit_artwork_date: StringResource = Res.string.content_desc_edit_artwork_date
    val date_unknown: StringResource = Res.string.date_unknown
    val btn_navigate: StringResource = Res.string.btn_navigate
    val btn_refine: StringResource = Res.string.btn_refine
    val btn_map_it: StringResource = Res.string.btn_map_it
    val photo_timeline_header: StringResource = Res.string.photo_timeline_header
    val content_desc_add_image: StringResource = Res.string.content_desc_add_image
    val content_desc_delete_image: StringResource = Res.string.content_desc_delete_image
    val content_desc_main_thumbnail: StringResource = Res.string.content_desc_main_thumbnail
    val content_desc_star_timeline: StringResource = Res.string.content_desc_star_timeline
    val tags_header: StringResource = Res.string.tags_header
    val content_desc_edit_tags: StringResource = Res.string.content_desc_edit_tags
    val content_desc_save_tags: StringResource = Res.string.content_desc_save_tags
    val quick_select_tags: StringResource = Res.string.quick_select_tags
    val recent_custom_tags: StringResource = Res.string.recent_custom_tags
    val add_custom_tag_label: StringResource = Res.string.add_custom_tag_label
    val add_custom_tag_placeholder: StringResource = Res.string.add_custom_tag_placeholder
    val content_desc_add_custom_tag: StringResource = Res.string.content_desc_add_custom_tag
    val no_tags_added: StringResource = Res.string.no_tags_added
    val ai_suggestions_title: StringResource = Res.string.ai_suggestions_title
    val ai_suggestions_intro: StringResource = Res.string.ai_suggestions_intro
    val suggested_title: StringResource = Res.string.suggested_title
    val suggested_tags: StringResource = Res.string.suggested_tags
    val content_desc_deselect: StringResource = Res.string.content_desc_deselect
    val apply_selected: StringResource = Res.string.apply_selected
    val err_key_missing_title: StringResource = Res.string.err_key_missing_title
    val err_key_missing_msg: StringResource = Res.string.err_key_missing_msg
    val err_key_invalid_title: StringResource = Res.string.err_key_invalid_title
    val err_key_invalid_msg: StringResource = Res.string.err_key_invalid_msg
    val err_quota_exceeded_title: StringResource = Res.string.err_quota_exceeded_title
    val err_quota_exceeded_msg: StringResource = Res.string.err_quota_exceeded_msg
    val err_safety_blocked_title: StringResource = Res.string.err_safety_blocked_title
    val err_safety_blocked_msg: StringResource = Res.string.err_safety_blocked_msg
    val err_generic_recognition_title: StringResource = Res.string.err_generic_recognition_title
    val ok: StringResource = Res.string.ok
    val wiki_searching_title: StringResource = Res.string.wiki_searching_title
    val wiki_add_link_title: StringResource = Res.string.wiki_add_link_title
    val wiki_success_msg: StringResource = Res.string.wiki_success_msg
    val wiki_btn_add: StringResource = Res.string.wiki_btn_add
    val wiki_not_found_toast: StringResource = Res.string.wiki_not_found_toast
    val delete_photo_title: StringResource = Res.string.delete_photo_title
    val delete_photo_msg: StringResource = Res.string.delete_photo_msg
    val delete_btn: StringResource = Res.string.delete_btn
    val delete_note_title: StringResource = Res.string.delete_note_title
    val delete_note_msg: StringResource = Res.string.delete_note_msg
    val save_spot: StringResource = Res.string.save_spot
    val gps_weak: StringResource = Res.string.gps_weak
    val gps_verified: StringResource = Res.string.gps_verified
    val update_coords_title: StringResource = Res.string.update_coords_title
    val update_coords_msg: StringResource = Res.string.update_coords_msg
    val spot_location_marker: StringResource = Res.string.spot_location_marker
    val location_updated_toast: StringResource = Res.string.location_updated_toast
    val confirm_location_btn: StringResource = Res.string.confirm_location_btn
    val starred_limit_title: StringResource = Res.string.starred_limit_title
    val starred_limit_msg: StringResource = Res.string.starred_limit_msg
    val toast_img_file_not_found: StringResource = Res.string.toast_img_file_not_found
    val toast_failed_share_img: StringResource = Res.string.toast_failed_share_img
    val toast_no_img_search: StringResource = Res.string.toast_no_img_search
    val toast_no_img_analyze: StringResource = Res.string.toast_no_img_analyze
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
    fun showToast(id: StringResource)
}

@Composable
expect fun rememberToastLauncher(): ToastLauncher

expect fun formatImageModel(imagePath: String, thumbnailPath: String): Any
