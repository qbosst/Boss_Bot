package me.qbosst.bossbot.config

import me.qbosst.bossbot.bot.BossBot
import me.qbosst.bossbot.util.isNumeric
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.nio.file.Files
import kotlin.system.exitProcess

object Config {

    private const val indentFactor = 4
    private val config: File = File("config.json")
    private lateinit var json: JSONObject

    init
    {
        // Check if config exists
        if(!config.exists()) {
            create()
        }

        // Loads the config file
        BossBot.LOG.info("Using config file at path: '${config.absolutePath}'")
        reload()
    }

    fun reload(shutdown: Boolean = false): Collection<Values>
    {
        try
        {
            val content = Files.readAllLines(config.toPath()).joinToString("")
            json = JSONObject(content)
        }
        catch (e: JSONException)
        {
            BossBot.LOG.error("There was a problem while trying to read the config file. A new blank one has been generated: $e")
            config.delete()
            create(shutdown)
        }
        return validate(shutdown)
    }

    private fun create(shutdown: Boolean = true)
    {
        // Tries to create a new config file
        if(config.createNewFile())
        {
            generate()
            BossBot.LOG.info("A config file has been generated at '${config.absolutePath}'. Please fill in the required missing values.")
        }
        else
        {
            throw IOException("A config file could not be generated.")
        }
        if(shutdown) exitProcess(0)
    }

    // Generates a new config file
    private fun generate()
    {
        json = JSONObject()
        for(value in Values.values())
        {
            json.put(value.valueName, value.defaultValue)
        }
        Files.write(config.toPath(), json.toString(indentFactor).toByteArray())
    }

    // Validates the current config file, making sure all needed values are present
    private fun validate(shutdown: Boolean = true): Collection<Values>
    {
        val invalid = mutableListOf<Values>()

        // Loops through every required value and checks if it is present, if not it will add it to the config
        for(v in Values.values())
        {
            if(!json.has(v.valueName))
            {
                json.put(v.valueName, v.defaultValue)
                if(!v.nullable && v.defaultValue.isEmpty())
                {
                    invalid.add(v)
                }
            }
            else if(json.get(v.valueName).toString().isEmpty() && !v.nullable)
            {
                invalid.add(v)
            }
        }
        Files.write(config.toPath(), json.toString(indentFactor).toByteArray())

        if(invalid.isNotEmpty() && shutdown)
        {
            BossBot.LOG.error("Please fill in the required config values: ${invalid.joinToString(", ") { it.valueName }}");
            exitProcess(0)
        }

        return invalid

    }
    enum class Values(val valueName: String, val defaultValue: String, val nullable: Boolean)
    {
        DISCORD_TOKEN("discord.token", "", false),
        DATABASE_USER("database.user", "", false),
        DATABASE_PASSWORD("database.password", "", false),
        DATABASE_HOST("database.host", "", false),
        DEFAULT_PREFIX("default.prefix", "!", true),
        DEFAULT_CACHE_SIZE("default.cache.size", "1000", true),
        DEFAULT_GREETING("default.greeting", "This is the default greeting!", true),
        DEVELOPER_ID("developer.id", "0", true),
        DEEPAI_TOKEN("deepai.token", "", true),
        THREADPOOL_SIZE("threadpool.size", "3", true),
        MAX_COLOURS_PER_GUILD("guild.max_colours", "100", true)
        ;

        fun getInt(): Int
        {
            return if(json.has(valueName) && !json.getString(valueName).isNumeric()) json.getInt(valueName) else defaultValue.toInt()
        }

        fun getLong(): Long
        {
            return if(json.has(valueName) && !json.getString(valueName).isNumeric()) json.getLong(valueName) else defaultValue.toLong()
        }

        override fun toString(): String
        {
            return if(json.has(valueName)) json.get(valueName).toString() else defaultValue
        }
    }
}