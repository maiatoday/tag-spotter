package net.maiatoday.tagspotter.core.database

import net.maiatoday.tagspotter.core.model.generateUuid as generateModelUuid

fun generateUuid(): String {
    return generateModelUuid()
}
