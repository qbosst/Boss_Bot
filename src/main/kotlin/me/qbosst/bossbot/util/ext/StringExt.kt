package me.qbosst.bossbot.util.ext

fun String.wrap(wrapper: String): String = wrapper+this+wrapper

fun String.maxLength(maxLength: Int, ending: String = "..."): String =
    if(length > maxLength) substring(0, maxLength-ending.length)+ending else this

/**
 * If this string is blank, it will replace it with a zero width character.
 */
fun String.zeroWidthIfBlank() = if(isBlank()) "\u200E" else this