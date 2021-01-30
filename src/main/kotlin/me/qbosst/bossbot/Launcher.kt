package me.qbosst.bossbot

import dev.kord.cache.map.MapLikeCollection
import dev.kord.cache.map.internal.MapEntryCache
import dev.kord.common.entity.PresenceStatus
import dev.kord.common.entity.Snowflake
import dev.kord.core.cache.data.GuildData
import dev.kord.core.cache.data.MessageData
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.qbosst.bossbot.config.BotConfig
import me.qbosst.bossbot.database.models.GuildSettings
import me.qbosst.bossbot.database.models.UserData
import me.qbosst.bossbot.extensions.*
import mu.KotlinLogging
import java.io.File
import java.util.*
import me.qbosst.bossbot.util.cache.MessageCache

private val logger = KotlinLogging.logger("Launcher.main")

suspend fun main(): Unit = try {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    val file = File(BotConfig.DIRECTORY)

    if(!file.exists()) {
        val default = BotConfig()
        file.writeBytes(json.encodeToString(default).encodeToByteArray())
        throw Exception("A config file has been generated at '${file.absolutePath}', please fill in the missing values")
    }

    val config = json.decodeFromString<BotConfig>(file.readText())

    val bot = BossBot {
        this.config = config

        database {
            this.host = config.databaseHost
            this.username = config.databaseUsername
            this.password = config.databasePassword
        }

        cache {
            this.cachedMessages = config.messageCacheSize

            kord {
                forDescription(UserData.description, lruCache(config.defaultCacheSize))
                forDescription(GuildSettings.description, lruCache(config.defaultCacheSize))

                messages { cache, description ->
                    MapEntryCache(cache, description, MessageCache(cachedMessages!!) as MapLikeCollection<MessageData, Snowflake>)
                }

                emojis(none())
                presences(none())
                webhooks(none())
            }
        }

        commands {
            this.prefix = config.defaultPrefix
            this.invokeOnMention = true
            this.slashCommands = false
        }

        presence {
            this.status = PresenceStatus.DoNotDisturb
            playing("Loading...")
        }

        members {
            this.fillPresences = false
            none()
        }

        extensions {
            add(::TimeExtension)
            add(::MessageExtension)
            add(::LoggerExtension)
            add { bot -> ColourExtension(bot, config.defaultCacheSize) }
            add { bot -> DeveloperExtension(bot, listOf(config.developerId)) }
        }
    }

    bot.start()

}
catch (t: Throwable) {
    logger.error(t) { "Could not initialize Boss Bot" }
} finally {
    println("Press any key to EXIT the program...")
    Scanner(System.`in`).next()
}