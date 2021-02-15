package me.qbosst.bossbot

import com.kotlindiscord.kord.extensions.utils.authorIsBot
import dev.kord.cache.map.MapLikeCollection
import dev.kord.common.entity.PresenceStatus
import dev.kord.common.entity.Snowflake
import dev.kord.core.cache.data.MessageData
import dev.kord.core.enableEvent
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.guild.GuildCreateEvent
import dev.kord.gateway.PrivilegedIntent
import dev.kord.gateway.editPresence
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.qbosst.bossbot.config.BotConfig
import me.qbosst.bossbot.database.models.GuildColours
import me.qbosst.bossbot.database.models.GuildSettings
import me.qbosst.bossbot.database.models.UserData
import me.qbosst.bossbot.database.models.getOrRetrieveSettings
import me.qbosst.bossbot.database.tables.GuildColoursTable
import me.qbosst.bossbot.database.tables.GuildSettingsTable
import me.qbosst.bossbot.database.tables.SpaceSpeakTable
import me.qbosst.bossbot.database.tables.UserDataTable
import me.qbosst.bossbot.extensions.*
import mu.KotlinLogging
import java.io.File
import java.util.*
import me.qbosst.bossbot.util.cache.MessageCache

@OptIn(PrivilegedIntent::class)
suspend fun main(): Unit = try {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    val file = File(BotConfig.DIRECTORY)

    // check if file exists, if not create a config file and throw error
    if(!file.exists()) {
        val default = BotConfig()
        file.writeBytes(json.encodeToString(default).encodeToByteArray())
        throw Exception("A config file has been generated at '${file.absolutePath}', please fill in the missing values")
    }

    // load config file
    val config = json.decodeFromString<BotConfig>(file.readText())

    // create instance of bot
    val bot = BossBot {
        this.config = config

        database {
            // set database connection properties
            this.host = config.database.host
            this.username = config.database.username
            this.password = config.database.password

            // add the tables that this database will use
            tables {
                add(GuildColoursTable)
                add(GuildSettingsTable)
                add(UserDataTable)
                add(SpaceSpeakTable)
            }
        }

        cache {
            // set the amount of cached messages we want
            this.cachedMessages = config.discord.messageCacheSize

            kord {
                // register custom objects to cache
                forDescription(UserData.description, lruCache(config.discord.defaultCacheSize))
                forDescription(GuildSettings.description, lruCache(config.discord.defaultCacheSize))
                forDescription(GuildColours.description, lruCache(config.discord.defaultCacheSize))

                // configure the kord objects we want to cache
                @Suppress("UNCHECKED_CAST")
                messages(mapLikeCollection(MessageCache(cachedMessages!!) { message -> !message.authorIsBot } as MapLikeCollection<MessageData, Snowflake>))

                emojis(none())
                presences(none())
                webhooks(none())
            }

            // register custom objects we want to cache
            transformCache { cache ->
                cache.register(UserData.description)
                cache.register(GuildSettings.description)
                cache.register(GuildColours.description)
            }
        }

        commands {
            this.invokeOnMention = true
            this.slashCommands = false
            this.messageCommands = true
            this.defaultPrefix = config.discord.defaultPrefix

            prefix { default ->
                val settings = message.getGuildOrNull()?.getOrRetrieveSettings()

                return@prefix settings?.prefix ?: default
            }
        }

        presence {
            this.status = PresenceStatus.DoNotDisturb
            playing("Loading...")
        }

        members {
            this.fillPresences = false
            none()
        }

        intents {
            // enable this, so we can cache guilds and their channels
            enableEvent(GuildCreateEvent::class)
        }

        extensions {
            add(::TimeExtension)
            add(::MessageExtension)
            add(::LoggerExtension)
            add(::ColourExtension)
            add { bot -> DeveloperExtension(bot, listOf(config.developerId)) }
            add { bot -> SpaceSpeakExtension(bot, config.spaceSpeak) }
        }
    }

    bot.on<ReadyEvent> {
        gateway.editPresence {
            status = PresenceStatus.Online
            playing("Loaded :)")
        }
    }

    bot.start()

}
catch (t: Throwable) {
    val logger = KotlinLogging.logger("me.qbosst.bossbot.main")
    logger.error(t) { "Could not initialize Boss Bot" }
} finally {
    println("Press any key to EXIT the program...")
    Scanner(System.`in`).next()
}