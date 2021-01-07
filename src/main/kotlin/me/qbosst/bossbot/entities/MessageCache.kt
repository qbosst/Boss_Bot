package me.qbosst.bossbot.entities

import me.qbosst.jda.ext.util.FixedCache
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import org.slf4j.LoggerFactory
import java.io.File

private const val DIRECTORY = "./cached"
private val LOG = LoggerFactory.getLogger(MessageCache::class.java)

class MessageCache(val size: Int) {

    private val cache: MutableMap<Long, FixedCache<Long, CachedMessage>> = mutableMapOf()

    fun put(message: Message): CachedMessage? {
        val guildCache = cache.computeIfAbsent(message.guild.idLong) { FixedCache(size) }

        return guildCache.put(message.idLong, CachedMessage(message)) { _, value ->
            // delete old files from storage, since it wont be retrieved anymore
            value.deleteFiles()
        }
    }

    fun pull(guild: Guild, messageId: Long): CachedMessage? = cache[guild.idLong]?.pull(messageId)

    operator fun get(guild: Guild, filter: (CachedMessage) -> Boolean): List<CachedMessage> =
        cache[guild.idLong]?.values?.filter(filter) ?: listOf()

    operator fun get(guild: Guild, messageId: Long): CachedMessage? = cache[guild.idLong]?.get(messageId)

    /**
     * Caches a [Message]
     */
    operator fun plus(message: Message): CachedMessage? = put(message)

    /**
     * Removes all the cached messages of a [Guild]
     */
    operator fun minus(guild: Guild): FixedCache<Long, CachedMessage>? = cache.remove(guild.idLong)

    /**
     * Checks if a [Guild]'s [Message]s is being cached
     *
     * @param guild the [Guild] to check
     */
    operator fun contains(guild: Guild): Boolean = cache.containsKey(guild.idLong)

    companion object {

        // when the first instance of MessageCache is created, run this
        init {
            File(DIRECTORY).apply {
                // delete all the current files in this directory, including the directory itself
                deleteRecursively()

                // re-create the directory
                mkdirs()
            }
            LOG.info("'$DIRECTORY' has been cleared")
        }
    }

}

/**
 * A stripped down version of a [Message] object.
 */
data class CachedMessage private constructor(
    val content: String,
    val user: CachedUser,
    val messageIdLong: Long,
    val channelIdLong: Long,
    val guildIdLong: Long,
    val attachments: Collection<Message.Attachment>,
    val embeds: Collection<MessageEmbed>
) {
    constructor(message: Message): this(
        message.contentRaw,
        CachedUser(message.author),
        message.idLong,
        message.channel.idLong,
        if(message.isFromGuild) message.guild.idLong else 0L,
        message.attachments,
        message.embeds
    )

    val files: List<File>
        get() = attachments.mapIndexed { index, attachment -> File(getDirectory(attachment, index)) }

    init {
        // download attachments to storage, too big to keep in memory
        attachments.forEachIndexed { index, attachment ->
            val directory = getDirectory(attachment, index)
            attachment
                .downloadToFile(directory)
                .whenComplete { _, throwable ->
                    throwable?.let { error -> LOG.error("Could not download attachment to '$directory'", error) }
                }
        }
    }

    /**
     * Deletes the files from storage
     */
    fun deleteFiles(files: List<File> = this.files) = files.forEach { file ->
        if(!file.delete())
            LOG.warn("Unable to delete file at '{}'. Maybe the file is open somewhere?", file.absolutePath)
    }

    private fun getDirectory(attachment: Message.Attachment, index: Int) = buildString {
        append("${DIRECTORY}/${messageIdLong}_${index}")

        // append file extension if not null
        attachment.fileExtension?.let { ext -> append(".${ext}") }
    }
}

data class CachedUser private constructor(val username: String, val discriminator: String, val idLong: Long) {
    constructor(user: User): this(user.name, user.discriminator, user.idLong)
}