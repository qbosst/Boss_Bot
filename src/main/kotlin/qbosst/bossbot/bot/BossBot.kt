package qbosst.bossbot.bot

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.StatusChangeEvent
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMuteEvent
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.MessageUpdateEvent
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import qbosst.bossbot.bot.listeners.EventWaiter
import qbosst.bossbot.bot.listeners.Listener
import qbosst.bossbot.config.Config
import qbosst.bossbot.database.Database
import java.time.OffsetDateTime
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import kotlin.system.exitProcess

object BossBot {

    val LOG: Logger = LoggerFactory.getLogger(BossBot.javaClass)
    val shards: ShardManager
    val startUp: OffsetDateTime = OffsetDateTime.now()
    val threadpool: ScheduledExecutorService = Executors.newScheduledThreadPool(Config.Values.THREADPOOL_SIZE.getInt())

    init
    {
        Database.connect(
                host = Config.Values.DATABASE_HOST.toString(),
                user = Config.Values.DATABASE_USER.toString(),
                password = Config.Values.DATABASE_PASSWORD.toString()
        )
        shards = connect(GatewayIntent.fromEvents(
                MessageReceivedEvent::class.java,
                MessageDeleteEvent::class.java,
                MessageUpdateEvent::class.java,
                GuildVoiceJoinEvent::class.java,
                GuildVoiceMoveEvent::class.java,
                GuildVoiceMuteEvent::class.java,
                GuildVoiceLeaveEvent::class.java,
                StatusChangeEvent::class.java,
                GuildLeaveEvent::class.java,
                GuildUnbanEvent::class.java,
                GuildMemberJoinEvent::class.java,
                GenericGuildMessageReactionEvent::class.java
        ))

        Runtime.getRuntime().addShutdownHook(object : Thread("Boss Bot Shutdown Hook")
        {
            override fun run()
            {
                LOG.info("Boss bot is shutting down!")
                shards.shutdown()
            }
        })
    }

    private fun connect(intents: Collection<GatewayIntent> = enumValues<GatewayIntent>().toMutableSet()): ShardManager
    {
        for(x in 0..5)
        {
            try
            {
                return DefaultShardManagerBuilder.create(intents)
                        .setToken(Config.Values.DISCORD_TOKEN.toString())
                        .setStatus(OnlineStatus.DO_NOT_DISTURB)
                        .setActivity(Activity.of(Activity.ActivityType.DEFAULT, "Loading..."))
                        .addEventListeners(EventWaiter, Listener)
                        .build()
            }
            catch (e: Exception)
            {
                LOG.error("Caught Exception: $e")
                runBlocking {
                    delay(5000)
                }
            }
        }
        exitProcess(0)
    }
}