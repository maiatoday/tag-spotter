package net.maiatoday.tagspotter.feature.settings.res

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.LOCAL_VARIABLE)
@Retention(AnnotationRetention.BINARY)
actual annotation class StringRes

actual object R {
    actual object string {
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
}

private val stringMap = mapOf(
    1 to "Settings",
    2 to "Back",
    3 to "Photographer Profile",
    4 to "Set your photographer name. This name will be automatically associated with any new spot you capture.",
    5 to "Photographer Name",
    6 to "e.g. Jane Doe",
    7 to "Map Preferences",
    8 to "Select your home city. This city will be the default focus when you open the map and have no tagged spots yet.",
    9 to "Home City",
    10 to "Custom",
    11 to "Latitude",
    12 to "e.g. 52.5200",
    13 to "Longitude",
    14 to "e.g. 13.4050",
    15 to "Darkmode Map Renders",
    16 to "Use dark themed tiles when system is in dark mode.",
    17 to "Settings Saved!",
    18 to "Please enter valid coordinates",
    19 to "Save Settings",
    20 to "Artist Identification",
    21 to "Identify street art and graffiti artists automatically using visual search options or AI.",
    22 to "Enable Recognition Features",
    23 to "In-App AI Detection (Gemini)",
    24 to "To use in-app AI recognition, provide your personal Gemini API key. This key is stored securely on your device.",
    25 to "Gemini API Key",
    26 to "AIzaSy…",
    27 to "Hide API Key",
    28 to "Show API Key"
)

@Composable
actual fun stringResource(id: Int): String {
    return stringMap[id] ?: "Unknown String"
}

@Composable
actual fun rememberToastLauncher(): ToastLauncher {
    return remember {
        object : ToastLauncher {
            override fun showToast(message: String) {
                println("Toast: $message")
            }
        }
    }
}
