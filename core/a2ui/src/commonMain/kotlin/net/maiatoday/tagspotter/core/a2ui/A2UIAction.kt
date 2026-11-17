package net.maiatoday.tagspotter.core.a2ui

sealed interface A2UIAction {
    data class ViewOnMap(
        val latitude: Double,
        val longitude: Double,
        val title: String,
        val spotId: String? = null
    ) : A2UIAction

    data class StarSpot(
        val spotId: String
    ) : A2UIAction

    data class NavigateToStop(
        val stopIndex: Int,
        val latitude: Double,
        val longitude: Double,
        val title: String
    ) : A2UIAction

    data class VoteOption(
        val pollId: String,
        val optionId: String
    ) : A2UIAction

    data class AnswerQuiz(
        val quizId: String,
        val optionId: String,
        val isCorrect: Boolean
    ) : A2UIAction

    data class OpenSpotDetail(
        val spotId: String
    ) : A2UIAction

    data class ClientEvent(
        val name: String,
        val surfaceId: String,
        val context: Map<String, Any?> = emptyMap()
    ) : A2UIAction

    data class ClientError(
        val code: String,
        val surfaceId: String,
        val message: String,
        val context: Map<String, Any?> = emptyMap()
    ) : A2UIAction

    data class Retry(
        val rawJson: String
    ) : A2UIAction

    data class Custom(
        val name: String,
        val params: Map<String, String> = emptyMap()
    ) : A2UIAction
}
