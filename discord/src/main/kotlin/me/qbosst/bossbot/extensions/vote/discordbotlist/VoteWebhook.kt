package me.qbosst.bossbot.extensions.vote.discordbotlist

import kotlinx.serialization.Serializable

@Serializable
data class VoteWebhook(
    val admin: Boolean? = null,
    val avatar: String? = null,
    val username: String,
    val id: Long
)