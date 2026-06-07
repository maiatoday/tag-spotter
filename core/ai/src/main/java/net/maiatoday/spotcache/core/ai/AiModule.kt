package net.maiatoday.spotcache.core.ai

import org.koin.dsl.module

val aiModule = module {
    single<AiRecognitionService> { AndroidAiRecognitionService(get()) }
}
