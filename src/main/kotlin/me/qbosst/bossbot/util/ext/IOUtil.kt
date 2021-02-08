package me.qbosst.bossbot.util.ext

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.qbosst.bossbot.util.cache.MessageCache
import mu.KotlinLogging
import java.io.File
import java.io.FileOutputStream

suspend fun HttpClient.downloadToFile(file: File, url: String) {
    if(!file.exists()) {
        withContext(Dispatchers.IO) {
            file.createNewFile()
        }
    }
    require(file.canWrite()) { "Cannot write to file ${file.name}" }

    val bytes = this.get<ByteReadChannel>(url)
    val bufferSize = 1024 * 100
    val buffer = ByteArray(bufferSize)

    withContext(Dispatchers.IO) {
        FileOutputStream(file).use {
            do {
                val currentRead = bytes.readAvailable(buffer)

                // channel has closed
                if(currentRead == -1) {
                    break
                }

                it.write(buffer, 0, currentRead)
            } while (currentRead >= 0)
        }
    }
}

fun Collection<File>.deleteAll() {
    val logger by lazy { KotlinLogging.logger("me.qbosst.bossbot.util.ext.deleteAll") }

    forEach { file ->
        if(!file.delete()) {
            logger.warn { "Could not delete file at '${file.absolutePath}'" }
        }
    }
}