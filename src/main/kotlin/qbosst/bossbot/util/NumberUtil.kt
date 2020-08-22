package qbosst.bossbot.util

/**
 *  Converts an integer into a string in hex
 */
fun Int.toHex(): String {
    val value = assertNumber(0, 255, this)

    return when {
        value <= 15 ->
        {
            "0${value.toString(16)}"
        }
        value.toString(16).length == 1 ->
        {
            "${value.toString(16)}0"
        }
        else -> {
            value.toString(16)
        }
    }
}

fun <T: Number> assertNumber(min: T, max: T, value: T): T
{
    return when(value)
    {
        is Short -> if(value > (max as Short)) max else if(value < (min as Short)) min else value
        is Int -> if(value > (max as Int)) max else if(value < (min as Int)) min else value
        is Long -> if(value > (max as Long)) max else if(value < (min as Long)) min else value
        is Byte -> if(value > (max as Byte)) max else if(value < (min as Byte)) min else value
        is Float -> if(value > (max as Float)) max else if(value < (min as Float)) min else value
        is Double -> if(value > (max as Double)) max else if(value < (min as Double)) min else value
        else -> throw NumberFormatException("This inheritor is not supported!")
    }
}