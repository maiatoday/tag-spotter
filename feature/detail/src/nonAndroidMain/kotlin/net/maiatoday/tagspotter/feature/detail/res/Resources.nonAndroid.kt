package net.maiatoday.tagspotter.feature.detail.res

import androidx.compose.runtime.Composable

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.LOCAL_VARIABLE)
@Retention(AnnotationRetention.BINARY)
actual annotation class StringRes

actual object R {
    actual val string: RStrings = RStrings
}

actual object RStrings {
    actual val field_notes_header: Int = 1
    actual val content_desc_wiki_ai_search: Int = 2
    actual val no_notes_written: Int = 3
    actual val write_note_label: Int = 4
    actual val write_note_placeholder: Int = 5
    actual val cancel: Int = 6
    actual val save: Int = 7
    actual val recognizer_prompt: Int = 8
    actual val content_desc_voice_input: Int = 9
    actual val content_desc_send_note: Int = 10
    actual val speech_recognition_unsupported: Int = 11
    actual val content_desc_edit_note: Int = 12
    actual val content_desc_delete_note: Int = 13
    actual val new_spot: Int = 14
    actual val mark_active: Int = 15
    actual val title_header: Int = 16
    actual val content_desc_save_title: Int = 17
    actual val content_desc_cancel_edit: Int = 18
    actual val title_label: Int = 19
    actual val title_placeholder: Int = 20
    actual val content_desc_edit_title: Int = 21
    actual val no_title_logged: Int = 22
    actual val content_desc_search_lens: Int = 23
    actual val content_desc_identify_ai: Int = 24
    actual val content_desc_save_artists: Int = 25
    actual val content_desc_add_artist: Int = 26
    actual val content_desc_remove_artist: Int = 27
    actual val content_desc_edit_artists: Int = 28
    actual val photographer_header: Int = 29
    actual val content_desc_save_photographer: Int = 30
    actual val photographer_label: Int = 31
    actual val photographer_placeholder_name: Int = 32
    actual val content_desc_edit_photographer: Int = 33
    actual val not_set: Int = 34
    actual val content_desc_save_artwork_date: Int = 35
    actual val content_desc_edit_artwork_date: Int = 36
    actual val date_unknown: Int = 37
    actual val btn_navigate: Int = 38
    actual val btn_refine: Int = 39
    actual val btn_map_it: Int = 40
    actual val photo_timeline_header: Int = 41
    actual val content_desc_add_image: Int = 42
    actual val content_desc_delete_image: Int = 43
    actual val content_desc_main_thumbnail: Int = 44
    actual val content_desc_star_timeline: Int = 45
    actual val tags_header: Int = 46
    actual val content_desc_edit_tags: Int = 47
    actual val content_desc_save_tags: Int = 48
    actual val quick_select_tags: Int = 49
    actual val recent_custom_tags: Int = 50
    actual val add_custom_tag_label: Int = 51
    actual val add_custom_tag_placeholder: Int = 52
    actual val content_desc_add_custom_tag: Int = 53
    actual val no_tags_added: Int = 54
    actual val ai_suggestions_title: Int = 55
    actual val ai_suggestions_intro: Int = 56
    actual val suggested_title: Int = 57
    actual val suggested_tags: Int = 58
    actual val content_desc_deselect: Int = 59
    actual val apply_selected: Int = 60
    actual val err_key_missing_title: Int = 61
    actual val err_key_missing_msg: Int = 62
    actual val err_key_invalid_title: Int = 63
    actual val err_key_invalid_msg: Int = 64
    actual val err_quota_exceeded_title: Int = 65
    actual val err_quota_exceeded_msg: Int = 66
    actual val err_safety_blocked_title: Int = 67
    actual val err_safety_blocked_msg: Int = 68
    actual val err_generic_recognition_title: Int = 69
    actual val ok: Int = 70
    actual val wiki_searching_title: Int = 71
    actual val wiki_add_link_title: Int = 72
    actual val wiki_success_msg: Int = 73
    actual val wiki_btn_add: Int = 74
    actual val wiki_not_found_toast: Int = 75
    actual val delete_photo_title: Int = 76
    actual val delete_photo_msg: Int = 77
    actual val delete_btn: Int = 78
    actual val delete_note_title: Int = 79
    actual val delete_note_msg: Int = 80
    actual val save_spot: Int = 81
    actual val gps_weak: Int = 82
    actual val gps_verified: Int = 83
    actual val update_coords_title: Int = 84
    actual val update_coords_msg: Int = 85
    actual val spot_location_marker: Int = 86
    actual val location_updated_toast: Int = 87
    actual val confirm_location_btn: Int = 88
    actual val starred_limit_title: Int = 89
    actual val starred_limit_msg: Int = 90
    actual val toast_img_file_not_found: Int = 91
    actual val toast_failed_share_img: Int = 92
    actual val toast_no_img_search: Int = 93
    actual val toast_no_img_analyze: Int = 94
}

@Composable
actual fun stringResource(id: Int): String {
    return ""
}

@Composable
actual fun stringResource(id: Int, vararg formatArgs: Any): String {
    return ""
}

@Composable
actual fun rememberToastLauncher(): ToastLauncher {
    return object : ToastLauncher {
        override fun showToast(message: String) {}
        override fun showToast(id: Int) {}
    }
}

actual fun formatImageModel(imagePath: String, thumbnailPath: String): Any {
    return Unit
}
