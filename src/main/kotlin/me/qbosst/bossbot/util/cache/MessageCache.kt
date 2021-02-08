package me.qbosst.bossbot.util.cache

import dev.kord.common.entity.Snowflake
import dev.kord.core.cache.data.AttachmentData
import dev.kord.core.cache.data.MessageData
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import me.qbosst.bossbot.util.ext.deleteAll
import me.qbosst.bossbot.util.ext.downloadToFile
import java.io.File

class MessageCache(
    maxSize: Int,
    private val logAttachment: (message: MessageData) -> Boolean
): MapLikeCollection<Snowflake, MessageData>(FixedCache(maxSize) { entry ->
    // delete message attachments from storage
    entry.value.files.deleteAll()
}) {
    private val httpClient = HttpClient(CIO)

    override suspend fun put(key: Snowflake, value: MessageData) {
        super.put(key, value)

        // check if we should be logging attachments
        if(!logAttachment.invoke(value)) return

        // download attachments
        val idLong = value.id.value
        value.attachments.forEachIndexed { index, attachment ->
            val file = File(generateDirectory(idLong, index, attachment.fileExtension))
            httpClient.downloadToFile(file, attachment.url)
        }
    }

    companion object {
        private const val DIRECTORY = "./cached"

        init {
            // delete previous attachments
            File(DIRECTORY).deleteRecursively()

            // create directory
            File(DIRECTORY).mkdir()
        }

        private fun generateDirectory(id: Long, index: Int, end: String?) = "${DIRECTORY}/${id}_${index}.${end}"

        private val AttachmentData.fileExtension: String?
            get() {
                val index = filename.lastIndexOf('.')+1
                return if(index == -1 || index == filename.length) null else filename.substring(index)
            }

        val MessageData.files: List<File>
            get() {
                val idLong = id.value
                return attachments.mapIndexed { i, attachment -> File(generateDirectory(idLong, i, attachment.fileExtension)) }
            }
    }
}

