package me.qbosst.bossbot.entities

import me.qbosst.bossbot.util.FixedCache
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.sharding.ShardManager
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

class MessageCache(val size: Int)
{

    private val cache: MutableMap<Long, FixedCache<Long, CachedMessage>> = mutableMapOf()

    companion object
    {
        private val LOG = LoggerFactory.getLogger(MessageCache::class.java)
    }

    init
    {
        Files.createDirectories(Paths.get("./attachments"))
        for(file in File("./attachments").listFiles()) file.delete()

        LOG.info("Message Cache Initialized!")
    }

    fun putMessage(m: Message): CachedMessage?
    {
        if(!cache.containsKey(m.guild.idLong))
        {
            cache[m.guild.idLong] = FixedCache(size)
        }
        return cache[m.guild.idLong]!!.put(m.idLong, CachedMessage.create(m))
        { _, value ->
            value.deleteFiles()
        }
    }

    fun pullMessage(guild: Guild, messageId: Long): CachedMessage?
    {
        return cache[guild.idLong]?.pull(messageId)
    }

    fun getMessages(guild: Guild, predicate: (CachedMessage) -> Boolean): List<CachedMessage>
    {
        return cache[guild.idLong]?.values()?.filter{ predicate.invoke(it) } ?: listOf()
    }

    fun getMessage(guild: Guild, messageId: Long): CachedMessage?
    {
        return cache[guild.idLong]?.get(messageId)
    }

    data class CachedMessage private constructor(
            val content: String,
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
            fun create(m: Message): CachedMessage
            {
                return CachedMessage(
                        content = m.contentRaw,
                        username = m.author.name,
                        discriminator = m.author.discriminator,
                        messageIdLong = m.idLong,
                        authorIdLong = m.author.idLong,
                        channelIdLong = m.channel.idLong,
                        guildIdLong = m.guild.idLong,
                        attachments = m.attachments
                )
            }
        }

        init
        {
            if(attachments.isNotEmpty())
            {
                Files.createDirectories(Paths.get("./attachments"))
                for(attachment in attachments.withIndex())
                {
                    attachment.value
                            .downloadToFile(File(generateDirectory(messageIdLong, attachment.index, attachment.value.fileExtension)))
                            .thenAccept { LOG.debug("Downloaded attachment to ${it.absolutePath}") }
                            .exceptionally { LOG.error("Exception caught while trying to download attachment: $it"); return@exceptionally null }
                }
            }
        }

        fun getAuthor(shards: ShardManager): User?
        {
            return shards.getUserById(authorIdLong)
        }

        fun getTextChannel(guild: Guild): TextChannel?
        {
            return guild.getTextChannelById(channelIdLong)
        }

        fun getGuild(shards: ShardManager): Guild?
        {
            return shards.getGuildById(guildIdLong)
        }

        fun getAttachmentFiles(): Collection<File>
        {
            val files = mutableListOf<File>()
            for(attachment in attachments.withIndex())
            {
                files.add(File(generateDirectory(messageIdLong, attachment.index, attachment.value.fileExtension)))
            }
            return files
        }

        fun deleteFiles() {
            getAttachmentFiles().forEach {
                if(it.delete())
                    LOG.debug("Successfully deleted file ${it.absolutePath}")
                else
                    LOG.warn("Unable to delete file at ${it.absolutePath}. The file might still be open somewhere")}
        }

        override fun getIdLong(): Long
        {
            return messageIdLong
        }

        private fun generateDirectory(messageId: Long, count: Int, extension: String?): String
        {
            return "./attachments/${messageId}_${count}" + if(extension != null) ".${extension}" else ""
        }
    }
}