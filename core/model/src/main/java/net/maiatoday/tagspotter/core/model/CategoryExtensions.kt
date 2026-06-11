package net.maiatoday.tagspotter.core.model

fun String.getCategoryCreatorLabel(): String = when (this) {
    "sculpture" -> "SCULPTOR"
    "architecture" -> "ARCHITECT"
    "nature" -> "GARDENER / PLANNER"
    "public_place" -> "DESIGNER / CREATOR"
    "food" -> "CHEF"
    else -> "ARTIST / CREW"
}

fun String.getCategoryCreatorPlaceholder(): String = when (this) {
    "sculpture" -> "e.g. Michelangelo"
    "architecture" -> "e.g. Frank Gehry"
    "nature" -> "e.g. Landscape Architect, City Parks"
    "public_place" -> "e.g. City Council"
    "food" -> "e.g. Gordon Ramsay"
    else -> "e.g. Banksy"
}

fun String.getCategoryCreatorTextFieldLabel(): String = when (this) {
    "sculpture" -> "Sculptor Name"
    "architecture" -> "Architect Name"
    "nature" -> "Gardener/Planner Name"
    "public_place" -> "Designer/Creator Name"
    "food" -> "Chef Name"
    else -> "Artist Name"
}

fun String.getCategoryCreatorUnknownLabel(): String = when (this) {
    "sculpture" -> "Unknown Sculptor"
    "architecture" -> "Unknown Architect"
    "nature" -> "Unknown Designer"
    "public_place" -> "Unknown Creator"
    "food" -> "Unknown Chef"
    else -> "Unknown Artist"
}

fun String.getCategoryActiveStatusLabel(): String = when (this) {
    "sculpture" -> "On Display"
    "architecture" -> "Standing"
    "nature" -> "Vibrant"
    "public_place" -> "Active"
    "food" -> "Open"
    else -> "Active Spot"
}

fun String.getCategoryInactiveStatusLabel(): String = when (this) {
    "graffiti" -> "Painted Over"
    "sculpture" -> "Removed"
    "architecture" -> "Demolished"
    "nature" -> "Gone"
    "public_place" -> "Closed"
    "food" -> "Closed"
    else -> "Inactive"
}

fun String.getCategoryStatusActionMarkInactiveText(): String = when (this) {
    "graffiti" -> "Mark as Painted Over"
    "sculpture" -> "Mark as Removed"
    "architecture" -> "Mark as Demolished"
    "nature" -> "Mark as Gone"
    "public_place" -> "Mark as Closed"
    "food" -> "Mark as Closed"
    else -> "Mark as Inactive"
}

fun String.getCategoryDateLabel(): String = when (this) {
    "graffiti" -> "PAINTED DATE"
    "sculpture" -> "CREATION DATE"
    "architecture" -> "COMPLETED DATE"
    "nature" -> "ESTABLISHED DATE"
    "public_place" -> "ESTABLISHED DATE"
    "food" -> "VISITED DATE"
    else -> "CREATION DATE"
}

fun String.getCategoryDateTextFieldLabel(): String = when (this) {
    "graffiti" -> "Painted Date"
    "sculpture" -> "Creation Date"
    "architecture" -> "Completed Date"
    "nature" -> "Established Date"
    "public_place" -> "Established Date"
    "food" -> "Visited Date"
    else -> "Creation Date"
}

fun String.getCategoryDatePlaceholder(): String = when (this) {
    "graffiti" -> "e.g. circa 2023, Dec 2024"
    "sculpture" -> "e.g. 1504, circa 2020"
    "architecture" -> "e.g. 1939, built 2005"
    "nature" -> "e.g. planted 2020, Spring 2023"
    "public_place" -> "e.g. opened 2010, circa 1990"
    "food" -> "e.g. June 2024, last week"
    else -> "e.g. circa 2023, Dec 2024"
}

