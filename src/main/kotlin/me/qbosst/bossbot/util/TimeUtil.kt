package me.qbosst.bossbot.util

import java.time.format.DateTimeFormatter

object TimeUtil
{
    private val TIME_REGEX = Regex("(?is)^((\\s*-?\\s*\\d+\\s*(${enumValues<TimeUnit>().map { it.regex }.joinToString("|")})\\s*,?\\s*(and)?)*).*")
    val DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")

    fun secondsToString(seconds: Long, locale: (TimeUnit, Long) -> String = { unit, count -> "${count}${unit.shortName}" }): String
    {
        if(seconds == 0L)
            return "0s"

        val negative = seconds < 0
        var secs = if(negative) -seconds else seconds
        val sb = StringBuilder()

        for(unit in TimeUnit.values().sortedBy { it.value }.reversed())
        {
            val calc = secs / unit.value
            if(calc > 0)
            {
                sb.append("${locale.invoke(unit, calc)} ")
                secs -= (calc) * unit.value
            }
        }

        sb.deleteCharAt(sb.lastIndex).toString()
        if(negative)
            sb.insert(0, "-")
        return sb.toString()
    }

    fun parseTime(time: String): Long
    {
        var timeStr = time.replace(TIME_REGEX, "$1")
        var seconds: Long = 0
        if(timeStr.isNotEmpty())
        {
            timeStr = timeStr.replace("(?i)(\\s|,|and)".toRegex(), "")
                    .replace("(?is)(-?\\d+|[a-z]+)".toRegex(), "$1 ")
                    .trim { it <= ' ' }
            val values = timeStr.split("\\s+".toRegex())
            try
            {
                var i = 0
                while(i < values.size)
                {
                    var num = values[i].toInt()
                    when
                    {
                        values[i+1].toLowerCase().startsWith("m") -> num *= 60
                        values[i+1].toLowerCase().startsWith("h") -> num *= 60 * 60
                        values[i+1].toLowerCase().startsWith("d") -> num *= 60 * 60 * 24
                    }
                    seconds += num
                    i += 2
                }
            }
            catch (e: Exception)
            {
                return 0
            }

        }
        return seconds
    }

    enum class TimeUnit(val value: Int, val shortName: String, val longName: String, val regex: Regex)
    {
        SECONDS(1, "s", "seconds", Regex("s(ec(ond)?s?)?")),
        MINUTES(60, "m", "minutes", Regex("m(in(ute)?s?)?")),
        HOURS(60*60, "h", "hours", Regex("h((ou)?rs?)?")),
        DAYS(60*60*24, "d", "days", Regex("d(ays?)?"))
    }
}