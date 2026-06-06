package net.maiatoday.tagspotter.feature.detail

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

data class SpotDraftParams(
    val imagePath: String?,
    val thumbnailPath: String?,
    val latitude: Double?,
    val longitude: Double?,
    val category: String?,
    val captureTime: Long?
)

val detailModule = module {
    viewModel { params ->
        val spotId: Long = params.get()
        val draft: SpotDraftParams? = params.getOrNull()
        DetailViewModel(
            spotId = spotId,
            repository = get(),
            settingsRepository = get(),
            aiRecognitionService = get(),
            secretsProvider = get(),
            draftImagePath = draft?.imagePath,
            draftThumbnailPath = draft?.thumbnailPath,
            draftLatitude = draft?.latitude,
            draftLongitude = draft?.longitude,
            draftCategory = draft?.category,
            draftCaptureTime = draft?.captureTime
        )
    }
}
