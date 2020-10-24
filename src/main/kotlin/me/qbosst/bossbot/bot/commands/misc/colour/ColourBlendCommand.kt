package me.qbosst.bossbot.bot.commands.misc.colour

import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.database.managers.GuildColoursManager
import me.qbosst.bossbot.util.assertNumber
import me.qbosst.bossbot.util.getGuildOrNull
import me.qbosst.bossbot.util.maxLength
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.awt.Color
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 *  This command will take in a list of colour and mix them all together equally.
 */
object ColourBlendCommand : Command(
    "blend",
    "Mixes the provided colours equally",
    guildOnly = false,
    usage = listOf("[colours...]"),
    examples = listOf("red green ffeedd", "ff0e329a orange a2f6e3"),
    botPermissions = listOf(Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES)
)
{
    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        if(args.isNotEmpty())
        {
            val colours = mutableSetOf<Color>()
            val guildColours = GuildColoursManager.get(event.getGuildOrNull())

            // Gets all the colours the user wants to mix
            for(arg in args)
            {
                val colour = systemColours[arg] ?: getColourByHex(arg) ?: if(arg.toLowerCase().matches(Regex("rand(om)?"))) Random.nextColour(false) else null ?: guildColours.get(arg)
                if(colour != null)
                    colours.add(colour)
                else
                {
                    event.channel.sendMessage("`${arg.maxLength()}` is not a valid colour!").queue()
                    return
                }
            }

            // Sends the mixed colour result
            ColourCommand.sendColourEmbed(event.channel, colours.blend()).queue()
        }
        else
            event.channel.sendMessage("You must provide at least 1 colour!").queue()
    }
}

fun Collection<Color>.blend(): Color
{
    val ratio: Float = 1f / this.size; var red = 0; var green = 0; var blue = 0; var alpha = 0
    for(colour in this)
    {
        red += (colour.red * ratio).roundToInt()
        green += (colour.green * ratio).roundToInt()
        blue += (colour.blue * ratio).roundToInt()
        alpha += (colour.alpha * ratio).roundToInt()
    }

    return Color(assertNumber(0, 255, red), assertNumber(0, 255, green), assertNumber(0, 255, blue), assertNumber(0, 255, alpha))
}

fun mixColours(vararg pair: Pair<Color, Float>): Color
{
    var percentage: Float = 0.0F
    var index = 0

    var r = 0; var g = 0; var b = 0; var a = 0
    while (percentage <= 1 && pair.size > index)
    {
        r += (pair[index].first.red * pair[index].second).roundToInt()
        g += (pair[index].first.green * pair[index].second).roundToInt()
        b += (pair[index].first.blue * pair[index].second).roundToInt()
        a += (pair[index].first.alpha * pair[index].second).roundToInt()

        percentage += pair[index].second
        index++
    }
    return Color(assertNumber(0, 255, r), assertNumber(0, 255, g), assertNumber(0, 255, b), assertNumber(0, 255, a))
}