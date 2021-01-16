package me.qbosst.bossbot.converters

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.ParseException
import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.converters.CoalescingConverter
import com.kotlindiscord.kord.extensions.commands.converters.SingleConverter
import java.util.regex.Pattern
import java.awt.Color as Colour

private val hexMatcher6 = Pattern.compile("(#)?(?<hex>[0-9A-Fa-f]{6})")
private val hexMatcher8 = Pattern.compile("(#)?(?<hex>[0-9A-Fa-f]{8})")
private val rgbaMatcher = Pattern.compile("((?<r>[0-9]{1,3})\\s(?<g>[0-9]{1,3})\\s(?<b>[0-9]{1,3})((\\s)(?<a>[0-9]{1,3}))?)")

class ColourConverter(
    private val colourProvider: suspend (CommandContext) -> Map<String, Colour> = { mapOf() }
): SingleConverter<Colour>() {
    override val signatureTypeString: String = "colour"

    override suspend fun parse(arg: String, context: CommandContext, bot: ExtensibleBot): Boolean {
        this.parsed = parseColour(arg, context, colourProvider)
            ?: throwException(arg)

        return true
    }
}

class ColourCoalescingConverter(
    private val colourProvider: suspend (CommandContext) -> Map<String, Colour> = { mapOf() }
): CoalescingConverter<Colour>() {
    override val signatureTypeString: String = "colour"

    override suspend fun parse(args: List<String>, context: CommandContext, bot: ExtensibleBot): Int {
        val arg = args.joinToString(" ")

        this.parsed = parseColour(arg, context, colourProvider)
            ?: throwException(arg)

        return args.size
    }
}

private fun throwException(arg: String): Nothing =
    throw ParseException("'$arg' must be a hex value, a colour name or rgba value")

private suspend fun parseColour(
    arg: String,
    context: CommandContext,
    colourProvider: suspend (CommandContext) -> Map<String, Colour> = { mapOf() }
): Colour? {
    val hex6 = hexMatcher6.matcher(arg)
    if(hex6.matches()) {
        val value = hex6.group("hex").toInt(16)
        return Colour(value, false)
    }

    val hex8 = hexMatcher8.matcher(arg)
    if(hex8.matches()) {
        val (r, g, b, a) = arg.chunked(2).map { it.toInt(16) }
        return Colour(r, g, b, a)
    }

    val rgba = rgbaMatcher.matcher(arg)
    if(rgba.matches()) {
        val red = rgba.group("r").toInt().coerceIn(0, 255)
        val green = rgba.group("g").toInt().coerceIn(0, 255)
        val blue = rgba.group("b").toInt().coerceIn(0, 255)
        val alpha = rgba.group("a")?.toIntOrNull()?.coerceIn(0, 255)

        return Colour(red, green, blue, alpha ?: 255)
    }

    val colours = colourProvider.invoke(context)
    if(colours.isNotEmpty()) {
        val colour = colours[arg.toLowerCase()]

        if(colour != null) {
            return colour
        }
    }

    return null
}