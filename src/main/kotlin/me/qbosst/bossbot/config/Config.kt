package me.qbosst.bossbot.config

import me.qbosst.bossbot.bot.BossBot
import me.qbosst.bossbot.util.getOrNull
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.nio.file.Files
import kotlin.system.exitProcess

object Config
{
    private const val indentFactor = 4
    private val config: File = File("config.json")
    private var json: JSONObject

    init
    {
        if(!config.exists())
        {
            if(config.createNewFile())
            {
                generate()
                BossBot.LOG.info("A config file has been generated at '${config.absolutePath}'. Please fill in the required missing values.")
                exitProcess(0)
            }
            else
                throw IOException("A config file could not be generated.")
        }
        else
        {
            val content = Files.readAllLines(config.toPath()).joinToString("")
            json = JSONObject(content)

            val invalid = validate()
            if(invalid.isNotEmpty())
            {
                BossBot.LOG.error("The following values are invalid; ${invalid.joinToString(", ") { it.name.toLowerCase() }}")

                for(value in invalid)
                    if(!json.has(value.name.toLowerCase()))
                        json.put(value.name.toLowerCase(), value.defaultValue)

                Files.write(config.toPath(), json.toString(indentFactor).toByteArray())
                exitProcess(0)
            }
        }
    }

    fun reload(): Collection<Values>
    {
        val content = Files.readAllLines(config.toPath()).joinToString("")
        json = JSONObject(content)

        return validate()
    }

    private fun generate()
    {
        json = JSONObject()

        for(value in enumValues<Values>())
            json.put(value.name.toLowerCase(), value.defaultValue)

        Files.write(config.toPath(), json.toString(indentFactor).toByteArray())
    }

    private fun validate(): Collection<Values>
    {
        val invalid = mutableSetOf<Values>()
        for(value in enumValues<Values>())
        {
            val name = value.name.toLowerCase()
            if(!value.isValid.invoke(if(json.has(name)) json.get(name) else null))
                invalid.add(value)
        }
        return invalid
    }

    enum class Values(val defaultValue: Any, val isValid: (Any?) -> Boolean = { it?.toString()?.isNotEmpty() ?: false })
    {
        DISCORD_TOKEN(""),
        DATABASE_USER(""),
        DATABASE_PASSWORD(""),
        DATABASE_URL(""),
        DEFAULT_PREFIX("!", { run()
        {
            val length = it?.toString()?.length ?: 0
            length > 0 && length < me.qbosst.bossbot.database.tables.GuildSettingsDataTable.max_prefix_length
        }}),
        DEFAULT_CACHE_SIZE(500, { (it?.toString()?.toIntOrNull() ?: 0) > 0 }),
        THREADPOOL_SIZE(3, { (it?.toString()?.toIntOrNull() ?: 0) > 0 }),
        DEVELOPER_ID(0L, { if(it?.toString()?.isNotEmpty() == true) it.toString().toLongOrNull() != null else true}),
        DEEPAI_TOKEN(""),
        DEFAULT_GREETING("")
        ;

        fun get(): Any?
        {
            return json.getOrNull(name.toLowerCase())
        }

        fun getString(): String?
        {
            return get()?.toString()
        }

        fun getInt(): Int?
        {
            return get()?.toString()?.toIntOrNull()
        }

        fun getLong(): Long?
        {
            return get()?.toString()?.toLongOrNull()
        }

        fun getOrDefault(): Any
        {
            return get() ?: defaultValue
        }

        fun getStringOrDefault(): String
        {
            return getString() ?: defaultValue.toString()
        }

        fun getIntOrDefault(): Int
        {
            return getInt() ?: (defaultValue as Int)
        }

        fun getLongOrDefault(): Long
        {
            return getLong() ?: (defaultValue as Long)
        }
    }

}