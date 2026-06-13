package net.maiatoday.tagspotter.core.ui.res

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD, AnnotationTarget.LOCAL_VARIABLE)
@Retention(AnnotationRetention.BINARY)
actual annotation class StringRes

actual object R {
    actual val string: RStrings = RStrings
}

actual object RStrings {
    actual val creator_label_sculpture: Int = 1
    actual val creator_label_architecture: Int = 2
    actual val creator_label_nature: Int = 3
    actual val creator_label_public_place: Int = 4
    actual val creator_label_food: Int = 5
    actual val creator_label_default: Int = 6
    actual val creator_placeholder_sculpture: Int = 7
    actual val creator_placeholder_architecture: Int = 8
    actual val creator_placeholder_nature: Int = 9
    actual val creator_placeholder_public_place: Int = 10
    actual val creator_placeholder_food: Int = 11
    actual val creator_placeholder_default: Int = 12
    actual val creator_tf_label_sculpture: Int = 13
    actual val creator_tf_label_architecture: Int = 14
    actual val creator_tf_label_nature: Int = 15
    actual val creator_tf_label_public_place: Int = 16
    actual val creator_tf_label_food: Int = 17
    actual val creator_tf_label_default: Int = 18
    actual val creator_unknown_sculpture: Int = 19
    actual val creator_unknown_architecture: Int = 20
    actual val creator_unknown_nature: Int = 21
    actual val creator_unknown_public_place: Int = 22
    actual val creator_unknown_food: Int = 23
    actual val creator_unknown_default: Int = 24
    actual val status_active_sculpture: Int = 25
    actual val status_active_architecture: Int = 26
    actual val status_active_nature: Int = 27
    actual val status_active_public_place: Int = 28
    actual val status_active_food: Int = 29
    actual val status_active_default: Int = 30
    actual val status_inactive_graffiti: Int = 31
    actual val status_inactive_sculpture: Int = 32
    actual val status_inactive_architecture: Int = 33
    actual val status_inactive_nature: Int = 34
    actual val status_inactive_public_place: Int = 35
    actual val status_inactive_food: Int = 36
    actual val status_inactive_default: Int = 37
    actual val status_action_inactive_graffiti: Int = 38
    actual val status_action_inactive_sculpture: Int = 39
    actual val status_action_inactive_architecture: Int = 40
    actual val status_action_inactive_nature: Int = 41
    actual val status_action_inactive_public_place: Int = 42
    actual val status_action_inactive_food: Int = 43
    actual val status_action_inactive_default: Int = 44
    actual val date_label_graffiti: Int = 45
    actual val date_label_sculpture: Int = 46
    actual val date_label_architecture: Int = 47
    actual val date_label_nature: Int = 48
    actual val date_label_public_place: Int = 49
    actual val date_label_food: Int = 50
    actual val date_label_default: Int = 51
    actual val date_tf_label_graffiti: Int = 52
    actual val date_tf_label_sculpture: Int = 53
    actual val date_tf_label_architecture: Int = 54
    actual val date_tf_label_nature: Int = 55
    actual val date_tf_label_public_place: Int = 56
    actual val date_tf_label_food: Int = 57
    actual val date_tf_label_default: Int = 58
    actual val date_placeholder_graffiti: Int = 59
    actual val date_placeholder_sculpture: Int = 60
    actual val date_placeholder_architecture: Int = 61
    actual val date_placeholder_nature: Int = 62
    actual val date_placeholder_public_place: Int = 63
    actual val date_placeholder_food: Int = 64
    actual val date_placeholder_default: Int = 65
}
