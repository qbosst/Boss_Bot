package me.qbosst.bossbot

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory
import dev.minn.jda.ktx.CoroutineEventManager
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.qbosst.bossbot.config.BotConfig
import me.qbosst.bossbot.database.manager.UserData
import me.qbosst.bossbot.database.tables.*
import me.qbosst.bossbot.listeners.Listener
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.ChunkingFilter
import net.dv8tion.jda.api.utils.MemberCachePolicy
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.transactionManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*
import kotlin.reflect.KFunction
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.jvmErasure

object BossBot
{
    private val LOG: Logger = LoggerFactory.getLogger(BossBot::class.java)

    lateinit var api: ShardManager
        private set

    lateinit var config: BotConfig
        private set

    lateinit var database: Database
        private set

    lateinit var listener: Listener
        private set

    fun init(config: BotConfig) {
        require(!this::api.isInitialized) { "Boss Bot has already been initialised!" }

        this.config = config

        this.listener = Listener(config.cacheSize)

        this.database = connectDb(config.databaseUrl, config.databaseUser, config.databasePassword)

        this.api = buildApi(
            token = config.discordToken,
            listeners = listOf(listener),
            enableIntents = listOf(GatewayIntent.GUILD_MEMBERS)
        )
    }

    private fun getIntents(
        listeners: Collection<Any>,
        enableIntents: Collection<GatewayIntent> = listOf(),
    ): Set<GatewayIntent> {
        LOG.info("Registering Event Listeners...")

        // find what events listeners use and convert them to gateway intents
        return listeners.asSequence()
            .map { listener ->
                LOG.debug("Registering ${listener::class.simpleName}...")

                @Suppress("UNCHECKED_CAST")
                listener::class.members.asSequence()
                        // filter methods
                    .filterIsInstance<KFunction<*>>()
                        // map methods to method parameters
                    .map { function -> function.valueParameters }.flatten()
                        // map kotlin class to java class
                    .map { parameter -> parameter.type.jvmErasure.javaObjectType }
                        // check if class extends GenericEvent
                    .filter { clazz -> GenericEvent::class.java.isAssignableFrom(clazz) }
                        // map to Generic Event Class
                    .map { clazz -> clazz as Class<out GenericEvent> }
            }
            .flatten()
            .toList()
                // convert list of classes to list of gateway intents
            .let { events -> GatewayIntent.fromEvents(events) }
                // add the intents that we want to include into this list
            .plus(enableIntents)
    }

    private fun buildApi(
        token: String,
        listeners: Collection<Any>,
        enableIntents: Collection<GatewayIntent> = listOf()
    ): ShardManager {
        LOG.info("Building Shard Manager...")
        val intents = getIntents(listeners, enableIntents)
        LOG.debug("Applying intents: {}", intents)

        return DefaultShardManagerBuilder.create(token, intents).apply {
            // listeners
            setEventManagerProvider { CoroutineEventManager() }
            addEventListeners(listeners)

            // presence
            setActivity(Activity.playing("Loading..."))
            setStatus(OnlineStatus.DO_NOT_DISTURB)

            // music
            setAudioSendFactory(NativeAudioSendFactory())

            // caching
            setMemberCachePolicy(MemberCachePolicy.ALL)
            setChunkingFilter(ChunkingFilter.NONE)
        }.build()
    }

    private fun connectDb(url: String, user: String, password: String): Database {
        val database = Database.connect(
            url = "jdbc:mysql://$url",
            user = user,
            password = password
        )

        // create tables
        val tables = listOf(
            GuildSettingsTable, GuildColoursTable, MemberDataTable, UserDataTable, UserSpaceMessagesTable
        )
        transaction {
            tables.forEach { table ->
                SchemaUtils.createMissingTablesAndColumns(table)
            }
        }

        return database
    }

    @JvmStatic
    fun main(args: Array<String>) {
        try {
            // json parser
            val json = Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
                prettyPrint = true
            }

            val file = File(BotConfig.DIRECTORY)

            // if config file does not exist, generate one and throw exception
            if(!file.exists()) {
                val default = BotConfig()
                file.writeBytes(json.encodeToString(default).encodeToByteArray())
                throw Exception("A config file has been generated at '${file.absolutePath}', please fill in the required values")
            }

            // otherwise load config file
            val config = json.decodeFromString<BotConfig>(file.readBytes().decodeToString())

            // start the bot
            init(config)
        }
        catch (e: Throwable) {
            LOG.error("Caught an unhandled exception while starting the Bot...", e)

            if(e is Error)
                throw e
        }
        finally {
            // wait for input, prevents terminal from closing
            Scanner(System.`in`).next()
        }
    }
}