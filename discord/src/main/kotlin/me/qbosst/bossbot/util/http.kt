package me.qbosst.bossbot.util

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

suspend fun HttpClient.downloadToFile(file: File, url: String) {
    // create file if it does not exist
    if(!file.exists()) {
        withContext(Dispatchers.IO) {
            file.createNewFile()
        }
    }

    // check if we can write to the file
    require(file.canWrite()) { "Cannot write to file '${file.name}'"}

    val bytes = get<ByteReadChannel>(url)
    val bufferSize = 1024*100
    val buffer = ByteArray(bufferSize)

    // download the content at the url and write that content to the file
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