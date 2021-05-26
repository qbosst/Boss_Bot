package me.qbosst.bossbot

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.utils.env
import com.kotlindiscord.kord.extensions.utils.loadModule
import dev.kord.gateway.Intent
import me.qbosst.bossbot.database.DatabaseManager
import me.qbosst.bossbot.database.dao.User
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
        }

        messageCommands {
            defaultPrefix = "b!"
        }

        hooks {

            setup {
                // database module
                loadModule {
                    single {
                        DatabaseManager {
                            host = "localhost:3306/bossbot?serverTimezone=BST"
                            username = "root"
                            password = "root"

                            tables {
                                add(UsersTable)
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

            kord {
                forDescription(User.description, lruCache(1000))
            }

            transformCache { cache ->
                cache.register(User.description)
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
