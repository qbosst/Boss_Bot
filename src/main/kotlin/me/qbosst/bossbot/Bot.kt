package me.qbosst.bossbot

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.builders.ExtensibleBotBuilder
import dev.kord.cache.map.MapLikeCollection
import dev.kord.cache.map.internal.MapEntryCache
import dev.kord.common.entity.DiscordShard
import dev.kord.core.cache.Generator
import dev.kord.core.cache.KordCacheBuilder
import dev.kord.core.enableEvent
import dev.kord.core.enableEvents
import dev.kord.core.event.Event
import dev.kord.core.event.interaction.InteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.gateway.Intents
import me.qbosst.bossbot.config.BotConfig
import me.qbosst.bossbot.database.DatabaseManager
import kotlin.reflect.KClass

class BossBot(
    val dbManager: DatabaseManager,
    config: BotConfig,
    settings: Builder,
): ExtensibleBot(settings, config.discord.token) {

    override suspend fun setup() {
        dbManager.init()

        super.setup()
    }

    override suspend fun start() {
        // login to Discord
        kord.run {
            gateway.start(resources.token) {
                shard = DiscordShard(0, resources.shards.totalShards)
                presence(settings.presenceBuilder)
                name = "kord"

                // intents include the bare minimum we need to use our extensions + user-defined intents
                intents = calculateIntents().plus(resources.intents).also {
                    val intents = it.values

                    // display the intents the bot is using
                    if(intents.isEmpty()) {
                        logger.warn { "You do not have any intents registered! You will not receive any events!" }
                    } else {
                        val intentsName = intents.map { intent -> intent::class.simpleName }
                        logger.debug { "Connecting to Discord with the following intents: $intentsName" }
                    }
                }
            }
        }
    }

    /**
     * Calculate the intents needed by the extensions based on what events they use
     */
    private fun calculateIntents() = Intents {
        @Suppress("UNCHECKED_CAST")
        val events: Iterable<KClass<out Event>> = extensions.asSequence()
                // map extensions to the event handlers they use
            .map { (_, extension) -> extension.eventHandlers }.flatten()
                // cast event handlers to an event class, or null if the cast failed
            .mapNotNull { handler -> handler.type as? KClass<out Event> }
            .asIterable()

        enableEvents(events)

        // if the bot uses message commands, enable the event needed to receive them
        if(messageCommands.commands.isNotEmpty()) {
            enableEvent(MessageCreateEvent::class)
        }
        // if the bot uses slash commands, enable the event needed to receive them
        if(slashCommands.commands.isNotEmpty()) {
            enableEvent(InteractionCreateEvent::class)
        }
    }

    /**
     * Builder class used to create an instance of [BossBot]
     */
    class Builder: ExtensibleBotBuilder() {
        /**
         * Object containing the bot's config
         */
        lateinit var config: BotConfig

        /**
         * Builder that shouldn't be directly set by the user
         */
        val dbBuilder: DatabaseManager.Builder = DatabaseManager.Builder()

        /**
         * DSL function to configure the bot's database.
         */
        fun database(builder: DatabaseManager.Builder.() -> Unit) {
            dbBuilder.builder()
        }

        override suspend fun build(token: String): BossBot {
            val dbManager = dbBuilder.build()
            val bot = BossBot(dbManager, config, this)

            bot.setup()

            extensionsBuilder.extensions.forEach { extension -> bot.addExtension(extension) }

            return bot
        }

        fun <K, V: Any> KordCacheBuilder.mapLikeCollection(map: MapLikeCollection<K, V>): Generator<K, V> = {
                cache, description -> MapEntryCache(cache, description, map)
        }
    }

    companion object {
        /**
         * DSL function to create an instance of [BossBot] using the [Builder]
         */
        suspend operator fun invoke(init: Builder.() -> Unit): BossBot = Builder()
            .apply(init)
            .run { build(config.discord.token) }
    }
}