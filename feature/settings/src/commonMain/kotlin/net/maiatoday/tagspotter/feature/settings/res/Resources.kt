package net.maiatoday.tagspotter.feature.settings.res

import androidx.compose.runtime.Composable

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.LOCAL_VARIABLE)
@Retention(AnnotationRetention.BINARY)
expect annotation class StringRes()

expect object SettingsRes {
    val string: SettingsStrings
}

expect object SettingsStrings {
    val settings_title: Int
    val content_desc_back: Int
    val profile_section_title: Int
    val profile_section_desc: Int
    val photographer_name_label: Int
    val photographer_name_placeholder: Int
    val map_preferences_title: Int
    val map_preferences_desc: Int
    val home_city_label: Int
    val city_custom: Int
    val latitude_label: Int
    val latitude_placeholder: Int
    val longitude_label: Int
    val longitude_placeholder: Int
    val darkmode_map_title: Int
    val darkmode_map_desc: Int
    val settings_saved_toast: Int
    val invalid_coordinates_toast: Int
    val save_settings_btn: Int
    val artist_id_title: Int
    val artist_id_desc: Int
    val enable_recognition_title: Int
    val ai_detection_title: Int
    val ai_detection_desc: Int
    val gemini_api_key_label: Int
    val gemini_api_key_placeholder: Int
    val content_desc_hide_api_key: Int
    val content_desc_show_api_key: Int
}

@Composable
expect fun stringResource(id: Int): String

interface ToastLauncher {
    fun showToast(message: String)
}

@Composable
expect fun rememberToastLauncher(): ToastLauncher
