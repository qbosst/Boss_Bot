package me.qbosst.bossbot.util.ext

import dev.kord.core.cache.data.AttachmentData
import dev.kord.core.entity.Attachment
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.utils.io.*
import java.io.File
import java.io.FileOutputStream

private val client = HttpClient(CIO)

suspend fun AttachmentData.downloadToFile(file: File) = client.downloadToFile(file, this.url)

suspend fun Attachment.downloadToFile(file: File) = client.downloadToFile(file, this.url)

val AttachmentData.fileExtension: String?
    get() {
        val index = filename.lastIndexOf('.')+1
        return if(index == -1 || index == filename.length) null else filename.substring(index)
    }

val Attachment.fileExtension: String? get() = data.fileExtension

suspend fun HttpClient.downloadToFile(file: File, url: String) {
    if(!file.exists()) {
        file.createNewFile()
    }
    require(file.canWrite()) { "Cannot write to file ${file.name}" }

    val bytes = this.get<ByteReadChannel>(url)
    val bufferSize = 1024 * 100
    val buffer = ByteArray(bufferSize)

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