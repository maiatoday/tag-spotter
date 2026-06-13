package net.maiatoday.tagspotter.feature.detail.res

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.LOCAL_VARIABLE)
@Retention(AnnotationRetention.BINARY)
actual annotation class StringRes

actual object R {
    actual object string {
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
}

private val stringMap = mapOf(
    1 to "FIELD NOTES",
    2 to "Search for links using AI",
    3 to "No notes written at this spot yet.",
    4 to "Write a note…",
    5 to "e.g. Tag faded, style details…",
    6 to "Cancel",
    7 to "Save",
    8 to "Speak now…",
    9 to "Voice input",
    10 to "Send note",
    11 to "Speech recognition is not supported on this device.",
    12 to "Edit note",
    13 to "Delete note",
    14 to "NEW SPOT",
    15 to "Mark Active",
    16 to "TITLE",
    17 to "Save title",
    18 to "Cancel edit",
    19 to "Title",
    20 to "e.g. Neon face stencil near Duomo",
    21 to "Edit title",
    22 to "No title logged.",
    23 to "Search artist with Lens",
    24 to "Identify artist with AI",
    25 to "Save artists",
    26 to "Add artist",
    27 to "Remove artist",
    28 to "Edit artists",
    29 to "PHOTOGRAPHER",
    30 to "Save photographer",
    31 to "Photographer",
    32 to "Enter name",
    33 to "Edit photographer",
    34 to "Not set",
    35 to "Save artwork date",
    36 to "Edit artwork date",
    37 to "Unknown",
    38 to "NAVIGATE",
    39 to "REFINE",
    40 to "MAP IT",
    41 to "PHOTO TIMELINE",
    42 to "Add image",
    43 to "Delete image",
    44 to "Main thumbnail",
    45 to "Star %1\$d",
    46 to "TAGS",
    47 to "Edit tags",
    48 to "Save tags",
    49 to "Quick Select Tags",
    50 to "Recent Custom Tags",
    51 to "Add Custom Tag",
    52 to "e.g. pasteup",
    53 to "Add custom tag",
    54 to "No tags added.",
    55 to "AI Recognition Suggestions",
    56 to "Gemini identified the following details from your photo. Select which ones to apply:",
    57 to "Suggested Title",
    58 to "Suggested Tags",
    59 to "Deselect",
    60 to "Apply Selected",
    61 to "Gemini API Key Missing",
    62 to "A Gemini API Key is required for in-app recognition. Please configure it in Settings.",
    63 to "Invalid API Key",
    64 to "The configured Gemini API Key is invalid or unauthorized. Please verify it in Settings.",
    65 to "API Quota Exceeded",
    66 to "You have exceeded your Gemini API limit. Please wait a while before trying again.",
    67 to "Safety Blocked",
    68 to "The image was flagged by Gemini’s safety filters and could not be analyzed.",
    69 to "Recognition Error",
    70 to "OK",
    71 to "Searching for Links…",
    72 to "Add Link",
    73 to "Found relevant Wikipedia page for “%1\$s”:\n\n%2\$s\n\nWould you like to add this to the field notes?",
    74 to "Add",
    75 to "No relevant Wikipedia page found.",
    76 to "Delete Photo?",
    77 to "Are you sure you want to delete this photo from the timeline? This action cannot be undone.",
    78 to "Delete",
    79 to "Delete Note?",
    80 to "Are you sure you want to delete this note? This action cannot be undone.",
    81 to "Save Spot",
    82 to "GPS Signal Weak",
    83 to "Verified GPS",
    84 to "Update Coordinates",
    85 to "Tap on the map to move the tag pin.",
    86 to "Spot Location",
    87 to "Location Updated!",
    88 to "Confirm Location",
    89 to "Starred Limit Reached",
    90 to "You can only star up to 100 spots due to geofencing limits. Please unstar some spots first.",
    91 to "Image file not found.",
    92 to "Failed to share image: %1\$s",
    93 to "No image available to search.",
    94 to "No image available to analyze."
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
                           .replace("%${index + 1}\$f", arg.toString())
                           .replace("%${index + 1}\$g", arg.toString())
                           .replace("%${index + 1}\$x", arg.toString())
                           .replace("%${index + 1}\$o", arg.toString())
                           // Support simpler placeholders too
                           .replace("%${index + 1}\$s", arg.toString())
                           .replace("%" + arg.toString(), arg.toString())
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
            override fun showToast(id: Int) {
                println("Toast: " + (stringMap[id] ?: "Unknown String"))
            }
        }
    }
}

actual fun formatImageModel(imagePath: String, thumbnailPath: String): Any {
    return thumbnailPath.ifEmpty { imagePath }
}
