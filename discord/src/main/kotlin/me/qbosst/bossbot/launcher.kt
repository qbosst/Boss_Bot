package me.qbosst.bossbot

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.utils.env
import com.kotlindiscord.kord.extensions.utils.loadModule
import dev.kord.gateway.Intent
import me.qbosst.bossbot.database.DatabaseManager
import me.qbosst.bossbot.database.dao.Guild
import me.qbosst.bossbot.database.dao.User
import me.qbosst.bossbot.database.dao.getGuildDAO
import me.qbosst.bossbot.database.tables.GuildsTable
import me.qbosst.bossbot.database.tables.UsersTable
import me.qbosst.bossbot.extensions.*
import mu.KLogger
import mu.KotlinLogging
import org.koin.dsl.bind

suspend fun main() = try {
    val bot = ExtensibleBot(env("token")!!) {
        intents {
            +Intent.Guilds
            +Intent.GuildMessages
            +Intent.DirectMessages
        }

        members {
            fillPresences = false
            none()
        }

        extensions {
            add(::EconomyExtension)
            add(::DeveloperExtension)
            add(::CasinoExtension)
            add(::MiscExtension)
        }

        messageCommands {
            defaultPrefix = "b!"

            prefix { defaultPrefix ->
                message.getGuildOrNull()?.getGuildDAO()?.prefix ?: defaultPrefix
            }

            check(defaultMessageCheck())
        }

        slashCommands {
            enabled = true
        }

        hooks {

            setup {
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

            beforeStart {
                // initialize database tables
                getKoin().get<DatabaseManager>().init()
            }
        }

        cache {
            cachedMessages = 10_000

            kord {
                forDescription(User.description, lruCache(1000))
                forDescription(Guild.description, lruCache(1000))

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
    }

    bot.start()

} catch (e: Exception) {
    val logger: KLogger = KotlinLogging.logger("me.qbosst.bossbot.main")
    logger.error(e) { "Could not initialize Boss Bot."}
} finally {
    println("Press any key to EXIT the program...")
    readLine()
}
