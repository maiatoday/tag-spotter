package net.maiatoday.tagspotter.feature.detail.res

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import java.io.File

actual typealias StringRes = androidx.annotation.StringRes

actual object DetailRes {
    actual val string: DetailStrings = DetailStrings
}

actual object DetailStrings {
    actual val field_notes_header: Int = net.maiatoday.tagspotter.feature.detail.R.string.field_notes_header
    actual val content_desc_wiki_ai_search: Int = net.maiatoday.tagspotter.feature.detail.R.string.content_desc_wiki_ai_search
    actual val no_notes_written: Int = net.maiatoday.tagspotter.feature.detail.R.string.no_notes_written
    actual val write_note_label: Int = net.maiatoday.tagspotter.feature.detail.R.string.write_note_label
    actual val write_note_placeholder: Int = net.maiatoday.tagspotter.feature.detail.R.string.write_note_placeholder
    actual val cancel: Int = net.maiatoday.tagspotter.feature.detail.R.string.cancel
    actual val save: Int = net.maiatoday.tagspotter.feature.detail.R.string.save
    actual val recognizer_prompt: Int = net.maiatoday.tagspotter.feature.detail.R.string.recognizer_prompt
    actual val content_desc_voice_input: Int = net.maiatoday.tagspotter.feature.detail.R.string.content_desc_voice_input
    actual val content_desc_send_note: Int = net.maiatoday.tagspotter.feature.detail.R.string.content_desc_send_note
    actual val speech_recognition_unsupported: Int = net.maiatoday.tagspotter.feature.detail.R.string.speech_recognition_unsupported
    actual val content_desc_edit_note: Int = net.maiatoday.tagspotter.feature.detail.R.string.content_desc_edit_note
    actual val content_desc_delete_note: Int = net.maiatoday.tagspotter.feature.detail.R.string.content_desc_delete_note
    actual val new_spot: Int = net.maiatoday.tagspotter.feature.detail.R.string.new_spot
    actual val mark_active: Int = net.maiatoday.tagspotter.feature.detail.R.string.mark_active
    actual val title_header: Int = net.maiatoday.tagspotter.feature.detail.R.string.title_header
    actual val content_desc_save_title: Int = net.maiatoday.tagspotter.feature.detail.R.string.content_desc_save_title
    actual val content_desc_cancel_edit: Int = net.maiatoday.tagspotter.feature.detail.R.string.content_desc_cancel_edit
    actual val title_label: Int = net.maiatoday.tagspotter.feature.detail.R.string.title_label
    actual val title_placeholder: Int = net.maiatoday.tagspotter.feature.detail.R.string.title_placeholder
    actual val content_desc_edit_title: Int = net.maiatoday.tagspotter.feature.detail.R.string.content_desc_edit_title
    actual val no_title_logged: Int = net.maiatoday.tagspotter.feature.detail.R.string.no_title_logged
    actual val content_desc_search_lens: Int = net.maiatoday.tagspotter.feature.detail.R.string.content_desc_search_lens
    actual val content_desc_identify_ai: Int = net.maiatoday.tagspotter.feature.detail.R.string.content_desc_identify_ai
    actual val content_desc_save_artists: Int = net.maiatoday.tagspotter.feature.detail.R.string.content_desc_save_artists
    actual val content_desc_add_artist: Int = net.maiatoday.tagspotter.feature.detail.R.string.content_desc_add_artist
    actual val content_desc_remove_artist: Int = net.maiatoday.tagspotter.feature.detail.R.string.content_desc_remove_artist
    actual val content_desc_edit_artists: Int = net.maiatoday.tagspotter.feature.detail.R.string.content_desc_edit_artists
    actual val photographer_header: Int = net.maiatoday.tagspotter.feature.detail.R.string.photographer_header
    actual val content_desc_save_photographer: Int = net.maiatoday.tagspotter.feature.detail.R.string.content_desc_save_photographer
    actual val photographer_label: Int = net.maiatoday.tagspotter.feature.detail.R.string.photographer_label
    actual val photographer_placeholder_name: Int = net.maiatoday.tagspotter.feature.detail.R.string.photographer_placeholder_name
    actual val content_desc_edit_photographer: Int = net.maiatoday.tagspotter.feature.detail.R.string.content_desc_edit_photographer
    actual val not_set: Int = net.maiatoday.tagspotter.feature.detail.R.string.not_set
    actual val content_desc_save_artwork_date: Int = net.maiatoday.tagspotter.feature.detail.R.string.content_desc_save_artwork_date
    actual val content_desc_edit_artwork_date: Int = net.maiatoday.tagspotter.feature.detail.R.string.content_desc_edit_artwork_date
    actual val date_unknown: Int = net.maiatoday.tagspotter.feature.detail.R.string.date_unknown
    actual val btn_navigate: Int = net.maiatoday.tagspotter.feature.detail.R.string.btn_navigate
    actual val btn_refine: Int = net.maiatoday.tagspotter.feature.detail.R.string.btn_refine
    actual val btn_map_it: Int = net.maiatoday.tagspotter.feature.detail.R.string.btn_map_it
    actual val photo_timeline_header: Int = net.maiatoday.tagspotter.feature.detail.R.string.photo_timeline_header
    actual val content_desc_add_image: Int = net.maiatoday.tagspotter.feature.detail.R.string.content_desc_add_image
    actual val content_desc_delete_image: Int = net.maiatoday.tagspotter.feature.detail.R.string.content_desc_delete_image
    actual val content_desc_main_thumbnail: Int = net.maiatoday.tagspotter.feature.detail.R.string.content_desc_main_thumbnail
    actual val content_desc_star_timeline: Int = net.maiatoday.tagspotter.feature.detail.R.string.content_desc_star_timeline
    actual val tags_header: Int = net.maiatoday.tagspotter.feature.detail.R.string.tags_header
    actual val content_desc_edit_tags: Int = net.maiatoday.tagspotter.feature.detail.R.string.content_desc_edit_tags
    actual val content_desc_save_tags: Int = net.maiatoday.tagspotter.feature.detail.R.string.content_desc_save_tags
    actual val quick_select_tags: Int = net.maiatoday.tagspotter.feature.detail.R.string.quick_select_tags
    actual val recent_custom_tags: Int = net.maiatoday.tagspotter.feature.detail.R.string.recent_custom_tags
    actual val add_custom_tag_label: Int = net.maiatoday.tagspotter.feature.detail.R.string.add_custom_tag_label
    actual val add_custom_tag_placeholder: Int = net.maiatoday.tagspotter.feature.detail.R.string.add_custom_tag_placeholder
    actual val content_desc_add_custom_tag: Int = net.maiatoday.tagspotter.feature.detail.R.string.content_desc_add_custom_tag
    actual val no_tags_added: Int = net.maiatoday.tagspotter.feature.detail.R.string.no_tags_added
    actual val ai_suggestions_title: Int = net.maiatoday.tagspotter.feature.detail.R.string.ai_suggestions_title
    actual val ai_suggestions_intro: Int = net.maiatoday.tagspotter.feature.detail.R.string.ai_suggestions_intro
    actual val suggested_title: Int = net.maiatoday.tagspotter.feature.detail.R.string.suggested_title
    actual val suggested_tags: Int = net.maiatoday.tagspotter.feature.detail.R.string.suggested_tags
    actual val content_desc_deselect: Int = net.maiatoday.tagspotter.feature.detail.R.string.content_desc_deselect
    actual val apply_selected: Int = net.maiatoday.tagspotter.feature.detail.R.string.apply_selected
    actual val err_key_missing_title: Int = net.maiatoday.tagspotter.feature.detail.R.string.err_key_missing_title
    actual val err_key_missing_msg: Int = net.maiatoday.tagspotter.feature.detail.R.string.err_key_missing_msg
    actual val err_key_invalid_title: Int = net.maiatoday.tagspotter.feature.detail.R.string.err_key_invalid_title
    actual val err_key_invalid_msg: Int = net.maiatoday.tagspotter.feature.detail.R.string.err_key_invalid_msg
    actual val err_quota_exceeded_title: Int = net.maiatoday.tagspotter.feature.detail.R.string.err_quota_exceeded_title
    actual val err_quota_exceeded_msg: Int = net.maiatoday.tagspotter.feature.detail.R.string.err_quota_exceeded_msg
    actual val err_safety_blocked_title: Int = net.maiatoday.tagspotter.feature.detail.R.string.err_safety_blocked_title
    actual val err_safety_blocked_msg: Int = net.maiatoday.tagspotter.feature.detail.R.string.err_safety_blocked_msg
    actual val err_generic_recognition_title: Int = net.maiatoday.tagspotter.feature.detail.R.string.err_generic_recognition_title
    actual val ok: Int = net.maiatoday.tagspotter.feature.detail.R.string.ok
    actual val wiki_searching_title: Int = net.maiatoday.tagspotter.feature.detail.R.string.wiki_searching_title
    actual val wiki_add_link_title: Int = net.maiatoday.tagspotter.feature.detail.R.string.wiki_add_link_title
    actual val wiki_success_msg: Int = net.maiatoday.tagspotter.feature.detail.R.string.wiki_success_msg
    actual val wiki_btn_add: Int = net.maiatoday.tagspotter.feature.detail.R.string.wiki_btn_add
    actual val wiki_not_found_toast: Int = net.maiatoday.tagspotter.feature.detail.R.string.wiki_not_found_toast
    actual val delete_photo_title: Int = net.maiatoday.tagspotter.feature.detail.R.string.delete_photo_title
    actual val delete_photo_msg: Int = net.maiatoday.tagspotter.feature.detail.R.string.delete_photo_msg
    actual val delete_btn: Int = net.maiatoday.tagspotter.feature.detail.R.string.delete_btn
    actual val delete_note_title: Int = net.maiatoday.tagspotter.feature.detail.R.string.delete_note_title
    actual val delete_note_msg: Int = net.maiatoday.tagspotter.feature.detail.R.string.delete_note_msg
    actual val save_spot: Int = net.maiatoday.tagspotter.feature.detail.R.string.save_spot
    actual val gps_weak: Int = net.maiatoday.tagspotter.feature.detail.R.string.gps_weak
    actual val gps_verified: Int = net.maiatoday.tagspotter.feature.detail.R.string.gps_verified
    actual val update_coords_title: Int = net.maiatoday.tagspotter.feature.detail.R.string.update_coords_title
    actual val update_coords_msg: Int = net.maiatoday.tagspotter.feature.detail.R.string.update_coords_msg
    actual val spot_location_marker: Int = net.maiatoday.tagspotter.feature.detail.R.string.spot_location_marker
    actual val location_updated_toast: Int = net.maiatoday.tagspotter.feature.detail.R.string.location_updated_toast
    actual val confirm_location_btn: Int = net.maiatoday.tagspotter.feature.detail.R.string.confirm_location_btn
    actual val starred_limit_title: Int = net.maiatoday.tagspotter.feature.detail.R.string.starred_limit_title
    actual val starred_limit_msg: Int = net.maiatoday.tagspotter.feature.detail.R.string.starred_limit_msg
    actual val toast_img_file_not_found: Int = net.maiatoday.tagspotter.feature.detail.R.string.toast_img_file_not_found
    actual val toast_failed_share_img: Int = net.maiatoday.tagspotter.feature.detail.R.string.toast_failed_share_img
    actual val toast_no_img_search: Int = net.maiatoday.tagspotter.feature.detail.R.string.toast_no_img_search
    actual val toast_no_img_analyze: Int = net.maiatoday.tagspotter.feature.detail.R.string.toast_no_img_analyze
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
            override fun showToast(id: Int) {
                Toast.makeText(context, id, Toast.LENGTH_SHORT).show()
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
