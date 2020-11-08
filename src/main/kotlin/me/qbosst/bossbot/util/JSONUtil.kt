package me.qbosst.bossbot.util

import net.dv8tion.jda.api.utils.data.DataObject
import org.json.JSONException
import org.json.JSONObject

object JSONUtil
{
    private const val BROAD_NUM = 32
    private const val CLOSE_NUM = 4

    fun parseJson(content: ByteArray, success: (DataObject) -> Unit, error: (Exception, String) -> Unit)
    {
        val contentAsString = content.decodeToString()
        try
        {
            success.invoke(JSONObject(contentAsString).toDataObject())
        }
        catch (e: JSONException)
        {
            val info = kotlin.run {
                val numbers = e.localizedMessage
                    .split(Regex("\\s+"))
                    .map { it.replace(Regex("\\D+"), "") }
                    .mapNotNull { it.toIntOrNull() }
                JSONErrorInfo(numbers[0], numbers[1], numbers[2])
            }
            val line = contentAsString.split(Regex("\n"))[info.atLine-1]
            val sb = StringBuilder("```asciidoc")
                .append("\n=== Failed to parse JSON: ${e.localizedMessage} ===")
                .append("\nAt line ${info.atLine}: ")
                .append("\n[ -> ${line.trim().maxLength(128)} <- ]")
                .append("\nAt around...")
                .append("\n[ -> ${line.substring(
                        (info.atCharacterInLine-BROAD_NUM).coerceIn(0, line.length),
                        (info.atCharacterInLine+BROAD_NUM).coerceIn(0, line.length)
                ).trim()} <-]")
                .append("\nAt around...")
                .append("\n[ -> ${line.substring(
                        (info.atCharacterInLine-CLOSE_NUM).coerceIn(0, line.length),
                        (info.atCharacterInLine+ CLOSE_NUM).coerceIn(0, line.length)
                ).trim()} <-]")
                .append("\nAt character ${info.atCharacter}:")
                .append("\n[ -> ${contentAsString.substring(info.atCharacter-1, info.atCharacter)} <- ]")
                .append("```")
                .toString()
            error.invoke(e, sb)
        }
    }

    private data class JSONErrorInfo(
        val atCharacter: Int,
        val atCharacterInLine: Int,
        val atLine: Int
    )
}

/**
 *  Gets a value based on the key from a JSON Object
 *
 *  @param key The key to get the value from
 *
 *  @return The value from the key. Null if the key does not have a value
 */
fun JSONObject.getOrNull(key: String): Any? = if(has(key)) get(key) else null

/**
 *  Converts a data object into a json object
 *
 *  @return JSON Object representing a data object
 */
fun DataObject.toJSONObject(): JSONObject = JSONObject(toJson().decodeToString())

/**
 *  Converts a Data Object straight to a JSON representation
 *
 *  @param indentFactor The indent of the JSON
 *
 *  @return Byte Array representing a JSON file in text
 */
fun DataObject.toJson(indentFactor: Int): ByteArray = toJSONObject().toString(indentFactor).toByteArray()

/**
 *  Converts a JSONObject to a Data Object
 *
 *  @return Data object representation of the JSON object
 */
fun JSONObject.toDataObject(): DataObject = DataObject.fromJson(this.toString())
