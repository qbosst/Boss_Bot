package me.qbosst.bossbot.config

import kotlinx.serialization.Serializable

@Serializable
class BotConfig(
    val discordToken: String = "token-here",
    val defaultPrefix: String = "b!",
    val databaseHost: String = "",
    val databaseUsername: String = "",
    val databasePassword: String = "",
    val messageCacheSize: Int = 10_000,
    val defaultCacheSize: Int = 500
) {

    companion object {
        const val DIRECTORY = "./config.json"
    }
}