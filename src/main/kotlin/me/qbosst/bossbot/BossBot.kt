package me.qbosst.bossbot

import com.kotlindiscord.kord.extensions.ExtensibleBot
import dev.kord.common.entity.Snowflake
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.qbosst.bossbot.config.BotConfig
import mu.KotlinLogging
import org.koin.core.logger.Level
import java.io.File
import java.util.Scanner

private val botLogger = KotlinLogging.logger {  }

class BossBot(
    token: String,
    defaultPrefix: String,
    addHelpExtension: Boolean = true,
    addSentryExtension: Boolean = true,
    invokeCommandOnMention: Boolean = true,
    messageCacheSize: Int = 10_000,
    commandThreads: Int = Runtime.getRuntime().availableProcessors() * 2,
    guildsToFill: List<Snowflake>? = listOf(),
    fillPresences: Boolean? = null,
    koinLogLevel: Level = Level.ERROR,
    val json: Json = Json {},
): ExtensibleBot(
    token,
    defaultPrefix,
    addHelpExtension,
    addSentryExtension,
    invokeCommandOnMention,
    messageCacheSize,
    commandThreads,
    guildsToFill,
    fillPresences,
    koinLogLevel
)

suspend fun main() {
    try {
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            prettyPrint = true
        }

        val file = File(BotConfig.DIRECTORY)

        // if config file does not exist, generate one and throw exception
        if(!file.exists()) {
            val default = BotConfig()
            file.writeBytes(json.encodeToString(default).encodeToByteArray())
            throw Exception("A config file has been generated at '${file.absolutePath}', please fill in the missing values")
        }

        // otherwise load config file
        val config = json.decodeFromString<BotConfig>(file.readText())

        val bot = BossBot(
            token = config.discordToken,
            defaultPrefix = config.defaultPrefix,
            json = json
        )

        bot.start()

    } catch (t: Throwable) {
        botLogger.error(t) { "Could not initialize Boss Bot "}
    } finally {
        println("Press any key to EXIT the program...")
        Scanner(System.`in`).next()
    }
}