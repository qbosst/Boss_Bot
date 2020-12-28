package me.qbosst.bossbot.util.extensions

import me.qbosst.bossbot.util.maxLength
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.exceptions.ParsingException
import net.dv8tion.jda.api.utils.data.DataArray
import net.dv8tion.jda.api.utils.data.DataObject
import java.io.ByteArrayOutputStream
import java.io.OutputStream

object JSONUtil
{
    fun parseJson(content: ByteArray, success: (DataObject) -> Unit, error: (Throwable, String) -> Unit)
    {
        val contentAsString = content.decodeToString()
        try
        {
            success.invoke(DataObject.fromJson(contentAsString))
        }
        catch (e: ParsingException)
        {
            val cause = e.cause ?: e
            error.invoke(cause, "```${cause.localizedMessage}```".maxLength(Message.MAX_CONTENT_LENGTH, "...```"))
        }
    }
}

fun DataObject.toPrettyJson(): ByteArray = toPrettyPrint(this)

fun DataArray.toPrettyJson(): ByteArray = toPrettyPrint(this)

private fun toPrettyPrint(json: Any): ByteArray
{
    val mapperField = json::class.java.getDeclaredField("mapper")
        .apply { isAccessible = true }
    val writerMethod = mapperField.get(json)::class.java.getDeclaredMethod("writerWithDefaultPrettyPrinter")
        .apply { isAccessible = true }
    val dataField = json::class.java.getDeclaredField("data")
        .apply { isAccessible = true }

    val outputStream = ByteArrayOutputStream()

    // get writer object
    val writer = writerMethod.invoke(mapperField.get(json))

    // write the data to the output stream
    writer::class.java
        .getDeclaredMethod("writeValue", OutputStream::class.java, Any::class.java)
        .apply { isAccessible = true }
        .also { method -> method.invoke(writer, outputStream, dataField.get(json)) }
        .apply { isAccessible = false }

    return outputStream.toByteArray()
        .also {
            mapperField.isAccessible = false
            writerMethod.isAccessible = false
            dataField.isAccessible = false
        }
}
