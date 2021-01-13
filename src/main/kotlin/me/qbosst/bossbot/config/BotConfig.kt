package me.qbosst.bossbot.config

import kotlinx.serialization.Serializable

@Serializable
class BotConfig(
    val discordToken: String = "token-here",
    val defaultPrefix: String = "b!"
) {

    companion object {
        const val DIRECTORY = "./config.json"
    }
}