package me.qbosst.bossbot

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.checks.isNotBot
import com.kotlindiscord.kord.extensions.i18n.SupportedLocales
import com.kotlindiscord.kord.extensions.sentry.SentryAdapter
import com.kotlindiscord.kord.extensions.utils.env
import com.kotlindiscord.kord.extensions.utils.loadModule
import dev.kord.cache.map.MapLikeCollection
import dev.kord.common.Color
import dev.kord.common.entity.PresenceStatus
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.cache.data.MessageData
import dev.kord.core.event.gateway.ReadyEvent
import dev.kord.gateway.Intent
import dev.kord.gateway.editPresence
import me.qbosst.bossbot.database.DatabaseManager
import me.qbosst.bossbot.database.dao.Guild
import me.qbosst.bossbot.database.dao.User
import me.qbosst.bossbot.database.dao.getGuildDAO
import me.qbosst.bossbot.database.tables.GuildsTable
import me.qbosst.bossbot.database.tables.SpaceSpeakTable
import me.qbosst.bossbot.database.tables.UsersTable
import me.qbosst.bossbot.extensions.*
import me.qbosst.bossbot.extensions.image.ImageExtension
import me.qbosst.bossbot.extensions.vote.VoteExtension
import me.qbosst.bossbot.util.getColour
import me.qbosst.bossbot.util.mapLikeCollection
import me.qbosst.bossbot.util.random
import mu.KLogger
import mu.KotlinLogging
import org.koin.dsl.bind
import org.koin.java.KoinJavaComponent.getKoin

suspend fun main() = try {
    val bot = ExtensibleBot(env("token")!!) {
        intents {
            +Intent.Guilds
            +Intent.GuildMessages
            +Intent.DirectMessages
            +Intent.GuildMessageReactions
            +Intent.DirectMessagesReactions
        }

        members {
            fillPresences = false
            none()
        }

        extensions {
            sentry = false

            add(::EconomyExtension)
            add(::DeveloperExtension)
            add(::CasinoExtension)
            add(::MiscExtension)
            add(::LoggerExtension)
            add(::SpaceSpeakExtension)
            add(::VoteExtension)
            add(::ImageExtension)

            help {
                colour {
                    // TODO: use bot's most dominant pfp colour instead of random colour
                    getGuild()?.getMemberOrNull(kord.selfId)?.getColour() ?: Color.random()
                }
            }
        }


        messageCommands {
            defaultPrefix = "b!"

            prefix { defaultPrefix ->
                message.getGuildOrNull()?.getGuildDAO()?.prefix ?: defaultPrefix
            }

            check(::isNotBot)
        }

        slashCommands {
            enabled = true

            env("testGuild")?.toLongOrNull()?.let {
                defaultGuild(it)
            }
        }

        hooks {
            created {
                // database module
                loadModule {
                    single {
                        DatabaseManager {
                            host = env("database.host")!!
                            username = env("database.username")!!
                            password = env("database.password")!!

                            tables {
                                add(UsersTable)
                                add(GuildsTable)
                                add(SpaceSpeakTable)
                            }
                        }
                    } bind DatabaseManager::class
                }

                // config module
                loadModule {
                    single {
                        // TODO: load config module
                    }
                }
            }

            beforeExtensionsAdded {
                // initialize database tables
                getKoin().get<DatabaseManager>().init()
            }

            afterKoinSetup {
                getKoin().get<SentryAdapter>().init {
                    dsn = env("sentry")!!
                    tracesSampleRate = 1.0
                    environment = "production"
                }
            }

            beforeStart {
                val kord = getKoin().get<Kord>()
                val intents = kord.resources.intents.values.map { it::class.simpleName }
                logger.info { "Connecting with the following intents: $intents" }
            }
        }

        cache {
            cachedMessages = null

            kord {
                forDescription(User.description, lruCache(1000))
                forDescription(Guild.description, lruCache(1000))

                messages(
                    @Suppress("UNCHECKED_CAST") // compiler being dumb
                    mapLikeCollection(MessageCache(5) as MapLikeCollection<MessageData, Snowflake>)
                )

                emojis(none())
                webhooks(none())
                presences(none())
                voiceState(none())
            }

            transformCache { cache ->
                cache.register(User.description)
                cache.register(Guild.description)
            }
        }

        i18n {
            defaultLocale = SupportedLocales.ENGLISH
        }

        presence {
            status = PresenceStatus.DoNotDisturb
            playing("Loading...")
        }
    }

    bot.on<ReadyEvent> {
        gateway.editPresence {
            status = PresenceStatus.Online
            playing("Loaded :D")
        }
    }

    bot.start()

} catch (e: Exception) {
    val logger: KLogger = KotlinLogging.logger("me.qbosst.bossbot.main")
    logger.error(e) { "Could not initialize Boss Bot." }
} finally {
    println("Press any key to EXIT the program...")
    readLine()
}
