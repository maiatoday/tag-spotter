package net.maiatoday.tagspotter.core.database

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun generateUuid(): String {
    return Uuid.random().toString()
}
