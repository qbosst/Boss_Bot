package me.qbosst.bossbot.util

/**
 *  Converts an integer (0-255) into a string in hex
 */
fun Int.toHex(): String {

    val value = coerceIn(0, 255)

    return when
    {
        value <= 15 -> "0${value.toString(16)}"
        value.toString(16).length == 1 -> "${value.toString(16)}0"
        else -> value.toString(16)
    }
}