package me.qbosst.bossbot.util

import me.qbosst.bossbot.bot.Constants

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

/**
 *  This will try to convert a String into a Boolean
 *
 *  @return Boolean. Null if the string could not be converted
 */
fun String.toBooleanOrNull(): Boolean? = when {
    this.isBoolFalse() -> false
    this.isBoolTrue() -> true
    else -> null
}

/**
 *  Splits a string up every n amount of characters
 *
 *  @param partitionSize The amount of characters to split it up after
 *
 *  @return List of strings
 */
fun String.split(partitionSize: Int): List<String>
{
    val parts = mutableListOf<String>()
    val len = length
    var i = 0
    while (i < len)
    {
        parts.add(this.substring(i, Math.min(len, i+partitionSize)))
        i+= partitionSize
    }
    return parts
}

/**
 *  Cuts off a string if it reaches the max length allowed
 *
 *  @param maxLength The maximum allowed of length that the string is allowed to be. Default is 32
 *
 *  @return String that is no longer than the maximum length specified.
 */
fun String.maxLength(maxLength: Int = 32, ending: String = "..."): String =
    if(length > maxLength) substring(0, maxLength-ending.length)+ending else this

/**
 *  Prevents Discord mentions by putting a zero width character after an '@'
 *
 *  @return Safe string that cannot mention anyone
 */
fun String.makeSafe(): String = replace("@", "@${Constants.ZERO_WIDTH}")