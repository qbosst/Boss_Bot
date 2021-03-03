package me.qbosst.bossbot

import com.kotlindiscord.kord.extensions.utils.authorIsBot
import dev.kord.cache.map.MapLikeCollection
import dev.kord.common.entity.PresenceStatus
import dev.kord.common.entity.Snowflake
import dev.kord.core.cache.data.MessageData
import dev.kord.core.enableEvent
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.core.event.guild.GuildCreateEvent
import dev.kord.core.event.interaction.InteractionCreateEvent
import dev.kord.gateway.editPresence
import me.qbosst.bossbot.commands.CommandRegistry
import me.qbosst.bossbot.config.BotConfig
import me.qbosst.bossbot.database.dao.GuildSettings
import me.qbosst.bossbot.database.dao.UserData
import me.qbosst.bossbot.database.dao.getSettings
import me.qbosst.bossbot.database.models.GuildColours
import me.qbosst.bossbot.database.tables.GuildColoursTable
import me.qbosst.bossbot.database.tables.GuildSettingsTable
import me.qbosst.bossbot.database.tables.SpaceSpeakTable
import me.qbosst.bossbot.database.tables.UserDataTable
import me.qbosst.bossbot.extensions.*
import me.qbosst.bossbot.util.cache.MessageCache
import me.qbosst.bossbot.util.defaultCheck
import mu.KLogger
import mu.KotlinLogging

suspend fun main() = try {
    // get the config file for the bot
    val config = BotConfig.from("config.json")

    // configure and create an instance of boss bot
    val bot = BossBot {
        this.config = config

        database {
            host = config.database.host
            username = config.database.username
            password = config.database.password

            tables {
                add(GuildSettingsTable)
                add(UserDataTable)
                add(SpaceSpeakTable)
                add(GuildColoursTable)
            }
        }

        cache {
            cachedMessages = config.discord.messageCacheSize

            kord {
                forDescription(GuildSettings.description, lruCache(config.discord.defaultCacheSize))
                forDescription(UserData.description, lruCache(config.discord.defaultCacheSize))
                forDescription(GuildColours.description, lruCache(config.discord.defaultCacheSize))

                emojis(none())
                presences(none())
                webhooks(none())

                val messageCache = MessageCache(cachedMessages!!) { message -> !message.authorIsBot }
                @Suppress("UNCHECKED_CAST")
                messages(mapLikeCollection(messageCache as MapLikeCollection<MessageData, Snowflake>))
            }

            transformCache { cache ->
                cache.register(GuildSettings.description)
                cache.register(UserData.description)
                cache.register(GuildColours.description)
            }
        }

        commands {
            invokeOnMention = true
            slashCommands = false
            messageCommands = true
            defaultPrefix = config.discord.defaultPrefix

            prefix { default -> getGuild()?.getSettings()?.prefix ?: default }

            messageRegistry { bot ->
                CommandRegistry(bot) {
                    globalCheck(::defaultCheck)
                }
            }
        }

        presence {
            status = PresenceStatus.DoNotDisturb
            playing("loading...")
        }

        members {
            fillPresences = false
            none()
        }

        intents {
            // adds the guilds and direct messages intent, allowing us to cache stuff
            enableEvent(GuildCreateEvent::class)
        }

        extensions {
            add(::LoggerExtension)
            add(::MessageExtension)
            add(::ColourExtension)
            add { bot -> MiscExtension(bot, config.discord.voteLinks) }
            add { bot -> DeveloperExtension(bot, listOf(config.discord.developerId)) }
            add { bot -> SpaceSpeakExtension(bot, config.spaceSpeak) }
        }
    }

    bot.on<ReadyEvent> {
        gateway.editPresence {
            status = PresenceStatus.Online
            playing("Loaded :)")
        }
    }

    // start the bot
    bot.start()

} catch (e: Exception) {
    val logger: KLogger = KotlinLogging.logger("me.qbosst.bossbot.main")
    logger.error(e) { "Could not initialize Boss Bot" }
} finally {
    // exit program
    println("Press any key to EXIT the program...")
    readLine()
}