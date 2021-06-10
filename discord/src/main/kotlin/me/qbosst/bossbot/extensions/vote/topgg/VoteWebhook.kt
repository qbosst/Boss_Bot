package me.qbosst.bossbot.extensions.vote.topgg

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VoteWebhook(
    @SerialName("bot") val botId: Long,
    @SerialName("user") val userId: Long,
    val type: Type,
    val isWeekend: Boolean,
    val query: String?
) {
    @Serializable
    enum class Type {
        @SerialName("test") TEST,
        @SerialName("upvote") VOTE
    }
}