package net.maiatoday.tagspotter.core.ui

import net.maiatoday.tagspotter.core.ui.res.TagRes
import net.maiatoday.tagspotter.core.ui.res.StringRes

@StringRes
fun String.getCategoryCreatorLabel(): Int = when (this) {
    "sculpture" -> TagRes.string.creator_label_sculpture
    "architecture" -> TagRes.string.creator_label_architecture
    "nature" -> TagRes.string.creator_label_nature
    "public_place" -> TagRes.string.creator_label_public_place
    "food" -> TagRes.string.creator_label_food
    else -> TagRes.string.creator_label_default
}

@StringRes
fun String.getCategoryCreatorPlaceholder(): Int = when (this) {
    "sculpture" -> TagRes.string.creator_placeholder_sculpture
    "architecture" -> TagRes.string.creator_placeholder_architecture
    "nature" -> TagRes.string.creator_placeholder_nature
    "public_place" -> TagRes.string.creator_placeholder_public_place
    "food" -> TagRes.string.creator_placeholder_food
    else -> TagRes.string.creator_placeholder_default
}

@StringRes
fun String.getCategoryCreatorTextFieldLabel(): Int = when (this) {
    "sculpture" -> TagRes.string.creator_tf_label_sculpture
    "architecture" -> TagRes.string.creator_tf_label_architecture
    "nature" -> TagRes.string.creator_tf_label_nature
    "public_place" -> TagRes.string.creator_tf_label_public_place
    "food" -> TagRes.string.creator_tf_label_food
    else -> TagRes.string.creator_tf_label_default
}

@StringRes
fun String.getCategoryCreatorUnknownLabel(): Int = when (this) {
    "sculpture" -> TagRes.string.creator_unknown_sculpture
    "architecture" -> TagRes.string.creator_unknown_architecture
    "nature" -> TagRes.string.creator_unknown_nature
    "public_place" -> TagRes.string.creator_unknown_public_place
    "food" -> TagRes.string.creator_unknown_food
    else -> TagRes.string.creator_unknown_default
}

@StringRes
fun String.getCategoryActiveStatusLabel(): Int = when (this) {
    "sculpture" -> TagRes.string.status_active_sculpture
    "architecture" -> TagRes.string.status_active_architecture
    "nature" -> TagRes.string.status_active_nature
    "public_place" -> TagRes.string.status_active_public_place
    "food" -> TagRes.string.status_active_food
    else -> TagRes.string.status_active_default
}

@StringRes
fun String.getCategoryInactiveStatusLabel(): Int = when (this) {
    "graffiti" -> TagRes.string.status_inactive_graffiti
    "sculpture" -> TagRes.string.status_inactive_sculpture
    "architecture" -> TagRes.string.status_inactive_architecture
    "nature" -> TagRes.string.status_inactive_nature
    "public_place" -> TagRes.string.status_inactive_public_place
    "food" -> TagRes.string.status_inactive_food
    else -> TagRes.string.status_inactive_default
}

@StringRes
fun String.getCategoryStatusActionMarkInactiveText(): Int = when (this) {
    "graffiti" -> TagRes.string.status_action_inactive_graffiti
    "sculpture" -> TagRes.string.status_action_inactive_sculpture
    "architecture" -> TagRes.string.status_action_inactive_architecture
    "nature" -> TagRes.string.status_action_inactive_nature
    "public_place" -> TagRes.string.status_action_inactive_public_place
    "food" -> TagRes.string.status_action_inactive_food
    else -> TagRes.string.status_action_inactive_default
}

@StringRes
fun String.getCategoryDateLabel(): Int = when (this) {
    "graffiti" -> TagRes.string.date_label_graffiti
    "sculpture" -> TagRes.string.date_label_sculpture
    "architecture" -> TagRes.string.date_label_architecture
    "nature" -> TagRes.string.date_label_nature
    "public_place" -> TagRes.string.date_label_public_place
    "food" -> TagRes.string.date_label_food
    else -> TagRes.string.date_label_default
}

@StringRes
fun String.getCategoryDateTextFieldLabel(): Int = when (this) {
    "graffiti" -> TagRes.string.date_tf_label_graffiti
    "sculpture" -> TagRes.string.date_tf_label_sculpture
    "architecture" -> TagRes.string.date_tf_label_architecture
    "nature" -> TagRes.string.date_tf_label_nature
    "public_place" -> TagRes.string.date_tf_label_public_place
    "food" -> TagRes.string.date_tf_label_food
    else -> TagRes.string.date_tf_label_default
}

@StringRes
fun String.getCategoryDatePlaceholder(): Int = when (this) {
    "graffiti" -> TagRes.string.date_placeholder_graffiti
    "sculpture" -> TagRes.string.date_placeholder_sculpture
    "architecture" -> TagRes.string.date_placeholder_architecture
    "nature" -> TagRes.string.date_placeholder_nature
    "public_place" -> TagRes.string.date_placeholder_public_place
    "food" -> TagRes.string.date_placeholder_food
    else -> TagRes.string.date_placeholder_default
}
