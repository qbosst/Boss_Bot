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
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService


object BossBot
{
    private val log: Logger = LoggerFactory.getLogger(BossBot::class.java)
    val api: ShardManager
    val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(6)

    init
    {
        // Connects to the database
        Database.connect(
                host = BotConfig.database_url,
                user = BotConfig.database_user,
                password = BotConfig.database_password
        )

        // Connects to discord
        api = getApi(
                token = BotConfig.discord_token,
                enableIntents = setOf(GatewayIntent.GUILD_MEMBERS, GatewayIntent.DIRECT_MESSAGE_REACTIONS)
        )
    }

    /**
     *  Used to connect the bot to discord
     *
     *  @param token The token to connect to discord with
     *  @param enableIntents Any additional intents to enable
     *
     *  @return shard manager
     */
    private fun getApi(token: String, enableIntents: Collection<GatewayIntent> = listOf()): ShardManager
    {
        log.info("Registering Event Listeners...")
        val listeners = loadObjectOrClass(Launcher::class.java.`package`.name, EventListener::class.java)

        val intents = listeners
                // get events from listener
                .map { listener ->
                    log.debug("Registering ${listener::class.java.simpleName}")

                    @Suppress("UNCHECKED_CAST")
                    listener::class.java.declaredMethods
                            .map { it.parameters.toList() }.flatten()
                            .filter { parameter -> GenericEvent::class.java.isAssignableFrom(parameter.type) }
                            .map { parameter -> parameter.type as Class<out GenericEvent> }
                }
                // convert events to intents
                .flatten()
                .let { events -> GatewayIntent.fromEvents(events) }
                .plus(enableIntents)

        return DefaultShardManagerBuilder.create(token, intents)
                .setChunkingFilter(ChunkingFilter.ALL) // TODO set to NONE
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .setActivity(Activity.playing("Loading..."))
                .setStatus(OnlineStatus.DO_NOT_DISTURB)
                .setAudioSendFactory(NativeAudioSendFactory())
                .addEventListeners(listeners)
                .build()
    }
}