package me.qbosst.bossbot.entities

import me.qbosst.bossbot.util.FixedCache
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
        // Delete the folder and all files inside
        FileUtils.deleteDirectory(File("./attachments"))

        // Re-create the folder
        Files.createDirectories(Paths.get("./attachments"))

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
        if(!cache.containsKey(m.guild.idLong))
            cache[m.guild.idLong] = FixedCache(size)

        return cache[m.guild.idLong]!!.put(m.idLong, CachedMessage.create(m))
        { _, value ->
            value.deleteFiles()
        }
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
    fun getMessages(guild: Guild, predicate: (CachedMessage) -> Boolean): List<CachedMessage> = cache[guild.idLong]?.values?.filter{ predicate.invoke(it) } ?: listOf()

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
     *  This class is used to cache a message
     *
     *  @param contentRaw The content of the message
     *  @param username The username of the author of the message
     *  @param discriminator The discriminator (tag) of the author of the message
     *  @param messageIdLong The ID of the message
     *  @param authorIdLong The ID of the author of the message
     *  @param channelIdLong The ID of the channel that the message was sent in
     *  @param guildIdLong The ID of the guild that the message was sent in
     *  @param attachments The attachments that the cached message came with
     */
    data class CachedMessage private constructor(
            val contentRaw: String,
            val username: String,
            val discriminator: String,
            private val messageIdLong: Long,
            val authorIdLong: Long,
            private val channelIdLong: Long,
            private val guildIdLong: Long,
            val attachments: Collection<Message.Attachment>
    ): ISnowflake
    {

        companion object
        {
            /**
             * Creates a cached message object
             *
             * @param m The message to cache
             *
             * @return CachedMessage object
             */
            fun create(m: Message): CachedMessage
            {
                return CachedMessage(
                        contentRaw = m.contentRaw,
                        username = m.author.name,
                        discriminator = m.author.discriminator,
                        messageIdLong = m.idLong,
                        authorIdLong = m.author.idLong,
                        channelIdLong = m.channel.idLong,
                        guildIdLong = if(m.isFromGuild) m.guild.idLong else 0L,
                        attachments = m.attachments
                )
            }
        }

        init
        {
            // If the message has attachments
            if(attachments.isNotEmpty())
            {
                // Downloads them to secondary storage
                Files.createDirectories(Paths.get("./attachments"))
                for(attachment in attachments.withIndex())
                    attachment.value
                            .downloadToFile(File(generateDirectory(messageIdLong, attachment.index, attachment.value.fileExtension)))
                            .thenAccept { LOG.debug("Downloaded attachment to ${it.absolutePath}") }
                            .exceptionally { LOG.error("Exception caught while trying to download attachment: $it"); return@exceptionally null }
            }
        }

        /**
         *  Gets the author of the message.
         *
         *  @param shards Object needed to get the user from
         *
         *  @return Author of the cached message. Null if the self-user does not have a copy of the user's id.
         */
        fun getAuthor(shards: ShardManager): User? = shards.getUserById(authorIdLong)

        /**
         *  Gets the text channel that the message was sent in
         *
         *  @param guild The guild to get the text channel from
         *
         *  @return Text Channel of the cached message. Null if the message did not come from a guild or the channel no longer exists
         */
        fun getTextChannel(guild: Guild): TextChannel? = guild.getTextChannelById(channelIdLong)

        /**
         *  Gets the guild that the message was sent in
         *
         *  @param shards Object needed to get the guild from
         *
         *  @return Guild that the message came from. Null if the message did not come from a guild
         */
        fun getGuild(shards: ShardManager): Guild? = if(guildIdLong == 0L) null else shards.getGuildById(guildIdLong)

        /**
         *  Retrieves the downloaded message attachments from secondary storage
         *
         *  @return Collection of attachments in files
         */
        fun getAttachmentFiles(): Collection<File>
        {
            val files = mutableListOf<File>()
            for(attachment in attachments.withIndex())
                files.add(File(generateDirectory(messageIdLong, attachment.index, attachment.value.fileExtension)))

            return files
        }

        /**
         *  Deletes the downloaded attachments from secondary storage
         */
        fun deleteFiles()
        {
            for(file in getAttachmentFiles())
                if(file.delete())
                    LOG.debug("Successfully deleted file ${file.absolutePath}")
                else
                    LOG.warn("Unable to delete file at ${file.absolutePath}. The file might still be open somewhere")
        }

        override fun getIdLong(): Long = messageIdLong

        /**
         *  Generates the name of the attachment file
         */
        private fun generateDirectory(messageId: Long, count: Int, extension: String?): String
        {
            return "./attachments/${messageId}_${count}" + if(extension != null) ".${extension}" else ""
        }
    }

    companion object
    {
        private val LOG = LoggerFactory.getLogger(MessageCache::class.java)
    }
}