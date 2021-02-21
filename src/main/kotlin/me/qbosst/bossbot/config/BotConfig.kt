package me.qbosst.bossbot.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.Exception

/**
 * Object containing config details needed for the bot.
 */
@Serializable
data class BotConfig(
    val discord: Discord,
    val database: Database,
    val spaceSpeak: SpaceSpeak
) {
    /**
     * Object containing config details for discord
     */
    @Serializable
    data class Discord(
        val token: String,
        val defaultPrefix: String,
        val messageCacheSize: Int,
        val defaultCacheSize: Int,
        val developerId: Long,
    )

    /**
     * Object containing config details for the database
     */
    @Serializable
    data class Database(
        val host: String,
        val username: String,
        val password: String,
    )

    @Serializable
    data class SpaceSpeak(
        val token: String,
        val emailAddress: String,
        val username: String
    )

    companion object {
        /**
         * JSON instance used to read and write config files
         */
        private val json: Json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            prettyPrint = true
        }

        /**
         * Creates an instance of a default config file
         */
        private fun createDefaultConfig(): BotConfig = BotConfig(
            discord = BotConfig.Discord(
                token = "token-here",
                defaultPrefix = "b!",
                messageCacheSize = 10_000,
                defaultCacheSize = 1_000,
                developerId = 332947254602235914
            ),
            database = BotConfig.Database(
                host = "",
                username = "root",
                password = "root"
            ),
            spaceSpeak = BotConfig.SpaceSpeak(
                token = "token-here",
                emailAddress = "",
                username = "Boss Bot"
            )
        )

        /**
         * Deserializes the file at [url] to a [BotConfig]
         */
        fun from(url: String): BotConfig {
            // we want to load a json config file
            if(!url.endsWith(".json")) {
                throw Exception("Config files must end with '.json'")
            }

            val file = File(url)

            // checks if file exists, if not create one with default values and throw error
            if(!file.exists()) {
                val defaultConfig: BotConfig = createDefaultConfig()
                file.writeText(json.encodeToString(defaultConfig))
                throw Exception("A config file has been generated at '${file.absolutePath}'. Please fill in the values")
            }

            // read the file and deserialize to a config
            val config: BotConfig = json.decodeFromString<BotConfig>(file.readText())

            return config
        }
    }
}