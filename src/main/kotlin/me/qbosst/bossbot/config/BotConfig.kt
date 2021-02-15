package me.qbosst.bossbot.config

import kotlinx.serialization.Serializable

@Serializable
data class BotConfig(
    val discord: Discord = Discord(),
    val database: Database = Database(),
    val developerId: Long = 332947254602235914,
    val spaceSpeak: SpaceSpeak = SpaceSpeak()
) {
    @Serializable
    data class Database(
        val host: String = "",
        val username: String = "",
        val password: String = "",
    )

    @Serializable
    data class Discord(
        val token: String = "token-here",
        val defaultPrefix: String = "b!",
        val messageCacheSize: Int = 10_000,
        val defaultCacheSize: Int = 500,
    )

    @Serializable
    data class SpaceSpeak(
        val token: String = "token-here",
        val emailAddress: String = "",
        val username: String = "Boss Bot"
    )

    companion object {
        const val DIRECTORY = "./config.json"
    }
}