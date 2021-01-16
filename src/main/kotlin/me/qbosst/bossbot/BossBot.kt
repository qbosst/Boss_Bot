package me.qbosst.bossbot

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.builders.StartBuilder
import dev.kord.common.entity.PresenceStatus
import dev.kord.common.entity.Snowflake
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.qbosst.bossbot.config.BotConfig
import me.qbosst.bossbot.database.DatabaseManager
import me.qbosst.bossbot.extensions.ColourExtension
import mu.KotlinLogging
import org.jetbrains.exposed.sql.Database
import org.koin.core.logger.Level
import java.io.File
import java.util.Scanner

private val botLogger = KotlinLogging.logger {  }

class BossBot(
    val config: BotConfig,
    val json: Json = Json {},
): ExtensibleBot(
    token = config.discordToken,
    prefix = config.defaultPrefix,
    messageCacheSize = config.messageCacheSize
) {
    lateinit var dbManager: DatabaseManager

    init {
        addExtension { ColourExtension(this, config.defaultCacheSize) }
    }

    override suspend fun start(builder: suspend StartBuilder.() -> Unit) {

        // connect to database
        dbManager = DatabaseManager(
            host = config.databaseHost,
            username = config.databaseUsername,
            password = config.databasePassword
        ).apply {
            connect()
        }

        super.start(builder)
    }
}

suspend fun main() {
    try {
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
            throw Exception("A config file has been generated at '${file.absolutePath}', please fill in the missing values")
        }

        // otherwise load config file
        val config = json.decodeFromString<BotConfig>(file.readText())

        // create instance of bot and start it
        val bot = BossBot(
            config = config,
            json = json
        ).apply {
            start {
                presence {
                    status = PresenceStatus.DoNotDisturb
                    playing("Loading...")
                }
            }
        }

    } catch (t: Throwable) {
        botLogger.error(t) { "Could not initialize Boss Bot "}
    } finally {
        println("Press any key to EXIT the program...")
        Scanner(System.`in`).next()
    }
}