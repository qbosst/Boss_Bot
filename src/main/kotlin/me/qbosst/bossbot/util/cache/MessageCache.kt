package me.qbosst.bossbot.util.cache

import dev.kord.cache.map.MapLikeCollection
import dev.kord.common.entity.Snowflake
import dev.kord.core.cache.data.MessageData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import me.qbosst.bossbot.util.ext.downloadToFile
import me.qbosst.bossbot.util.ext.fileExtension
import mu.KotlinLogging
import java.io.File

class MessageCache(maxSize: Int): MapLikeCollection<Snowflake, MessageData> {

    init {
        // generates the directory for the cached attachments
        File(DIRECTORY).mkdirs()
    }


    private val cache = object: FixedCache<Snowflake, MessageData>(maxSize) {

        override fun onRemoveEldestEntry(entry: MutableMap.MutableEntry<Snowflake, MessageData>) {
            val message = entry.value

            // delete attachment files from storage as they are not longer needed
            message.deleteFiles()
        }
    }

    override suspend fun clear() = cache.clear()

    override suspend fun get(key: Snowflake): MessageData? = cache[key]

    override fun getByKey(predicate: suspend (Snowflake) -> Boolean): Flow<MessageData> = flow {
        for((key, value) in cache.entries.toList()) {
            if(predicate(key)) {
                emit(value)
            }
        }
    }

    override fun getByValue(predicate: suspend (MessageData) -> Boolean): Flow<MessageData> = flow {
        for((_, value) in cache.entries.toList()) {
            if(predicate(value)) {
                emit(value)
            }
        }
    }

    override suspend fun put(key: Snowflake, value: MessageData) {
        cache[key] = value

        val id = value.id.value

        // downloads message attachments to storage
        value.attachments.forEachIndexed { index, attachment ->
            val directory = generateDirectory(id, index, attachment.fileExtension)
            val file = File(directory)
            attachment.downloadToFile(file)
        }
    }

    override suspend fun remove(key: Snowflake) {
        val old = cache.remove(key)

        // delete message attachment files from storage
        old?.deleteFiles()
    }

    override fun values(): Flow<MessageData> = flow {
        cache.values.toList().forEach { emit(it) }
    }

    companion object {
        private val logger = KotlinLogging.logger("MessageCache")

        const val DIRECTORY = "./cached"

        private fun generateDirectory(id: Long, index: Int, extension: String?) =
            "${DIRECTORY}/${id}_${index}.${extension}"

        private val MessageData.files: List<File>
            get() = attachments.mapIndexed { index, attachment ->
                val directory = generateDirectory(id.value, index, attachment.fileExtension)
                File(directory)
            }

        private fun MessageData.deleteFiles(files: List<File> = this.files) {
            files.forEach { file ->
                if(!file.delete()) {
                    logger.warn { "Could not delete file '${file.absolutePath}'" }
                }
            }
        }
    }

}

