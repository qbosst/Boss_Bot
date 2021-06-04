package me.qbosst.bossbot.util

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaZoneId
import java.util.TimeZone as JTimeZone

fun TimeZone.toJavaTimeZone(): JTimeZone = JTimeZone.getTimeZone(toJavaZoneId())

fun TimeZone.Companion.findZones(query: String): List<TimeZone> {
    val connector = "[-_\\s/]?"
    val separator = "[^/]+".toPattern()
    val replaceMatching = "[_-]".toRegex()
    val replaceWith = "[-_\\\\s]?"

    val queryLower = query.lowercase()

    return availableZoneIds.asSequence()
        .sorted()
        .map { id ->
            val nameRegex = buildString {
                append('(')

                val matcher = separator.matcher(id)
                while(matcher.find()) {
                    val regex = matcher.group().replace(replaceMatching, replaceWith)
                    append("(${regex})?${connector}")
                }
                deleteRange(lastIndex-connector.length, lastIndex)
                append(')')
            }

            val zoneId = of(id)
            val abbreviationRegex = buildString {
                append('(')
                val timeZone = zoneId.toJavaTimeZone()
                append("${timeZone.getDisplayName(true, 0)}|${timeZone.getDisplayName(false, 0)}")
                append(')')
            }

            zoneId to "($nameRegex)|$abbreviationRegex)".lowercase().toRegex()
        }
        .filter { (_, regex) ->
            queryLower.matches(regex)
        }
        .map { (key, _) ->
            key
        }
        .toList()
}