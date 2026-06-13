package net.maiatoday.tagspotter.feature.detail.res

import androidx.compose.runtime.Composable

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.LOCAL_VARIABLE)
@Retention(AnnotationRetention.BINARY)
expect annotation class StringRes()

expect object DetailRes {
    val string: DetailStrings
}

expect object DetailStrings {
        val field_notes_header: Int
        val content_desc_wiki_ai_search: Int
        val no_notes_written: Int
        val write_note_label: Int
        val write_note_placeholder: Int
        val cancel: Int
        val save: Int
        val recognizer_prompt: Int
        val content_desc_voice_input: Int
        val content_desc_send_note: Int
        val speech_recognition_unsupported: Int
        val content_desc_edit_note: Int
        val content_desc_delete_note: Int
        val new_spot: Int
        val mark_active: Int
        val title_header: Int
        val content_desc_save_title: Int
        val content_desc_cancel_edit: Int
        val title_label: Int
        val title_placeholder: Int
        val content_desc_edit_title: Int
        val no_title_logged: Int
        val content_desc_search_lens: Int
        val content_desc_identify_ai: Int
        val content_desc_save_artists: Int
        val content_desc_add_artist: Int
        val content_desc_remove_artist: Int
        val content_desc_edit_artists: Int
        val photographer_header: Int
        val content_desc_save_photographer: Int
        val photographer_label: Int
        val photographer_placeholder_name: Int
        val content_desc_edit_photographer: Int
        val not_set: Int
        val content_desc_save_artwork_date: Int
        val content_desc_edit_artwork_date: Int
        val date_unknown: Int
        val btn_navigate: Int
        val btn_refine: Int
        val btn_map_it: Int
        val photo_timeline_header: Int
        val content_desc_add_image: Int
        val content_desc_delete_image: Int
        val content_desc_main_thumbnail: Int
        val content_desc_star_timeline: Int
        val tags_header: Int
        val content_desc_edit_tags: Int
        val content_desc_save_tags: Int
        val quick_select_tags: Int
        val recent_custom_tags: Int
        val add_custom_tag_label: Int
        val add_custom_tag_placeholder: Int
        val content_desc_add_custom_tag: Int
        val no_tags_added: Int
        val ai_suggestions_title: Int
        val ai_suggestions_intro: Int
        val suggested_title: Int
        val suggested_tags: Int
        val content_desc_deselect: Int
        val apply_selected: Int
        val err_key_missing_title: Int
        val err_key_missing_msg: Int
        val err_key_invalid_title: Int
        val err_key_invalid_msg: Int
        val err_quota_exceeded_title: Int
        val err_quota_exceeded_msg: Int
        val err_safety_blocked_title: Int
        val err_safety_blocked_msg: Int
        val err_generic_recognition_title: Int
        val ok: Int
        val wiki_searching_title: Int
        val wiki_add_link_title: Int
        val wiki_success_msg: Int
        val wiki_btn_add: Int
        val wiki_not_found_toast: Int
        val delete_photo_title: Int
        val delete_photo_msg: Int
        val delete_btn: Int
        val delete_note_title: Int
        val delete_note_msg: Int
        val save_spot: Int
        val gps_weak: Int
        val gps_verified: Int
        val update_coords_title: Int
        val update_coords_msg: Int
        val spot_location_marker: Int
        val location_updated_toast: Int
        val confirm_location_btn: Int
        val starred_limit_title: Int
        val starred_limit_msg: Int
        val toast_img_file_not_found: Int
        val toast_failed_share_img: Int
        val toast_no_img_search: Int
        val toast_no_img_analyze: Int
    }

@Composable
expect fun stringResource(id: Int): String

@Composable
expect fun stringResource(id: Int, vararg formatArgs: Any): String

interface ToastLauncher {
    fun showToast(message: String)
    fun showToast(id: Int)
}

@Composable
expect fun rememberToastLauncher(): ToastLauncher

expect fun formatImageModel(imagePath: String, thumbnailPath: String): Any
