package qbosst.bossbot.util

/**
 *  This checks if a string is numeric
 */
fun String.isNumeric(): Boolean
{
    return matches(Regex("[0-9]"))
}

/**
 *  This checks if a string is a hex value
 *
 *  @param amount The amount of characters it should check, with the default being 6
 */
fun String.isHex(amount: Int = 6): Boolean
{
    return matches(Regex("^[0-9A-Fa-f]{$amount}"))
}

/**
 *  This will check if a string is a true boolean
 */
fun String.isBoolTrue(): Boolean
{
    return toLowerCase().matches(Regex("t(rue)?")) || this == "1"
}

/**
 *  This will check if a string is a false boolean
 */
fun String.isBoolFalse(): Boolean
{
    return toLowerCase().matches(Regex("f(alse)?")) || this == "0"
}