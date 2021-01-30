package me.qbosst.bossbot

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.builders.ExtensibleBotBuilder
import dev.kord.common.entity.DiscordShard
import dev.kord.core.enableEvents
import dev.kord.core.event.Event
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.gateway.Intents
import me.qbosst.bossbot.config.BotConfig
import me.qbosst.bossbot.database.DatabaseManager
import me.qbosst.bossbot.database.DatabaseManagerBuilder
import mu.KotlinLogging
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

private val botLogger = KotlinLogging.logger {  }

class BossBot(
    settings: BossBotBuilder,
    val database: DatabaseManager,
    val config: BotConfig
): ExtensibleBot(settings, settings.config.discordToken) {

    @OptIn(ExperimentalStdlibApi::class)
    override suspend fun start() {
        database.connect()

        kord.apply {
            gateway.start(resources.token) {
                shard = DiscordShard(0, resources.shardCount)
                presence(settings.presenceBuilder)
                intents = Intents(settings.intentsBuilder ?: {
                    // get events used and calculate intents
                    @Suppress("UNCHECKED_CAST")
                    val events: Iterable<KClass<out Event>> = extensions.asSequence()
                        .map { (_, extension) -> extension.eventHandlers }.flatten()
                        .map { handlers -> handlers.type }
                        .filter { clazz -> clazz.isSubclassOf(Event::class) }
                        .map { clazz -> clazz as KClass<out Event> }
                        .plus(
                            buildSet {
                                val bot = this@BossBot

                                // check if bot uses commands, if so add event used to receive command events
                                if(bot.commands.isNotEmpty()) {
                                    add(MessageCreateEvent::class)
                                }
                            }
                        )
                        .asIterable()

                    enableEvents(events)

                    val intents = flags().values.map { intent -> intent::class.simpleName }
                    if(intents.isEmpty()) {
                        botLogger.warn { "You do not have any intents registered! You will not receive any events" }
                    } else {
                        botLogger.info { "You are connecting to Discord with the following intents $intents" }
                    }
                })
                name = "kord"
            }
        }
    }

    companion object {
        /**
         * DSL Function for creating a bot instance.
         */
        suspend operator fun invoke(init: BossBotBuilder.() -> Unit): BossBot {
            val builder = BossBotBuilder().apply(init)
            val token = builder.config.discordToken

            return builder.build(token)
        }
    }
}

class BossBotBuilder: ExtensibleBotBuilder() {
    lateinit var config: BotConfig

    lateinit var databaseBuilder: DatabaseManagerBuilder

    fun database(builder: DatabaseManagerBuilder.() -> Unit) {
        this.databaseBuilder = DatabaseManagerBuilder().apply(builder)
    }

    override suspend fun build(token: String): BossBot {
        require(::config.isInitialized) { "Please provide a config" }
        require(::databaseBuilder.isInitialized) { "Please use the database builder" }

        val database = databaseBuilder.build()
        val bot = BossBot(this, database, config)

        bot.setup()

        extensionsBuilder.extensions.forEach { bot.addExtension(it) }

        return bot
    }
}