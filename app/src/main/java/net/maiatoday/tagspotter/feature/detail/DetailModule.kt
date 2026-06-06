package net.maiatoday.tagspotter.feature.detail

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val detailModule = module {
    viewModel { params ->
        DetailViewModel(
            spotId = params.get(),
            repository = get(),
            settingsRepository = get(),
            aiRecognitionService = get(),
            secretsProvider = get(),
            draftImagePath = params.getOrNull(),
            draftThumbnailPath = params.getOrNull(),
            draftLatitude = params.getOrNull(),
            draftLongitude = params.getOrNull(),
            draftCategory = params.getOrNull(),
            draftCaptureTime = params.getOrNull()
        )
    }
}
