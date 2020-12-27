package me.qbosst.bossbot.entities

import me.qbosst.bossbot.util.FixedCache
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.sharding.ShardManager
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

/**
 *  This class is used to manage cached messages.
 *  The idea of the class is from the original author listed below but i've made some changes so not everything is made
 *  by him.
 *
 *  @param size The size of the maximum amount of cached messages allowed per guild
 *
 *  @author John Grosh (john.a.grosh@gmail.com)
 */
class MessageCache(val size: Int)
{
    private val cache: MutableMap<Long, FixedCache<Long, CachedMessage>> = mutableMapOf()

    init
    {
        val directory = File(DIRECTORY)

        // Delete the directory and all files inside
        if(!directory.deleteRecursively())
            LOG.warn("Could not delete directory {}", directory.absolutePath)

        // Recreate the directory
        directory.mkdirs()

        LOG.info("Message Cache Initialized!")
    }

    /**
     *  Caches a message
     *
     *  @param m The message to cache
     *
     *  @return The old cached message. Null if there was no message cached previously with the same ID
     */
    fun putMessage(m: Message): CachedMessage?
    {
        val guildCache = cache.computeIfAbsent(m.guild.idLong) { FixedCache(size) }

        return guildCache.put(m.idLong, CachedMessage(m)) { _, value -> value.deleteFiles() }
    }

    /**
     *  Removes a message from cache
     *
     *  @param guild The guild to remove the cached message from
     *  @param messageId The id of the cached message to remove
     *
     *  @return CachedMessage. Null if the message was not cached
     */
    fun pullMessage(guild: Guild, messageId: Long): CachedMessage? = cache[guild.idLong]?.pull(messageId)

    /**
     *  Gets a list of cached messages based on a condition specified
     *
     *  @param guild The guild to get the list of messages from
     *  @param predicate The condition needed to get the cached message
     *
     *  @return List of cached messages that met the condition
     */
    fun getMessages(guild: Guild, predicate: (CachedMessage) -> Boolean): List<CachedMessage> =
            cache[guild.idLong]?.values?.filter { message -> predicate.invoke(message) } ?: listOf()

    /**
     *  Returns a cached message
     *
     *  @param guild The guild to get the cached message from
     *  @param messageId The id of the cached message to get
     *
     *  @return CachedMessage. Null if the message was not cached
     */
    fun getMessage(guild: Guild, messageId: Long): CachedMessage? = cache[guild.idLong]?.get(messageId)

    /**
     *  Represents a Message object.
     *
     *  @param contentRaw The content of the message
     *  @param messageIdLong The ID of the message
     *  @param channelIdLong The ID of the channel that the message was sent in
     *  @param guildIdLong The ID of the guild that the message was sent in
     *  @param attachments The attachments that the cached message came with
     */
    data class CachedMessage private constructor(val content: String,
                                                 val author: CachedUser,
                                                 val messageIdLong: Long,
                                                 val channelIdLong: Long,
                                                 val guildIdLong: Long,
                                                 val attachments: Collection<Message.Attachment>): ISnowflake
    {
        constructor(m: Message): this(m.contentRaw, CachedUser(m.author), m.idLong, m.channel.idLong, m.guild.idLong,
                m.attachments)

        val files: List<File>
            get() = attachments.mapIndexed { index, attachment -> File(generateDirectory(attachment, index)) }

        init
        {
            // downloads attachments
            attachments.withIndex().forEach { (index, attachment) ->
                attachment
                        .downloadToFile(File(generateDirectory(attachment, index)))
                        .whenComplete { file, throwable ->
                            if(throwable != null)
                                LOG.error("Could not download attachment", throwable)
                            else
                                LOG.debug("Downloaded attachment to `{}`", file.absolutePath)
                        }
            }
        }

        /**
         *  Deletes the downloaded attachments from secondary storage
         */
        fun deleteFiles(files: Collection<File> = this.files) = files.forEach { file ->
            if(!file.delete())
                LOG.warn("Unable to delete file at '{}'. The file might still be open somewhere", file.absolutePath)
        }

        override fun getIdLong(): Long = messageIdLong

        private fun generateDirectory(attachment: Message.Attachment, count: Int): String =
                "${DIRECTORY}/${messageIdLong}_${count}" +
                        (attachment.fileExtension?.let { append-> ".${append}" } ?: "N/A")

        data class CachedUser private constructor(val username: String, val discriminator: String, val idLong: Long)
        {
            constructor(user: User): this(user.name, user.discriminator, user.idLong)
        }
    }

    companion object
    {
        private val LOG = LoggerFactory.getLogger(MessageCache::class.java)
        private const val DIRECTORY = "./cached"
    }
}