package net.maiatoday.tagspotter.feature.main

import net.maiatoday.tagspotter.core.ai.AiRecognitionService
import org.koin.core.module.Module
import org.koin.dsl.module

fun createIosAiModule(aiService: AiRecognitionService): Module = module {
    single<AiRecognitionService> { aiService }
}
