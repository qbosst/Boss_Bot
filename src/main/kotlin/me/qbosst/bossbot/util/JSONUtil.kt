package me.qbosst.bossbot.util

import net.dv8tion.jda.api.utils.data.DataObject
import org.json.JSONObject

fun JSONObject.getOrNull(key: String): Any?
{
    return if(has(key)) get(key) else null
}

fun DataObject.toJSONObject(): JSONObject = JSONObject(toJson().decodeToString())

fun DataObject.toJson(indentFactor: Int): ByteArray = toJSONObject().toString(indentFactor).toByteArray()