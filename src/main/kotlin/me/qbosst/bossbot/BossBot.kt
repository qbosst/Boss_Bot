package me.qbosst.bossbot

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.builders.ExtensibleBotBuilder
import dev.kord.cache.map.MapLikeCollection
import dev.kord.cache.map.internal.MapEntryCache
import dev.kord.common.entity.DiscordShard
import dev.kord.core.cache.Generator
import dev.kord.core.cache.KordCacheBuilder
import dev.kord.core.cache.data.MessageData
import dev.kord.core.enableEvent
import dev.kord.core.enableEvents
import dev.kord.core.event.Event
import dev.kord.core.event.channel.ChannelCreateEvent
import dev.kord.core.event.guild.GuildCreateEvent
import dev.kord.core.event.guild.GuildUpdateEvent
import dev.kord.core.event.interaction.InteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.user.VoiceStateUpdateEvent
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

    override suspend fun start() {
        // connect to database
        database.connect()

        // add extensions, do this so that extensions can make database calls in setup methods.
        settings.extensionsBuilder.extensions.forEach { this.addExtension(it) }

        // connect to discord
        kord.apply {
            gateway.start(resources.token) {
                shard = DiscordShard(0, resources.shardCount)
                presence(settings.presenceBuilder)

                // use intents builder from bot builder as additional intents we want to use
                val enable = Intents.IntentsBuilder().apply(settings.intentsBuilder ?: {}).flags()

                intents = calculateIntents().plus(enable).also {
                    val intents = it.values
                    if(intents.isEmpty()) {
                        logger.warn { "You do not have any intents registered! You will not receive any events" }
                    } else {
                        logger.info { "You are connecting to Discord with the following intents ${intents.map { intent -> intent::class.simpleName }}" }
                    }
                }
                name = "kord"
            }
        }
    }

    /**
     * Calculates the intents that the bot needs from the extensions
     */
    private fun calculateIntents() = Intents {
        val bot = this@BossBot

        @Suppress("UNCHECKED_CAST")
        val events: Iterable<KClass<out Event>> = extensions.asSequence()
            // get events used by extensions
            .map { (_, extension) -> extension.eventHandlers }.flatten()
            .map { handler -> handler.type }
            .filter { clazz -> clazz.isSubclassOf(Event::class) }
            .map { clazz -> clazz as KClass<out Event> }
            .asIterable()

        enableEvents(events)

        if(bot.messageCommands.commands.isNotEmpty()) {
            enableEvent(MessageCreateEvent::class)
        }

        if(bot.slashCommands.commands.isNotEmpty()) {
            enableEvent(InteractionCreateEvent::class)
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

    fun <K, V : Any> KordCacheBuilder.map(map: MutableMap<K, V>): Generator<K ,V> = { cache, description ->
        MapEntryCache(cache, description, MapLikeCollection.from(map))
    }

    fun <K, V : Any> KordCacheBuilder.mapLikeCollection(map: MapLikeCollection<K, V>): Generator<K, V> = {
            cache, description ->
        MapEntryCache(cache, description, map)
    }

    override suspend fun build(token: String): BossBot {
        require(::config.isInitialized) { "Please provide a config" }
        require(::databaseBuilder.isInitialized) { "Please use the database builder" }

        val database = databaseBuilder.build()
        val bot = BossBot(this, database, config)

        bot.setup()

        return bot
    }
}