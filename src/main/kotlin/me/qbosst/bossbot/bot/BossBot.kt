package me.qbosst.bossbot.bot

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import me.qbosst.bossbot.bot.listeners.EventWaiter
import me.qbosst.bossbot.bot.listeners.Listener
import me.qbosst.bossbot.config.Config
import me.qbosst.bossbot.database.Database
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import javax.security.auth.login.LoginException
import kotlin.system.exitProcess

object BossBot {

    val LOG: Logger = LoggerFactory.getLogger(BossBot.javaClass)
    val shards: ShardManager
    val startUp: OffsetDateTime = OffsetDateTime.now()
    val threadpool: ScheduledExecutorService = Executors.newScheduledThreadPool(Config.Values.THREADPOOL_SIZE.getIntOrDefault())

    init
    {
        Database.connect(
                host = Config.Values.DATABASE_URL.getStringOrDefault(),
                user = Config.Values.DATABASE_USER.getStringOrDefault(),
                password = Config.Values.DATABASE_PASSWORD.getStringOrDefault()
        )
        shards = connect(Config.Values.DISCORD_TOKEN.getString())

        Runtime.getRuntime().addShutdownHook(object : Thread("Boss Bot Shutdown Hook")
        {
            override fun run()
            {
                LOG.info("Boss bot is shutting down!")
                shards.shutdown()
            }
        })
    }

    private fun connect(token: String?, intents: Collection<GatewayIntent> = enumValues<GatewayIntent>().toMutableSet()): ShardManager
    {
        if(token.isNullOrEmpty())
            throw LoginException("The token may not be null or empty!")
        else
        {
            for(x in 0..5)
            {
                try
                {
                    return DefaultShardManagerBuilder.create(intents)
                            .setToken(token)
                            .setStatus(OnlineStatus.DO_NOT_DISTURB)
                            .setActivity(Activity.of(Activity.ActivityType.DEFAULT, "Loading..."))
                            .addEventListeners(EventWaiter, Listener)
                            .setAudioSendFactory(NativeAudioSendFactory())
                            .build()
                }
                catch (e: Exception)
                {
                    LOG.error("Caught Exception: $e")
                    runBlocking()
                    {
                        delay(5000)
                    }
                }
            }
            exitProcess(0)
        }
    }
}