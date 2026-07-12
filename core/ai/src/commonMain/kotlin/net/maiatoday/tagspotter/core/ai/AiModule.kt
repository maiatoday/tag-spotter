package net.maiatoday.tagspotter.core.ai

import org.koin.dsl.module

val aiModule = module {
    single<AiRecognitionService> { UnsupportedAiRecognitionService() }
}
