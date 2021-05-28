package me.qbosst.bossbot.extensions

import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.authorIsBot
import dev.kord.common.entity.Snowflake
import dev.kord.core.cache.data.AttachmentData
import dev.kord.core.cache.data.MessageData
import dev.kord.core.event.message.MessageDeleteEvent
import dev.kord.core.event.message.MessageUpdateEvent
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import me.qbosst.bossbot.util.cache.AbstractMapLikeCollection
import me.qbosst.bossbot.util.cache.FixedCache
import me.qbosst.bossbot.util.downloadToFile
import me.qbosst.bossbot.util.isNotBot
import mu.KLogger
import mu.KotlinLogging
import java.io.File

private const val DIRECTORY = "./cached"

private val MessageData.files: List<File>
    get() = attachments.mapIndexed { index, attachment -> File(genDir(id.value, index, attachment.fileExtension)) }

private val httpClient = HttpClient(CIO)

private val AttachmentData.fileExtension: String?
    get() {
        val index = filename.lastIndexOf('.') + 1
        return if(index == -1 || index == filename.length) null else filename.substring(index)
    }

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

class MessageCache(maxSize: Int): AbstractMapLikeCollection<Snowflake, MessageData>(
    FixedCache(maxSize) { (_, message) ->
        message.files.deleteAll()
    }
) {
    override suspend fun put(key: Snowflake, message: MessageData) {
        super.put(key, message)

        if(message.authorIsBot) {
            return
        }

        val jobs = coroutineScope {
            message.attachments.mapIndexed { index, attachment ->
                async {
                    val file = File(genDir(message.id.value, index, attachment.fileExtension))
                    httpClient.downloadToFile(file, attachment.url)
                }
            }
        }

        // make sure all attachments have finished downloading
        jobs.awaitAll()
    }
}

class LoggerExtension: Extension() {
    override val name: String = "logger"

    override suspend fun setup() {
        event<MessageUpdateEvent> {
            check(::anyGuild, ::isNotBot)

            action {}
        }

        event<MessageDeleteEvent> {
            check(::anyGuild, ::isNotBot)

            action {}
        }
    }
}