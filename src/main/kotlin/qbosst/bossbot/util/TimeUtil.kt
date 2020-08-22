package qbosst.bossbot.util

import java.time.format.DateTimeFormatter

private val timeRegex = "(?is)^((\\s*-?\\s*\\d+\\s*(${TimeUnit.DAYS.regex}|${TimeUnit.HOURS.regex}|${TimeUnit.MINUTES.regex}|${TimeUnit.SECONDS.regex})\\s*,?\\s*(and)?)*).*".toRegex()
val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")

fun secondsToString(seconds: Long): String
{
    var secs = seconds
    val sb = StringBuilder()

    return if(secs > 0) {
        for(unit in TimeUnit.values().sortedBy { it.value }.reversed())
        {
            val calc = secs / unit.value
            if(calc > 0)
            {
                sb.append(calc).append("${unit.shortName} ")
                secs -= (calc) * unit.value
            }
        }

        sb.deleteCharAt(sb.lastIndex).toString()
    } else "0s"
}

fun getSeconds(time: String): Long
{
    var timeStr = time.replace(timeRegex, "$1")
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

private enum class TimeUnit(val value: Int, val shortName: String, val longName: String, val regex: Regex)
{
    SECONDS(1, "s", "seconds", Regex("s(ec(ond)?s?)?")),
    MINUTES(60, "m", "minutes", Regex("m(in(ute)?s?)?")),
    HOURS(60*60, "h", "hours", Regex("h((ou)?rs?)?")),
    DAYS(60*60*24, "d", "days", Regex("d(ays?)?"))
}