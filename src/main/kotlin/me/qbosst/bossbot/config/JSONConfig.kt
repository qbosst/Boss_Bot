package me.qbosst.bossbot.config

import me.qbosst.bossbot.util.extensions.toPrettyJson
import net.dv8tion.jda.api.utils.data.DataObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.file.Files

/**
 *  JSON Config
 *
 *  @param directory The directory to perform IO on the file
 *  @param default The default values of the config
 */
open class JSONConfig(directory: String, protected var default: DataObject)
{
    private val config = File(directory)
    protected val data: DataObject

    init
    {
        log.info("Config path: ${config.absolutePath}")
        // Create config if it doesn't exist
        data = if(!config.exists())
            if(config.createNewFile())
            {
                log.info("A config file has been generated at '${config.absolutePath}'")
                generateDefault()
                read()
            }
            else
                throw IOException("A config file could not be generated at '${config.absolutePath}'")

        else
            read()

        default.toMap().forEach { (key, value) ->
            if(!data.hasKey(key))
                data.put(key, value)
        }
        write()
    }

    /**
     *  Reads from the config file
     *
     *  @return data containing all the data read
     */
    protected fun read(): DataObject
    {
        val content = Files.readAllLines(config.toPath()).joinToString("")
        return DataObject.fromJson(content)
    }

    /**
     *  Writes to the config file
     *
     *  @param data The data to write to the config. Default is the config that was originally read
     */
    protected fun write(data: DataObject = this.data)
    {
        Files.write(config.toPath(), data.toPrettyJson())
    }

    protected fun generateDefault()
    {
        write(default)
    }

    /**
     *  Gets a value from the key in the config
     *
     *  @param key The key for the value
     *
     *  @return Value from the config. Null if the config does not contain the key.
     */
    fun get(key: String): Any? = if(data.hasKey(key)) data.get(key) else null

    companion object
    {
        val log: Logger = LoggerFactory.getLogger(this::class.java)
    }
}