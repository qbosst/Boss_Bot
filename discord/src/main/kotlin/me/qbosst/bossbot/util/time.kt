package me.qbosst.bossbot.util

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaZoneId
import kotlin.time.DurationUnit
import java.util.TimeZone as JTimeZone

val DurationUnit.regex
    get() = when(this) {
        DurationUnit.NANOSECONDS -> "n(ano)?s(ec(ond)?s?)?"
        DurationUnit.MICROSECONDS -> "μ(icro)?s(ec(ond)?s?)?"
        DurationUnit.MILLISECONDS -> "m(illi)?s(ec(ond)?s?)?"
        DurationUnit.SECONDS -> "s(ec(ond)?s?)?"
        DurationUnit.MINUTES -> "m(in(ute)?s?)?"
        DurationUnit.HOURS -> "h(((ou)?)rs?)?"
        DurationUnit.DAYS -> "d(ays?)?"
    }

val DurationUnit.abbreviation
    get() = when(this) {
        DurationUnit.NANOSECONDS -> "ns"
        DurationUnit.MICROSECONDS -> "μs"
        DurationUnit.MILLISECONDS -> "ms"
        DurationUnit.SECONDS -> "s"
        DurationUnit.MINUTES -> "m"
        DurationUnit.HOURS -> "h"
        DurationUnit.DAYS -> "d"
    }

fun TimeZone.toJavaTimeZone(): JTimeZone = JTimeZone.getTimeZone(toJavaZoneId())