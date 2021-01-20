package me.qbosst.bossbot.converters.impl

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.ParseException
import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.converters.SingleConverter
import com.kotlindiscord.kord.extensions.commands.parser.Argument
import dev.kord.rest.builder.interaction.OptionsBuilder
import dev.kord.rest.builder.interaction.StringChoiceBuilder
import java.util.regex.Pattern
import java.awt.Color as Colour

class ColourConverter(
    val colourProvider: suspend (CommandContext) -> Map<String, Colour> = { mapOf() }
): SingleConverter<Colour>() {
    override val signatureTypeString: String = "colour"

    override suspend fun parse(arg: String, context: CommandContext, bot: ExtensibleBot): Boolean {
        val hex6 = hexMatcher6.matcher(arg)
        if(hex6.matches()) {
            val value = hex6.group("hex").toInt(16)
            this.parsed = Colour(value, false)
            return true
        }

        val hex8 = hexMatcher8.matcher(arg)
        if(hex8.matches()) {
            val (r, g, b, a) = hex8.group("hex").chunked(2).map { it.toInt(16) }
            this.parsed = Colour(r, g, b, a)
            return true
        }

        val rgba = rgbaMatcher.matcher(arg)
        if(rgba.matches()) {
            val red = rgba.group("r").toInt().coerceIn(0, 255)
            val green = rgba.group("g").toInt().coerceIn(0, 255)
            val blue = rgba.group("b").toInt().coerceIn(0, 255)
            val alpha = rgba.group("a")?.toIntOrNull()?.coerceIn(0, 255)

            this.parsed = Colour(red, green, blue, alpha ?: 255)
            return true
        }

        val colours = colourProvider.invoke(context)
        if(colours.isNotEmpty()) {
            val colour = colours[arg.toLowerCase()]

            if(colour != null) {
                this.parsed = colour
                return true
            }
        }

        throw ParseException("'${arg}' is not a valid colour")

        return true
    }

    override suspend fun toSlashOption(arg: Argument<*>): OptionsBuilder =
        StringChoiceBuilder(arg.displayName, arg.description).apply { required = true }

    companion object {
        private val hexMatcher6 = Pattern.compile("(#)?(?<hex>[0-9A-Fa-f]{6})")
        private val hexMatcher8 = Pattern.compile("(#)?(?<hex>[0-9A-Fa-f]{8})")
        private val rgbaMatcher = Pattern.compile("((?<r>[0-9]{1,3})\\s(?<g>[0-9]{1,3})\\s(?<b>[0-9]{1,3})((\\s)(?<a>[0-9]{1,3}))?)")
    }
}