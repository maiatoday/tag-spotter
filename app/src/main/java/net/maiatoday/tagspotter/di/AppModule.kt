package net.maiatoday.tagspotter.di

import net.maiatoday.tagspotter.data.LocalSpotRepository
import net.maiatoday.tagspotter.data.SharedPreferencesSettingsRepository
import net.maiatoday.tagspotter.data.SpotDatabase
import net.maiatoday.tagspotter.data.SpotRepository
import net.maiatoday.tagspotter.data.SettingsRepository
import net.maiatoday.tagspotter.domain.LocationProvider
import net.maiatoday.tagspotter.domain.PhotoProcessor
import net.maiatoday.tagspotter.data.service.AndroidLocationProvider
import net.maiatoday.tagspotter.data.service.AndroidPhotoProcessor
import net.maiatoday.tagspotter.domain.GeofenceService
import net.maiatoday.tagspotter.data.service.AndroidGeofenceService
import net.maiatoday.tagspotter.ui.viewmodel.DetailViewModel
import net.maiatoday.tagspotter.ui.viewmodel.GalleryViewModel
import net.maiatoday.tagspotter.ui.viewmodel.MainViewModel
import net.maiatoday.tagspotter.ui.viewmodel.MapViewModel
import net.maiatoday.tagspotter.ui.viewmodel.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Database and Dao
    single { SpotDatabase.getDatabase(androidContext()) }
    single { get<SpotDatabase>().spotDao() }

    // Repositories
    single<SpotRepository> { LocalSpotRepository(androidContext(), get(), get()) }
    single<SettingsRepository> { SharedPreferencesSettingsRepository(androidContext()) }

    // Services
    single<LocationProvider> { AndroidLocationProvider(androidContext()) }
    single<PhotoProcessor> { AndroidPhotoProcessor(androidContext()) }
    single<GeofenceService> { AndroidGeofenceService(androidContext()) }

    // ViewModels
    viewModel { MainViewModel(get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get()) }
    viewModel { GalleryViewModel(get(), get()) }
    viewModel { MapViewModel(get(), get()) }
    viewModel { params ->
        DetailViewModel(
            spotId = params.get(),
            repository = get(),
            settingsRepository = get(),
            draftImagePath = params.getOrNull(),
            draftThumbnailPath = params.getOrNull(),
            draftLatitude = params.getOrNull(),
            draftLongitude = params.getOrNull(),
            draftCategory = params.getOrNull(),
            draftCaptureTime = params.getOrNull()
        )
    }
}
