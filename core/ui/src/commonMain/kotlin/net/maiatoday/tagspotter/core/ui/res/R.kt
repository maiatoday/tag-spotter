package net.maiatoday.tagspotter.core.ui.res

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.LOCAL_VARIABLE)
@Retention(AnnotationRetention.BINARY)
expect annotation class StringRes()

expect object R {
    object string {
        val creator_label_sculpture: Int
        val creator_label_architecture: Int
        val creator_label_nature: Int
        val creator_label_public_place: Int
        val creator_label_food: Int
        val creator_label_default: Int
        val creator_placeholder_sculpture: Int
        val creator_placeholder_architecture: Int
        val creator_placeholder_nature: Int
        val creator_placeholder_public_place: Int
        val creator_placeholder_food: Int
        val creator_placeholder_default: Int
        val creator_tf_label_sculpture: Int
        val creator_tf_label_architecture: Int
        val creator_tf_label_nature: Int
        val creator_tf_label_public_place: Int
        val creator_tf_label_food: Int
        val creator_tf_label_default: Int
        val creator_unknown_sculpture: Int
        val creator_unknown_architecture: Int
        val creator_unknown_nature: Int
        val creator_unknown_public_place: Int
        val creator_unknown_food: Int
        val creator_unknown_default: Int
        val status_active_sculpture: Int
        val status_active_architecture: Int
        val status_active_nature: Int
        val status_active_public_place: Int
        val status_active_food: Int
        val status_active_default: Int
        val status_inactive_graffiti: Int
        val status_inactive_sculpture: Int
        val status_inactive_architecture: Int
        val status_inactive_nature: Int
        val status_inactive_public_place: Int
        val status_inactive_food: Int
        val status_inactive_default: Int
        val status_action_inactive_graffiti: Int
        val status_action_inactive_sculpture: Int
        val status_action_inactive_architecture: Int
        val status_action_inactive_nature: Int
        val status_action_inactive_public_place: Int
        val status_action_inactive_food: Int
        val status_action_inactive_default: Int
        val date_label_graffiti: Int
        val date_label_sculpture: Int
        val date_label_architecture: Int
        val date_label_nature: Int
        val date_label_public_place: Int
        val date_label_food: Int
        val date_label_default: Int
        val date_tf_label_graffiti: Int
        val date_tf_label_sculpture: Int
        val date_tf_label_architecture: Int
        val date_tf_label_nature: Int
        val date_tf_label_public_place: Int
        val date_tf_label_food: Int
        val date_tf_label_default: Int
        val date_placeholder_graffiti: Int
        val date_placeholder_sculpture: Int
        val date_placeholder_architecture: Int
        val date_placeholder_nature: Int
        val date_placeholder_public_place: Int
        val date_placeholder_food: Int
        val date_placeholder_default: Int
    }
}
