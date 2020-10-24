package me.qbosst.bossbot.util

/**
 *  This checks if a string is a hex value
 *
 *  @param amount The amount of characters it should check, with the default being 6
 *
 *  @return If the string is a hex value
 */
fun String.isHex(amount: Int = 6): Boolean = matches(Regex("^[0-9A-Fa-f]{$amount}$"))

/**
 *  This will check if a string is a true boolean
 *
 *  @return Whether the string represents a boolean that is true
 */
fun String.isBoolTrue(): Boolean = toLowerCase().matches(Regex("t(rue)?")) || this == "1"

/**
 *  This will check if a string is a false boolean
 *
 *  @return Whether the string represents a boolean that is false
 */
fun String.isBoolFalse(): Boolean = toLowerCase().matches(Regex("f(alse)?")) || this == "0"

fun String.toBooleanOrNull(): Boolean? = when {
    this.isBoolFalse() -> false
    this.isBoolTrue() -> true
    else -> null
}