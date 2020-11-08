package me.qbosst.bossbot.bot

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory
import me.qbosst.bossbot.Launcher
import me.qbosst.bossbot.config.BotConfig
import me.qbosst.bossbot.database.Database
import me.qbosst.bossbot.util.loadObjectOrClass
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.ChunkingFilter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

/**
 *  Main class for starting boss bot
 */
object BossBot {

    val LOG: Logger = LoggerFactory.getLogger(BossBot::class.java)
    val SHARDS_MANAGER: ShardManager
    val START_UP: OffsetDateTime = OffsetDateTime.now()
    val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(6)

    private val name = this::class.java.simpleName

    init
    {
        // Connects to the database
        Database.connect(
                host = BotConfig.database_url,
                user = BotConfig.database_user,
                password = BotConfig.database_password
        )

        // Connects to discord
        SHARDS_MANAGER = getShardsManager(
                token = BotConfig.discord_token,
                intents = setOf(GatewayIntent.GUILD_MEMBERS)
        )

        Runtime.getRuntime().addShutdownHook(object : Thread("$name Shutdown Hook")
        {
            override fun run()
            {
                LOG.info("$name is shutting down!")
                SHARDS_MANAGER.shutdown()
            }
        })
    }

    /**
     *  Used to connect the bot to discord
     *
     *  @param token The token to connect to discord with
     *  @param intents Any additional intents to enable
     *
     *  @return shard manager
     */
    private fun getShardsManager(token: String, intents: Set<GatewayIntent> = setOf()): ShardManager
    {
        // Get event listeners
        val listeners = loadObjectOrClass(Launcher::class.java.`package`.name, EventListener::class.java)

        LOG.info("Registered ${listeners.size} listener(s): ${listeners.joinToString(", ") { it::class.java.simpleName }}")

        val events = mutableListOf<Class<out GenericEvent>>()
        for(listener in listeners)
        // Gets all the methods in the class
            for(method in listener::class.java.declaredMethods)
            // Gets the parameters of those methods in the class
                for(parameter in method.parameters)
                // Checks if the parameter is subclass of a generic event
                    if(Event::class.java.isAssignableFrom(parameter.type))
                    // If the list does not already contain the event, add it to the events list
                        if(!events.contains(parameter.type))
                            @Suppress("UNCHECKED_CAST") events.add(parameter.type as Class<out GenericEvent>)

        LOG.info("Registered ${events.size} events(s): ${events.joinToString(", ") { it.simpleName }}")

        return DefaultShardManagerBuilder.create(GatewayIntent.fromEvents(events))
                .setToken(token)
                .setStatus(OnlineStatus.DO_NOT_DISTURB)
                .setActivity(Activity.of(Activity.ActivityType.DEFAULT, "Loading..."))
                .addEventListeners(listeners)
                .setAudioSendFactory(NativeAudioSendFactory())
                .setChunkingFilter(ChunkingFilter.ALL)
                .enableIntents(intents)
                .build()
    }
}