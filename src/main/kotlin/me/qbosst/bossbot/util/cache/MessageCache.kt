package me.qbosst.bossbot.util.cache

import dev.kord.common.entity.Snowflake
import dev.kord.core.cache.data.AttachmentData
import dev.kord.core.cache.data.MessageData
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import kotlinx.coroutines.*
import me.qbosst.bossbot.util.ext.deleteAll
import me.qbosst.bossbot.util.ext.downloadToFile
import java.io.File

class MessageCache(
    maxSize: Int,
    private val logAttachment: (message: MessageData) -> Boolean
): AbMapLikeCollection<Snowflake, MessageData>(
    FixedCache(maxSize) { entry -> entry.value.files.deleteAll() }  // delete all attachments when removed to makes space for a new message
) {
    private val httpClient = HttpClient(CIO)

    override suspend fun put(key: Snowflake, value: MessageData) {
        super.put(key, value)

        // check if we should be logging attachments, if not return
        if(!logAttachment.invoke(value)) return

        // download attachments to file
        val jobs = coroutineScope {
            value.attachments.mapIndexed { index, att ->
                async {
                    val file = File(generateDirectory(value.id.value, index, att.fileExtension))
                    httpClient.downloadToFile(file, att.url)
                }
            }
        }

        // make sure all attachments have finished downloading
        jobs.awaitAll()
    }
}

private const val DIRECTORY = "./cached"

private fun generateDirectory(id: Long, index: Int, end: String?) = "${DIRECTORY}/${id}_${index}" +
        (end?.let { ".${end}" } ?: "")

private val AttachmentData.fileExtension: String?
    get() {
        val index = filename.lastIndexOf('.') + 1
        return if(index == -1 || index == filename.length) null else filename.substring(index)
    }

val MessageData.files: List<File>
    get() = attachments.mapIndexed { index, att -> File(generateDirectory(id.value, index, att.fileExtension)) }

