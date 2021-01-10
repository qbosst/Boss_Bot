package me.qbosst.bossbot.config

import kotlinx.serialization.Serializable

@Serializable
class BotConfig(
    val discordToken: String = "token-here",
    val defaultPrefix: String = "b!",
    val databaseUrl: String = "",
    val databaseUser: String = "root",
    val databasePassword: String = "root",
    val cacheSize: Int = 100,
    val developerIds: List<Long> = listOf(332947254602235914),
    val deepaiToken: String = "",
    val spotifyClientId: String = "",
    val spotifyClientSecret: String = "",
    val voteLinks: List<String> = listOf(),
    val spaceSpeakToken: String = "",
    val spaceSpeakEmailAddress: String = ""
) {
    companion object {
        const val DIRECTORY = "./config.json"
    }
}