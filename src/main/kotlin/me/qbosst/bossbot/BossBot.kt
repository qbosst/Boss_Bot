package me.qbosst.bossbot

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.builders.ExtensibleBotBuilder
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.qbosst.bossbot.config.BotConfig
import me.qbosst.bossbot.database.DatabaseManager
import me.qbosst.bossbot.database.models.UserData
import me.qbosst.bossbot.extensions.*
import mu.KotlinLogging
import java.io.File
import java.util.Scanner

private val botLogger = KotlinLogging.logger {  }


class BossBot(
    val config: BotConfig,
    val database: DatabaseManager,
    settings: ExtensibleBotBuilder,
): ExtensibleBot(settings, config.discordToken) {

    override suspend fun setup() {
        database.connect()

        super.setup()
    }

    class Builder: ExtensibleBotBuilder() {
        lateinit var databaseBuilder: DatabaseManager.Builder

        lateinit var config: BotConfig

        fun database(builder: DatabaseManager.Builder.() -> Unit) {
            this.databaseBuilder = DatabaseManager.Builder().apply(builder)
        }

        override suspend fun build(token: String): BossBot {
            val database = databaseBuilder.build()

            val bot = BossBot(config, database, this)

            bot.setup()
            extensionsBuilder.extensions.forEach { bot.addExtension(it) }
            return bot
        }
    }

    companion object {
        /**
         * DSL Method for creating an instance of [BossBot]
         */
        suspend operator fun invoke(init: BossBot.Builder.() -> Unit): BossBot {
            val builder = BossBot.Builder().apply(init)
            val token = builder.config.discordToken
            return builder.build(token)
        }
    }
}

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

        // create boss bot
        val bot = BossBot {
            this.config = config

            database {
                this.host = config.databaseHost
                this.username = config.databaseUsername
                this.password = config.databasePassword
            }

            extensions {
                add { bot -> ColourExtension(bot, config.defaultCacheSize) }
                add { bot -> DeveloperExtension(bot, listOf(config.developerId)) }
                add(::TimeExtension)
                add(::MessageExtension)
            }

            cache {
                kord {
                    forDescription(UserData.description, lruCache(config.defaultCacheSize))
                }
            }

            commands {
                this.prefix = config.defaultPrefix
                this.invokeOnMention = true
                this.slashCommands = false
            }

        }

        bot.start()

    } catch (t: Throwable) {
        botLogger.error(t) { "Could not initialize Boss Bot "}
    } finally {
        println("Press any key to EXIT the program...")
        Scanner(System.`in`).next()
    }
}