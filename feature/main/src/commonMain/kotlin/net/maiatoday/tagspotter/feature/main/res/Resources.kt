package net.maiatoday.tagspotter.feature.main.res

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import tagspotter.feature.main.generated.resources.Res
import tagspotter.feature.main.generated.resources.*

annotation class StringRes

object MainRes {
    val string = MainStrings
}

object MainStrings {
    val drawer_settings_label: StringResource = Res.string.drawer_settings_label
    val content_desc_settings: StringResource = Res.string.content_desc_settings
    val drawer_mock_data_label: StringResource = Res.string.drawer_mock_data_label
    val drawer_version_format: StringResource = Res.string.drawer_version_format
    val content_desc_gallery_tab: StringResource = Res.string.content_desc_gallery_tab
    val tab_gallery_label: StringResource = Res.string.tab_gallery_label
    val content_desc_maps_tab: StringResource = Res.string.content_desc_maps_tab
    val tab_maps_label: StringResource = Res.string.tab_maps_label
    val content_desc_camera_tab: StringResource = Res.string.content_desc_camera_tab
    val tab_camera_label: StringResource = Res.string.tab_camera_label
    val content_desc_files_tab: StringResource = Res.string.content_desc_files_tab
    val tab_files_label: StringResource = Res.string.tab_files_label
    val loading_processing_image: StringResource = Res.string.loading_processing_image
}

@Composable
fun stringResource(id: StringResource): String {
    return org.jetbrains.compose.resources.stringResource(id)
}

@Composable
fun stringResource(id: StringResource, vararg formatArgs: Any): String {
    return org.jetbrains.compose.resources.stringResource(id, *formatArgs)
}
