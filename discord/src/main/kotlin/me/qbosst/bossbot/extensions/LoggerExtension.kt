package me.qbosst.bossbot.extensions

import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.guildFor
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.*
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.cache.data.AttachmentData
import dev.kord.core.cache.data.MessageData
import dev.kord.core.entity.Attachment
import dev.kord.core.event.Event
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.MessageDeleteEvent
import dev.kord.core.event.message.MessageUpdateEvent
import dev.kord.rest.builder.message.EmbedBuilder
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import me.qbosst.bossbot.database.dao.getGuildDAO
import me.qbosst.bossbot.util.cache.AbstractMapLikeCollection
import me.qbosst.bossbot.util.cache.FixedCache
import me.qbosst.bossbot.util.idLong
import me.qbosst.bossbot.util.isNotBot
import me.qbosst.bossbot.util.zeroWidthIfBlank
import mu.KLogger
import mu.KotlinLogging
import java.io.File

private const val DIRECTORY = "./cached"

private val MessageData.files: List<File>
    get() = attachments.mapIndexed { index, attachment -> File(genDir(id.value, index, attachment.fileExtension)) }

private val AttachmentData.fileExtension: String?
    get() {
        val index = filename.lastIndexOf('.') + 1
        return if(index == -1 || index == filename.length) null else filename.substring(index)
    }

private val Attachment.fileExtension: String?
    get() = data.fileExtension

private fun genDir(id: Long, index: Int, extension: String?) =
    "$DIRECTORY/${id}_$index" + (extension?.let { ".$extension" } ?: "")

private fun Collection<File>.deleteAll() {
    val logger: KLogger = KotlinLogging.logger("me.qbosst.bossbot.extensions.deleteAll")

    forEach { file ->
        if(!file.delete()) {
            logger.warn { "Could not delete file at '${file.absolutePath}'" }
        }
    }
}

private suspend fun Event.getLogChannel(): MessageChannelBehavior? {
    val guild = guildFor(this) ?: return null
    val settings = guild.getGuildDAO()

    return when(this) {
        is MessageUpdateEvent, is MessageDeleteEvent ->
            settings.messageLogChannel?.let { id -> guild.getChannelOfOrNull(Snowflake(id)) }
        else ->
            null
    }
}

class MessageCache(maxSize: Int): AbstractMapLikeCollection<Snowflake, MessageData>(
    FixedCache(maxSize) { (_, message) ->
        message.files.deleteAll()
    }
)

class LoggerExtension: Extension() {
    override val name: String = "logger"

    override suspend fun setup() {
        // create directory to store attachments
        withContext(Dispatchers.IO) {
            File(DIRECTORY).mkdir()
        }

        event<MessageCreateEvent> {
            check(::anyGuild, ::isNotBot)

            action {
                val message = event.message

                val jobs = coroutineScope {
                    message.attachments.mapIndexed { index, attachment ->
                        async {
                            val file = File(genDir(message.idLong, index, attachment.fileExtension))
                            attachment.downloadToFile(file)
                        }
                    }
                }

                jobs.awaitAll()
            }
        }

        event<MessageUpdateEvent> {
            check(::anyGuild, ::isNotBot)

            action {
                val logChannel = event.getLogChannel() ?: return@action

                val newMessage = event.message.asMessage()
                val oldMessage = event.old
                val author = newMessage.author!!

                logChannel.createEmbed {
                    author {
                        name = "Message Edited"
                        icon = author.avatar.url
                    }

                    field("Channel", true) { event.channel.mention }
                    field("Author", true) { author.mention }
                    field("Message", true) { "[Jump to Message](${newMessage.getJumpUrl()})"}

                    val limit = EmbedBuilder.Field.Limits.value
                    field("Before", true) { oldMessage?.content?.take(limit)?.zeroWidthIfBlank() ?: "N/A" }
                    field("After", true) { newMessage.content.take(limit).zeroWidthIfBlank() }

                    footer {
                        text = "Message ID: ${newMessage.idLong} | User ID: ${author.idLong}"
                    }

                    // TODO: add colour
                    timestamp = Clock.System.now()
                }
            }
        }

        event<MessageDeleteEvent> {
            check(::anyGuild, ::isNotBot)

            action {
                val message = event.message
                val files = message?.data?.files

                run {
                    val logChannel = event.getLogChannel() ?: return@run
                    val fileStreams = files?.map { file -> file.name to file.inputStream() }
                    val author = message?.author

                    logChannel.createMessage {
                        embed {
                            author {
                                name = "Message Deleted"
                                icon = message?.author?.avatar?.url
                            }

                            field("Channel", true) { event.channel.mention }
                            field("Author", true) { author?.let { "${it.tag} ${it.mention}" } ?: "N/A" }

                            if(!fileStreams.isNullOrEmpty()) {
                                field("Attachments", true) { fileStreams.size.toString() }
                            }

                            val content = message?.content?.take(EmbedBuilder.Field.Limits.value)
                            if(content == null || content.isNotEmpty()) {
                                field("Content", true) { content ?: "N/A" }
                            }

                            footer {
                                text = buildString {
                                    append("Message ID: ${event.messageId.value}")

                                    if(author != null) {
                                        append(" | User ID: ${author.idLong}")
                                    }
                                }
                            }

                            // TODO: add colour
                            timestamp = Clock.System.now()
                        }

                        fileStreams?.forEach { (fileName, stream) ->
                            addFile(fileName, stream)
                        }
                    }

                    fileStreams?.forEach { (_, stream) -> stream.close() }
                }

                withContext(Dispatchers.IO) {
                    files?.deleteAll()
                }
            }
        }
    }

    override suspend fun unload() {
        // delete directory that stores attachments
        withContext(Dispatchers.IO) {
            File(DIRECTORY).deleteRecursively()
        }
    }
}