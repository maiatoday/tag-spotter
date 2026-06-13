package net.maiatoday.tagspotter.core.ui

import net.maiatoday.tagspotter.core.ui.res.TagRes

import kotlin.test.assertEquals
import kotlin.test.Test

class CategoryExtensionsTest {

    @Test
    fun testGetCategoryCreatorLabel() {
        assertEquals(TagRes.string.creator_label_default, "graffiti".getCategoryCreatorLabel())
        assertEquals(TagRes.string.creator_label_sculpture, "sculpture".getCategoryCreatorLabel())
        assertEquals(TagRes.string.creator_label_architecture, "architecture".getCategoryCreatorLabel())
        assertEquals(TagRes.string.creator_label_nature, "nature".getCategoryCreatorLabel())
        assertEquals(TagRes.string.creator_label_public_place, "public_place".getCategoryCreatorLabel())
        assertEquals(TagRes.string.creator_label_food, "food".getCategoryCreatorLabel())
        assertEquals(TagRes.string.creator_label_default, "unknown_category".getCategoryCreatorLabel())
    }

    @Test
    fun testGetCategoryCreatorPlaceholder() {
        assertEquals(TagRes.string.creator_placeholder_default, "graffiti".getCategoryCreatorPlaceholder())
        assertEquals(TagRes.string.creator_placeholder_sculpture, "sculpture".getCategoryCreatorPlaceholder())
        assertEquals(TagRes.string.creator_placeholder_architecture, "architecture".getCategoryCreatorPlaceholder())
        assertEquals(TagRes.string.creator_placeholder_nature, "nature".getCategoryCreatorPlaceholder())
        assertEquals(TagRes.string.creator_placeholder_public_place, "public_place".getCategoryCreatorPlaceholder())
        assertEquals(TagRes.string.creator_placeholder_food, "food".getCategoryCreatorPlaceholder())
        assertEquals(TagRes.string.creator_placeholder_default, "unknown_category".getCategoryCreatorPlaceholder())
    }

    @Test
    fun testGetCategoryCreatorTextFieldLabel() {
        assertEquals(TagRes.string.creator_tf_label_default, "graffiti".getCategoryCreatorTextFieldLabel())
        assertEquals(TagRes.string.creator_tf_label_sculpture, "sculpture".getCategoryCreatorTextFieldLabel())
        assertEquals(TagRes.string.creator_tf_label_architecture, "architecture".getCategoryCreatorTextFieldLabel())
        assertEquals(TagRes.string.creator_tf_label_nature, "nature".getCategoryCreatorTextFieldLabel())
        assertEquals(TagRes.string.creator_tf_label_public_place, "public_place".getCategoryCreatorTextFieldLabel())
        assertEquals(TagRes.string.creator_tf_label_food, "food".getCategoryCreatorTextFieldLabel())
        assertEquals(TagRes.string.creator_tf_label_default, "unknown_category".getCategoryCreatorTextFieldLabel())
    }

    @Test
    fun testGetCategoryCreatorUnknownLabel() {
        assertEquals(TagRes.string.creator_unknown_default, "graffiti".getCategoryCreatorUnknownLabel())
        assertEquals(TagRes.string.creator_unknown_sculpture, "sculpture".getCategoryCreatorUnknownLabel())
        assertEquals(TagRes.string.creator_unknown_architecture, "architecture".getCategoryCreatorUnknownLabel())
        assertEquals(TagRes.string.creator_unknown_nature, "nature".getCategoryCreatorUnknownLabel())
        assertEquals(TagRes.string.creator_unknown_public_place, "public_place".getCategoryCreatorUnknownLabel())
        assertEquals(TagRes.string.creator_unknown_food, "food".getCategoryCreatorUnknownLabel())
        assertEquals(TagRes.string.creator_unknown_default, "unknown_category".getCategoryCreatorUnknownLabel())
    }

    @Test
    fun testGetCategoryActiveStatusLabel() {
        assertEquals(TagRes.string.status_active_default, "graffiti".getCategoryActiveStatusLabel())
        assertEquals(TagRes.string.status_active_sculpture, "sculpture".getCategoryActiveStatusLabel())
        assertEquals(TagRes.string.status_active_architecture, "architecture".getCategoryActiveStatusLabel())
        assertEquals(TagRes.string.status_active_nature, "nature".getCategoryActiveStatusLabel())
        assertEquals(TagRes.string.status_active_public_place, "public_place".getCategoryActiveStatusLabel())
        assertEquals(TagRes.string.status_active_food, "food".getCategoryActiveStatusLabel())
        assertEquals(TagRes.string.status_active_default, "unknown_category".getCategoryActiveStatusLabel())
    }

    @Test
    fun testGetCategoryInactiveStatusLabel() {
        assertEquals(TagRes.string.status_inactive_graffiti, "graffiti".getCategoryInactiveStatusLabel())
        assertEquals(TagRes.string.status_inactive_sculpture, "sculpture".getCategoryInactiveStatusLabel())
        assertEquals(TagRes.string.status_inactive_architecture, "architecture".getCategoryInactiveStatusLabel())
        assertEquals(TagRes.string.status_inactive_nature, "nature".getCategoryInactiveStatusLabel())
        assertEquals(TagRes.string.status_inactive_public_place, "public_place".getCategoryInactiveStatusLabel())
        assertEquals(TagRes.string.status_inactive_food, "food".getCategoryInactiveStatusLabel())
        assertEquals(TagRes.string.status_inactive_default, "unknown_category".getCategoryInactiveStatusLabel())
    }

    @Test
    fun testGetCategoryStatusActionMarkInactiveText() {
        assertEquals(TagRes.string.status_action_inactive_graffiti, "graffiti".getCategoryStatusActionMarkInactiveText())
        assertEquals(TagRes.string.status_action_inactive_sculpture, "sculpture".getCategoryStatusActionMarkInactiveText())
        assertEquals(TagRes.string.status_action_inactive_architecture, "architecture".getCategoryStatusActionMarkInactiveText())
        assertEquals(TagRes.string.status_action_inactive_nature, "nature".getCategoryStatusActionMarkInactiveText())
        assertEquals(TagRes.string.status_action_inactive_public_place, "public_place".getCategoryStatusActionMarkInactiveText())
        assertEquals(TagRes.string.status_action_inactive_food, "food".getCategoryStatusActionMarkInactiveText())
        assertEquals(TagRes.string.status_action_inactive_default, "unknown_category".getCategoryStatusActionMarkInactiveText())
    }

    @Test
    fun testGetCategoryDateLabel() {
        assertEquals(TagRes.string.date_label_graffiti, "graffiti".getCategoryDateLabel())
        assertEquals(TagRes.string.date_label_sculpture, "sculpture".getCategoryDateLabel())
        assertEquals(TagRes.string.date_label_architecture, "architecture".getCategoryDateLabel())
        assertEquals(TagRes.string.date_label_nature, "nature".getCategoryDateLabel())
        assertEquals(TagRes.string.date_label_public_place, "public_place".getCategoryDateLabel())
        assertEquals(TagRes.string.date_label_food, "food".getCategoryDateLabel())
        assertEquals(TagRes.string.date_label_default, "unknown_category".getCategoryDateLabel())
    }

    @Test
    fun testGetCategoryDateTextFieldLabel() {
        assertEquals(TagRes.string.date_tf_label_graffiti, "graffiti".getCategoryDateTextFieldLabel())
        assertEquals(TagRes.string.date_tf_label_sculpture, "sculpture".getCategoryDateTextFieldLabel())
        assertEquals(TagRes.string.date_tf_label_architecture, "architecture".getCategoryDateTextFieldLabel())
        assertEquals(TagRes.string.date_tf_label_nature, "nature".getCategoryDateTextFieldLabel())
        assertEquals(TagRes.string.date_tf_label_public_place, "public_place".getCategoryDateTextFieldLabel())
        assertEquals(TagRes.string.date_tf_label_food, "food".getCategoryDateTextFieldLabel())
        assertEquals(TagRes.string.date_tf_label_default, "unknown_category".getCategoryDateTextFieldLabel())
    }

    @Test
    fun testGetCategoryDatePlaceholder() {
        assertEquals(TagRes.string.date_placeholder_graffiti, "graffiti".getCategoryDatePlaceholder())
        assertEquals(TagRes.string.date_placeholder_sculpture, "sculpture".getCategoryDatePlaceholder())
        assertEquals(TagRes.string.date_placeholder_architecture, "architecture".getCategoryDatePlaceholder())
        assertEquals(TagRes.string.date_placeholder_nature, "nature".getCategoryDatePlaceholder())
        assertEquals(TagRes.string.date_placeholder_public_place, "public_place".getCategoryDatePlaceholder())
        assertEquals(TagRes.string.date_placeholder_food, "food".getCategoryDatePlaceholder())
        assertEquals(TagRes.string.date_placeholder_default, "unknown_category".getCategoryDatePlaceholder())
    }
}
