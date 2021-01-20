package me.qbosst.bossbot.util

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object TimeUtil {

    val DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy")

    @OptIn(ExperimentalStdlibApi::class)
    private val zoneMatcher: Map<ZoneId, Regex> = buildMap {

        val connector = "[-_\\s/]?"
        val separator = Pattern.compile("[^/]+")
        val replaceMatching = "[_-]".toRegex()
        val replaceWith = "[-_\\\\s]?"
        ZoneId.getAvailableZoneIds().sorted().forEach { id ->
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

            val zoneId = ZoneId.of(id)
            val abbreviationRegex = buildString {
                append('(')
                val timeZone = TimeZone.getTimeZone(zoneId)
                append("${timeZone.getDisplayName(true, 0)}|${timeZone.getDisplayName(false, 0)}")
                append(')')
            }

            put(zoneId, "(${nameRegex}|${abbreviationRegex})".toLowerCase().toRegex())
        }
    }

    private val timeRegex = buildString {
        val units = enumValues<TimeUnit>().joinToString("|") { unit -> unit.regex }
        append("(?is)^((\\s*-?\\s*\\d+\\s*(${units})\\s*,?\\s*(and)?)*).*")
    }.toRegex()

    private val TimeUnit.value
        get() = TimeUnit.NANOSECONDS.convert(1, this)

    val TimeUnit.abbreviation
        get() = when(this) {
            TimeUnit.NANOSECONDS -> "ns"
            TimeUnit.MICROSECONDS -> "μs"
            TimeUnit.MILLISECONDS -> "ms"
            TimeUnit.SECONDS -> "s"
            TimeUnit.MINUTES -> "m"
            TimeUnit.HOURS -> "h"
            TimeUnit.DAYS -> "d"
        }

    val TimeUnit.regex
        get() = when(this) {
            TimeUnit.NANOSECONDS -> "n(ano)?s(ec(ond)?s?)?"
            TimeUnit.MICROSECONDS -> "μ(icro)?s(ec(ond)?s?)?"
            TimeUnit.MILLISECONDS -> "m(illi)?s(ec(ond)?s?)?"
            TimeUnit.SECONDS -> "s(ec(ond)?s?)?"
            TimeUnit.MINUTES -> "m(in(ute)?s?)?"
            TimeUnit.HOURS -> "h(((ou)?)rs?)?"
            TimeUnit.DAYS -> "d(ays?)?"
        }

    fun zoneIdOf(id: String): ZoneId? =
        ZoneId.getAvailableZoneIds()
            .firstOrNull { it.equals(id, true) }
            ?.let { ZoneId.of(it) }

    fun filterZones(query: String): List<ZoneId> {
        val lower = query.toLowerCase()

        return zoneMatcher.asSequence()
            .filter { (_, regex) -> lower.matches(regex) }
            .map { (key, _) -> key }
            .toList()
    }
    fun timeToString(
        time: Long,
        unit: TimeUnit = TimeUnit.SECONDS,
        locale: (TimeUnit, Long) -> String = { _unit, _time -> "${_time}${_unit.abbreviation}" },
        delimiter: String = " "
    ): String {
        if(time == 0L) {
            return locale.invoke(unit, 0)
        }

        val isNegative = time < 0
        var timeLeft = if(isNegative) -time else time

        return buildString {
            val range = (unit.ordinal until enumValues<TimeUnit>().count()).reversed()
            for(index in range) {
                val tempUnit = enumValues<TimeUnit>().first { it.ordinal == index }
                val tempValue = tempUnit.value / unit.value

                val result = timeLeft / tempValue
                if(result > 0) {
                    append(locale.invoke(tempUnit, result))
                    append(delimiter)
                    timeLeft -= result*tempValue
                }
            }

            deleteRange(this.length-delimiter.length, delimiter.length)
        }
    }

    fun timeToString(
        time: Int,
        unit: TimeUnit = TimeUnit.SECONDS,
        locale: (TimeUnit, Long) -> String = { _unit, _time -> "${_time}${_unit.abbreviation}" },
        delimiter: String = " "
    ) = timeToString(time.toLong(), unit, locale, delimiter)

    fun parseTime(time: String, outputUnit: TimeUnit = TimeUnit.SECONDS): Long? {
        var timeStr = time.replace(timeRegex, "$1")
        var count: Long = 0
        if(timeStr.isNotBlank()) {
            // split the string up
            timeStr = timeStr.replace("(?i)(\\s|,|and)".toRegex(), "")
                .replace("(?is)(-?\\d+|[a-z]+)".toRegex(), "$1 ")
                .trim { it <= ' ' }
            val values = timeStr.split("\\s+".toRegex())

            try {
                var i = 0
                while (i < values.size)
                {
                    // the unit provided
                    val unit = values[i+1].toLowerCase()
                        .let { start -> enumValues<TimeUnit>().first { unit -> start.startsWith(unit.abbreviation) } }

                    val num = values[i].toLong().let { num -> outputUnit.convert(num, unit) }

                    count += num
                    i += 2
                }
            } catch (e: Exception) {
                return 0
            }

            return count
        } else {
            return null
        }
    }
}