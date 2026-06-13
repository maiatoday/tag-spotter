package net.maiatoday.tagspotter.feature.settings.res

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

actual typealias StringRes = androidx.annotation.StringRes

actual object SettingsRes {
    actual val string: SettingsStrings = SettingsStrings
}

actual object SettingsStrings {
    actual val settings_title: Int = net.maiatoday.tagspotter.feature.settings.R.string.settings_title
    actual val content_desc_back: Int = net.maiatoday.tagspotter.feature.settings.R.string.content_desc_back
    actual val profile_section_title: Int = net.maiatoday.tagspotter.feature.settings.R.string.profile_section_title
    actual val profile_section_desc: Int = net.maiatoday.tagspotter.feature.settings.R.string.profile_section_desc
    actual val photographer_name_label: Int = net.maiatoday.tagspotter.feature.settings.R.string.photographer_name_label
    actual val photographer_name_placeholder: Int = net.maiatoday.tagspotter.feature.settings.R.string.photographer_name_placeholder
    actual val map_preferences_title: Int = net.maiatoday.tagspotter.feature.settings.R.string.map_preferences_title
    actual val map_preferences_desc: Int = net.maiatoday.tagspotter.feature.settings.R.string.map_preferences_desc
    actual val home_city_label: Int = net.maiatoday.tagspotter.feature.settings.R.string.home_city_label
    actual val city_custom: Int = net.maiatoday.tagspotter.feature.settings.R.string.city_custom
    actual val latitude_label: Int = net.maiatoday.tagspotter.feature.settings.R.string.latitude_label
    actual val latitude_placeholder: Int = net.maiatoday.tagspotter.feature.settings.R.string.latitude_placeholder
    actual val longitude_label: Int = net.maiatoday.tagspotter.feature.settings.R.string.longitude_label
    actual val longitude_placeholder: Int = net.maiatoday.tagspotter.feature.settings.R.string.longitude_placeholder
    actual val darkmode_map_title: Int = net.maiatoday.tagspotter.feature.settings.R.string.darkmode_map_title
    actual val darkmode_map_desc: Int = net.maiatoday.tagspotter.feature.settings.R.string.darkmode_map_desc
    actual val settings_saved_toast: Int = net.maiatoday.tagspotter.feature.settings.R.string.settings_saved_toast
    actual val invalid_coordinates_toast: Int = net.maiatoday.tagspotter.feature.settings.R.string.invalid_coordinates_toast
    actual val save_settings_btn: Int = net.maiatoday.tagspotter.feature.settings.R.string.save_settings_btn
    actual val artist_id_title: Int = net.maiatoday.tagspotter.feature.settings.R.string.artist_id_title
    actual val artist_id_desc: Int = net.maiatoday.tagspotter.feature.settings.R.string.artist_id_desc
    actual val enable_recognition_title: Int = net.maiatoday.tagspotter.feature.settings.R.string.enable_recognition_title
    actual val ai_detection_title: Int = net.maiatoday.tagspotter.feature.settings.R.string.ai_detection_title
    actual val ai_detection_desc: Int = net.maiatoday.tagspotter.feature.settings.R.string.ai_detection_desc
    actual val gemini_api_key_label: Int = net.maiatoday.tagspotter.feature.settings.R.string.gemini_api_key_label
    actual val gemini_api_key_placeholder: Int = net.maiatoday.tagspotter.feature.settings.R.string.gemini_api_key_placeholder
    actual val content_desc_hide_api_key: Int = net.maiatoday.tagspotter.feature.settings.R.string.content_desc_hide_api_key
    actual val content_desc_show_api_key: Int = net.maiatoday.tagspotter.feature.settings.R.string.content_desc_show_api_key
}

@Composable
actual fun stringResource(id: Int): String {
    return androidx.compose.ui.res.stringResource(id)
}

@Composable
actual fun rememberToastLauncher(): ToastLauncher {
    val context = LocalContext.current
    return remember(context) {
        object : ToastLauncher {
            override fun showToast(message: String) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
