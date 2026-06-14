package net.maiatoday.tagspotter.feature.settings.res

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import tagspotter.feature.settings.generated.resources.Res
import tagspotter.feature.settings.generated.resources.*

annotation class StringRes

object SettingsRes {
    val string = SettingsStrings
}

object SettingsStrings {
    val settings_title: StringResource = Res.string.settings_title
    val content_desc_back: StringResource = Res.string.content_desc_back
    val profile_section_title: StringResource = Res.string.profile_section_title
    val profile_section_desc: StringResource = Res.string.profile_section_desc
    val photographer_name_label: StringResource = Res.string.photographer_name_label
    val photographer_name_placeholder: StringResource = Res.string.photographer_name_placeholder
    val map_preferences_title: StringResource = Res.string.map_preferences_title
    val map_preferences_desc: StringResource = Res.string.map_preferences_desc
    val home_city_label: StringResource = Res.string.home_city_label
    val city_custom: StringResource = Res.string.city_custom
    val latitude_label: StringResource = Res.string.latitude_label
    val latitude_placeholder: StringResource = Res.string.latitude_placeholder
    val longitude_label: StringResource = Res.string.longitude_label
    val longitude_placeholder: StringResource = Res.string.longitude_placeholder
    val darkmode_map_title: StringResource = Res.string.darkmode_map_title
    val darkmode_map_desc: StringResource = Res.string.darkmode_map_desc
    val settings_saved_toast: StringResource = Res.string.settings_saved_toast
    val invalid_coordinates_toast: StringResource = Res.string.invalid_coordinates_toast
    val save_settings_btn: StringResource = Res.string.save_settings_btn
    val artist_id_title: StringResource = Res.string.artist_id_title
    val artist_id_desc: StringResource = Res.string.artist_id_desc
    val enable_recognition_title: StringResource = Res.string.enable_recognition_title
    val ai_detection_title: StringResource = Res.string.ai_detection_title
    val ai_detection_desc: StringResource = Res.string.ai_detection_desc
    val gemini_api_key_label: StringResource = Res.string.gemini_api_key_label
    val gemini_api_key_placeholder: StringResource = Res.string.gemini_api_key_placeholder
    val content_desc_hide_api_key: StringResource = Res.string.content_desc_hide_api_key
    val content_desc_show_api_key: StringResource = Res.string.content_desc_show_api_key
}

@Composable
fun stringResource(id: StringResource): String {
    return org.jetbrains.compose.resources.stringResource(id)
}

interface ToastLauncher {
    fun showToast(message: String)
}

@Composable
expect fun rememberToastLauncher(): ToastLauncher
