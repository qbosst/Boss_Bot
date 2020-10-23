package me.qbosst.bossbot.bot

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory
import me.qbosst.bossbot.Launcher
import me.qbosst.bossbot.config.Config
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
    val shards: ShardManager
    val startUp: OffsetDateTime = OffsetDateTime.now()
    val threadpool: ScheduledExecutorService = Executors.newScheduledThreadPool(Config.Values.THREADPOOL_SIZE.getIntOrDefault())

    private val name = this::class.java.simpleName

    init
    {
        // Connects to the database
        Database.connect(
                host = Config.Values.DATABASE_URL.getStringOrDefault(),
                user = Config.Values.DATABASE_USER.getStringOrDefault(),
                password = Config.Values.DATABASE_PASSWORD.getStringOrDefault()
        )
        // Connects to discord
        shards = connectDiscord(Config.Values.DISCORD_TOKEN.getStringOrDefault())

        Runtime.getRuntime().addShutdownHook(object : Thread("$name Shutdown Hook")
        {
            override fun run()
            {
                LOG.info("$name is shutting down!")
                shards.shutdown()
            }
        })
    }

    /**
     *  Used to connect the bot to discord
     *
     *  @param token The token to connect to discord with
     *
     *  @return shard manager
     */
    private fun connectDiscord(token: String): ShardManager
    {
        // Get event listeners
        val listeners = loadObjectOrClass(Launcher::class.java.`package`.name, EventListener::class.java)

        LOG.debug("Registered ${listeners.size} listener(s): ${listeners.joinToString(", ")}")

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

        LOG.debug("Registered ${events.size} events(s): ${events.joinToString(", ") { it.simpleName }}")

        return DefaultShardManagerBuilder.create(GatewayIntent.fromEvents(events))
                .setToken(token)
                .setStatus(OnlineStatus.DO_NOT_DISTURB)
                .setActivity(Activity.of(Activity.ActivityType.DEFAULT, "Loading..."))
                .addEventListeners(listeners)
                .setAudioSendFactory(NativeAudioSendFactory())
                .setChunkingFilter(ChunkingFilter.ALL)
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                .build()
    }
}