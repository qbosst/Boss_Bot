package qbosst.bossbot.util

import org.json.JSONObject

fun JSONObject.getOrNull(key: String): Any?
{
    return if(has(key)) get(key) else null
}