package net.maiatoday.tagspotter.feature.settings.res

import androidx.compose.runtime.Composable

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.LOCAL_VARIABLE)
@Retention(AnnotationRetention.BINARY)
actual annotation class StringRes

actual object R {
    actual val string: RStrings = RStrings
}

actual object RStrings {
    actual val settings_title: Int = 1
    actual val content_desc_back: Int = 2
    actual val profile_section_title: Int = 3
    actual val profile_section_desc: Int = 4
    actual val photographer_name_label: Int = 5
    actual val photographer_name_placeholder: Int = 6
    actual val map_preferences_title: Int = 7
    actual val map_preferences_desc: Int = 8
    actual val home_city_label: Int = 9
    actual val city_custom: Int = 10
    actual val latitude_label: Int = 11
    actual val latitude_placeholder: Int = 12
    actual val longitude_label: Int = 13
    actual val longitude_placeholder: Int = 14
    actual val darkmode_map_title: Int = 15
    actual val darkmode_map_desc: Int = 16
    actual val settings_saved_toast: Int = 17
    actual val invalid_coordinates_toast: Int = 18
    actual val save_settings_btn: Int = 19
    actual val artist_id_title: Int = 20
    actual val artist_id_desc: Int = 21
    actual val enable_recognition_title: Int = 22
    actual val ai_detection_title: Int = 23
    actual val ai_detection_desc: Int = 24
    actual val gemini_api_key_label: Int = 25
    actual val gemini_api_key_placeholder: Int = 26
    actual val content_desc_hide_api_key: Int = 27
    actual val content_desc_show_api_key: Int = 28
}

@Composable
actual fun stringResource(id: Int): String {
    return ""
}

@Composable
actual fun rememberToastLauncher(): ToastLauncher {
    return object : ToastLauncher {
        override fun showToast(message: String) {}
    }
}
