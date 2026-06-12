package net.maiatoday.tagspotter.core.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CategoryExtensionsTest {

    @Test
    fun testGetCategoryCreatorLabel() {
        assertEquals(R.string.creator_label_default, "graffiti".getCategoryCreatorLabel())
        assertEquals(R.string.creator_label_sculpture, "sculpture".getCategoryCreatorLabel())
        assertEquals(R.string.creator_label_architecture, "architecture".getCategoryCreatorLabel())
        assertEquals(R.string.creator_label_nature, "nature".getCategoryCreatorLabel())
        assertEquals(R.string.creator_label_public_place, "public_place".getCategoryCreatorLabel())
        assertEquals(R.string.creator_label_food, "food".getCategoryCreatorLabel())
        assertEquals(R.string.creator_label_default, "unknown_category".getCategoryCreatorLabel())
    }

    @Test
    fun testGetCategoryCreatorPlaceholder() {
        assertEquals(R.string.creator_placeholder_default, "graffiti".getCategoryCreatorPlaceholder())
        assertEquals(R.string.creator_placeholder_sculpture, "sculpture".getCategoryCreatorPlaceholder())
        assertEquals(R.string.creator_placeholder_architecture, "architecture".getCategoryCreatorPlaceholder())
        assertEquals(R.string.creator_placeholder_nature, "nature".getCategoryCreatorPlaceholder())
        assertEquals(R.string.creator_placeholder_public_place, "public_place".getCategoryCreatorPlaceholder())
        assertEquals(R.string.creator_placeholder_food, "food".getCategoryCreatorPlaceholder())
        assertEquals(R.string.creator_placeholder_default, "unknown_category".getCategoryCreatorPlaceholder())
    }

    @Test
    fun testGetCategoryCreatorTextFieldLabel() {
        assertEquals(R.string.creator_tf_label_default, "graffiti".getCategoryCreatorTextFieldLabel())
        assertEquals(R.string.creator_tf_label_sculpture, "sculpture".getCategoryCreatorTextFieldLabel())
        assertEquals(R.string.creator_tf_label_architecture, "architecture".getCategoryCreatorTextFieldLabel())
        assertEquals(R.string.creator_tf_label_nature, "nature".getCategoryCreatorTextFieldLabel())
        assertEquals(R.string.creator_tf_label_public_place, "public_place".getCategoryCreatorTextFieldLabel())
        assertEquals(R.string.creator_tf_label_food, "food".getCategoryCreatorTextFieldLabel())
        assertEquals(R.string.creator_tf_label_default, "unknown_category".getCategoryCreatorTextFieldLabel())
    }

    @Test
    fun testGetCategoryCreatorUnknownLabel() {
        assertEquals(R.string.creator_unknown_default, "graffiti".getCategoryCreatorUnknownLabel())
        assertEquals(R.string.creator_unknown_sculpture, "sculpture".getCategoryCreatorUnknownLabel())
        assertEquals(R.string.creator_unknown_architecture, "architecture".getCategoryCreatorUnknownLabel())
        assertEquals(R.string.creator_unknown_nature, "nature".getCategoryCreatorUnknownLabel())
        assertEquals(R.string.creator_unknown_public_place, "public_place".getCategoryCreatorUnknownLabel())
        assertEquals(R.string.creator_unknown_food, "food".getCategoryCreatorUnknownLabel())
        assertEquals(R.string.creator_unknown_default, "unknown_category".getCategoryCreatorUnknownLabel())
    }

    @Test
    fun testGetCategoryActiveStatusLabel() {
        assertEquals(R.string.status_active_default, "graffiti".getCategoryActiveStatusLabel())
        assertEquals(R.string.status_active_sculpture, "sculpture".getCategoryActiveStatusLabel())
        assertEquals(R.string.status_active_architecture, "architecture".getCategoryActiveStatusLabel())
        assertEquals(R.string.status_active_nature, "nature".getCategoryActiveStatusLabel())
        assertEquals(R.string.status_active_public_place, "public_place".getCategoryActiveStatusLabel())
        assertEquals(R.string.status_active_food, "food".getCategoryActiveStatusLabel())
        assertEquals(R.string.status_active_default, "unknown_category".getCategoryActiveStatusLabel())
    }

    @Test
    fun testGetCategoryInactiveStatusLabel() {
        assertEquals(R.string.status_inactive_graffiti, "graffiti".getCategoryInactiveStatusLabel())
        assertEquals(R.string.status_inactive_sculpture, "sculpture".getCategoryInactiveStatusLabel())
        assertEquals(R.string.status_inactive_architecture, "architecture".getCategoryInactiveStatusLabel())
        assertEquals(R.string.status_inactive_nature, "nature".getCategoryInactiveStatusLabel())
        assertEquals(R.string.status_inactive_public_place, "public_place".getCategoryInactiveStatusLabel())
        assertEquals(R.string.status_inactive_food, "food".getCategoryInactiveStatusLabel())
        assertEquals(R.string.status_inactive_default, "unknown_category".getCategoryInactiveStatusLabel())
    }

    @Test
    fun testGetCategoryStatusActionMarkInactiveText() {
        assertEquals(R.string.status_action_inactive_graffiti, "graffiti".getCategoryStatusActionMarkInactiveText())
        assertEquals(R.string.status_action_inactive_sculpture, "sculpture".getCategoryStatusActionMarkInactiveText())
        assertEquals(R.string.status_action_inactive_architecture, "architecture".getCategoryStatusActionMarkInactiveText())
        assertEquals(R.string.status_action_inactive_nature, "nature".getCategoryStatusActionMarkInactiveText())
        assertEquals(R.string.status_action_inactive_public_place, "public_place".getCategoryStatusActionMarkInactiveText())
        assertEquals(R.string.status_action_inactive_food, "food".getCategoryStatusActionMarkInactiveText())
        assertEquals(R.string.status_action_inactive_default, "unknown_category".getCategoryStatusActionMarkInactiveText())
    }

    @Test
    fun testGetCategoryDateLabel() {
        assertEquals(R.string.date_label_graffiti, "graffiti".getCategoryDateLabel())
        assertEquals(R.string.date_label_sculpture, "sculpture".getCategoryDateLabel())
        assertEquals(R.string.date_label_architecture, "architecture".getCategoryDateLabel())
        assertEquals(R.string.date_label_nature, "nature".getCategoryDateLabel())
        assertEquals(R.string.date_label_public_place, "public_place".getCategoryDateLabel())
        assertEquals(R.string.date_label_food, "food".getCategoryDateLabel())
        assertEquals(R.string.date_label_default, "unknown_category".getCategoryDateLabel())
    }

    @Test
    fun testGetCategoryDateTextFieldLabel() {
        assertEquals(R.string.date_tf_label_graffiti, "graffiti".getCategoryDateTextFieldLabel())
        assertEquals(R.string.date_tf_label_sculpture, "sculpture".getCategoryDateTextFieldLabel())
        assertEquals(R.string.date_tf_label_architecture, "architecture".getCategoryDateTextFieldLabel())
        assertEquals(R.string.date_tf_label_nature, "nature".getCategoryDateTextFieldLabel())
        assertEquals(R.string.date_tf_label_public_place, "public_place".getCategoryDateTextFieldLabel())
        assertEquals(R.string.date_tf_label_food, "food".getCategoryDateTextFieldLabel())
        assertEquals(R.string.date_tf_label_default, "unknown_category".getCategoryDateTextFieldLabel())
    }

    @Test
    fun testGetCategoryDatePlaceholder() {
        assertEquals(R.string.date_placeholder_graffiti, "graffiti".getCategoryDatePlaceholder())
        assertEquals(R.string.date_placeholder_sculpture, "sculpture".getCategoryDatePlaceholder())
        assertEquals(R.string.date_placeholder_architecture, "architecture".getCategoryDatePlaceholder())
        assertEquals(R.string.date_placeholder_nature, "nature".getCategoryDatePlaceholder())
        assertEquals(R.string.date_placeholder_public_place, "public_place".getCategoryDatePlaceholder())
        assertEquals(R.string.date_placeholder_food, "food".getCategoryDatePlaceholder())
        assertEquals(R.string.date_placeholder_default, "unknown_category".getCategoryDatePlaceholder())
    }
}
