package me.qbosst.bossbot.entities.parsers

import me.qbosst.jda.ext.commands.entities.IContext
import me.qbosst.jda.ext.commands.parsers.Parser
import java.util.*
import java.util.regex.Pattern
import java.awt.Color as Colour

class ColourParser: Parser<Colour>
{
    override suspend fun parse(ctx: IContext, param: String): Optional<Colour> = parse(param)

    override suspend fun parse(ctx: IContext, params: List<String>): Pair<Array<Colour>, List<String>> =
        Parser.defaultParse(this, ctx, params)

    fun parse(param: String): Optional<Colour>
    {
        val hex6 = hexMatcher6.matcher(param)
        if(hex6.matches())
        {
            val hexInt = hex6.group("hex").toInt(16)
            return Optional.of(Colour(hexInt, false))
        }

        val hex8 = hexMatcher8.matcher(param)
        if(hex8.matches())
        {
            val (r, g, b, a) = param.chunked(2).map { it.toInt(16) }
            return Optional.of(Colour(r, g, b, a))
        }

        val rgba = rgbaMatcher.matcher(param)
        if(rgba.matches())
        {
            val red = rgba.group("r").toInt()
            val green = rgba.group("g").toInt()
            val blue = rgba.group("b").toInt()
            val alpha = rgba.group("a")?.toIntOrNull()

            return Optional.of(Colour(red, green, blue, alpha ?: 255))
        }

        return Optional.empty()
    }

    companion object
    {
        private val hexMatcher6 = Pattern.compile("(#)?(?<hex>[0-9A-Fa-f]{6})")
        private val hexMatcher8 = Pattern.compile("(#)?(?<hex>[0-9A-Fa-f]{8})")
        private val rgbaMatcher = Pattern.compile("((?<r>[0-9]{1,3})\\s(?<g>[0-9]{1,3})\\s(?<b>[0-9]{1,3})((\\s)(?<a>[0-9]{1,3}))?)")
    }
}