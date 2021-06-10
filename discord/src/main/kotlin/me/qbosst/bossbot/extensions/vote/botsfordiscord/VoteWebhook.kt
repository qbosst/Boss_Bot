package me.qbosst.bossbot.extensions.vote.botsfordiscord

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VoteWebhook(
    @SerialName("user") val userId: Long,
    @SerialName("bot") val botId: Long,
    val type: Type,
    val votes: Votes,
    val query: Map<String, String>? = null
) {
    @Serializable
    enum class Type {
        @SerialName("test") TEST,
        @SerialName("vote") VOTE
    }

    @Serializable
    data class Votes(
        val totalVotes: Int,
        val votes24: Int,
        val votesMonth: Int,
        val hasVoted: List<String>? = null,
        val hasVoted24: List<String>? = null
    )
}