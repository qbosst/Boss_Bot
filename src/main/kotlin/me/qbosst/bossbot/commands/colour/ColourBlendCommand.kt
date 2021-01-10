package me.qbosst.bossbot.commands.colour

import me.qbosst.bossbot.database.manager.colours
import me.qbosst.bossbot.entities.parsers.ColourParser
import me.qbosst.bossbot.util.ColourUtil
import me.qbosst.bossbot.util.ColourUtil.blend
import me.qbosst.bossbot.util.ColourUtil.nextColour
import me.qbosst.bossbot.util.ColourUtil.sendColourEmbed
import me.qbosst.jda.ext.commands.annotations.CommandFunction
import me.qbosst.jda.ext.commands.annotations.Greedy
import me.qbosst.jda.ext.commands.entities.Command
import me.qbosst.bossbot.entities.Context
import java.util.regex.Pattern
import kotlin.random.Random

class ColourBlendCommand: Command() {
    override val label: String = "blend"

    @CommandFunction
    fun execute(ctx: Context, @Greedy colours: Array<String>) {

        val toBlend = colours.mapNotNull { arg ->
            val result = parser.parse(arg)
            val lower = arg.toLowerCase()

            return@mapNotNull when {
                result.isPresent ->
                    result.get()
                randomMatcher.matcher(arg).matches() ->
                    Random.nextColour(true)
                else ->
                    ColourUtil.systemColours[lower] ?: ctx.guild?.colours?.get(lower)
            }
        }

        if(toBlend.isEmpty()) {
            ctx.messageChannel.sendMessage("invalid colours proiveded").queue()
        } else {
            ctx.messageChannel.sendColourEmbed(toBlend.blend()).queue()
        }
    }

    companion object {
        private val parser = ColourParser()
        private val randomMatcher = Pattern.compile("rand(om)?")
    }
}