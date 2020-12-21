package me.qbosst.bossbot.util

import java.lang.IllegalArgumentException
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object TimeUtil
{
    private val timeRegex = "(?is)^((\\s*-?\\s*\\d+\\s*(${enumValues<TimeUnit>().joinToString("|") { it.regex }})\\s*,?\\s*(and)?)*).*".toRegex()

    private val zoneMatcher: Map<ZoneId, Regex> = ZoneId.getAvailableZoneIds().sorted()
            .map { id ->
                val zoneId = ZoneId.of(id)

                // split the zone id into a more user-friendly regex
                val zoneName = StringBuilder("(")
                        .apply {
                            val connector = "[-_\\s/]?"
                            val separator: Pattern = Pattern.compile("[^/]+")

                            val matcher = separator.matcher(id)
                            val replaceMatching = "[_-]".toRegex()
                            val replaceWith = "[-_\\\\s]?"
                            while (matcher.find())
                                append("(${matcher.group().replace(replaceMatching, replaceWith)})?${connector}")

                            deleteRange(lastIndex-connector.length, lastIndex).append(")")
                        }

                // get the abbreviations of this time zone
                val abbreviations = StringBuilder("(")
                        .apply {
                            val zone = TimeZone.getTimeZone(zoneId)
                            append("${zone.getDisplayName(true, 0)})|(${zone.getDisplayName(false, 0)})")
                        }

                Pair(zoneId, "(${zoneName}|${abbreviations})".toLowerCase().toRegex())
            }
            .toMap()

    private val TimeUnit.value
        get() = TimeUnit.NANOSECONDS.convert(1, this)

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

    val TimeUnit.formattedName
        get() = name.toLowerCase()

    val dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy")

    fun filterZones(query: String) = query.toLowerCase()
            .let { q -> zoneMatcher.filter { zone -> q.matches(zone.value) } }
            .map { entry -> entry.key }

    /**
     *  Gets the zone id of the [query] provided case insensitive.
     *
     *  @param query The zoneId string to try and get the Zone Id object from
     *
     *  @return Zone Id object. Null if no zone id corresponded to the parameter given
     */
    fun zoneIdOf(query: String?): ZoneId? = query
            ?.let { q -> ZoneId.getAvailableZoneIds().firstOrNull() { id -> q.equals(id, true)} }
            ?.let { id -> ZoneId.of(id) }

    fun parseTime(time: String, outputUnit: TimeUnit = TimeUnit.SECONDS): Long
    {
        var timeStr = time.replace(timeRegex, "$1")
        var count: Long = 0
        if(timeStr.isNotBlank())
        {
            // split the string up
            timeStr = timeStr.replace("(?i)(\\s|,|and)".toRegex(), "")
                    .replace("(?is)(-?\\d+|[a-z]+)".toRegex(), "$1 ")
                    .trim { it <= ' ' }
            val values = timeStr.split("\\s+".toRegex())

            try
            {
                var i = 0
                while (i < values.size)
                {
                    // the unit provided
                    val unit = values[i+1].toLowerCase()
                            .let { start -> TimeUnit.values().first { unit -> start.startsWith(unit.abbreviation) } }

                    val num = values[i].toLong().let { num -> outputUnit.convert(num, unit) }

                    count += num
                    i += 2
                }
            }
            catch (e: Exception)
            {
                return 0
            }
        }
        return count
    }

    fun timeToString(time: Long, unit: TimeUnit = TimeUnit.SECONDS,
                     locale: (TimeUnit, Long) -> String = {tu, l -> "${l}${tu.abbreviation}"}): String
    {
        if (time == 0L)
            return "0${unit.abbreviation}"

        val isNegative = time < 0
        var amount = if(isNegative) -time else time
        val sb = StringBuilder(if(isNegative) "-" else "")

        for (index in (unit.ordinal until TimeUnit.values().count()).reversed())
        {
            val tempUnit = enumValues<TimeUnit>().first { it.ordinal == index }
            val tempValue = tempUnit.value / unit.value

            val calc = amount / tempValue
            if(calc > 0)
            {
                sb.append("${locale.invoke(tempUnit, calc)} ")
                amount -= (calc) * tempValue
            }
        }

        return sb.deleteCharAt(sb.lastIndex).toString()
    }

    fun timeToString(time: Int, unit: TimeUnit = TimeUnit.SECONDS,
                     locale: (TimeUnit, Long) -> String = { tu, l -> "${l}${tu.abbreviation}"}): String =
            timeToString(time.toLong(), unit, locale)
}