package me.qbosst.bossbot.extensions.vote.discordboats

import kotlinx.serialization.Serializable

@Serializable
data class VoteWebhook(val bot: BotData, val user: UserData) {

    @Serializable
    data class BotData(val id: Long, val name: String)

    @Serializable
    data class UserData(val id: Long, val username: String, val discriminator: String)
}