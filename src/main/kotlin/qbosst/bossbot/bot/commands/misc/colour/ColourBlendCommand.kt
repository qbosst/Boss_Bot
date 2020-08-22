package qbosst.bossbot.bot.commands.misc.colour

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import qbosst.bossbot.bot.commands.Command
import qbosst.bossbot.database.data.GuildColoursData
import qbosst.bossbot.util.assertNumber
import qbosst.bossbot.util.makeSafe
import java.awt.Color
import kotlin.math.roundToInt
import kotlin.random.Random

object ColourBlendCommand : Command(
    "blend",
    "Mixes the provided colours equally",
    guildOnly = false,
    usage = listOf("[colours...]"),
    examples = listOf("red green ffeedd", "ff0e329a orange a2f6e3"),
    botPermissions = listOf(Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES)
) {
    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        if(args.isNotEmpty())
        {
            val colours = mutableSetOf<Color>()
            val guildColours = GuildColoursData.get(if(event.isFromGuild) event.guild else null)
            for(arg in args)
            {
                colours.add(
                        guildColours.get(arg) ?: systemColours[arg] ?: getColourByHex(arg)
                        ?: if(arg.toLowerCase().matches(Regex("rand(om)?")))
                        {
                            Random.nextColour(false)
                        }
                        else
                        {
                            event.channel.sendMessage("`${arg.makeSafe()}` is not a valid colour!").queue()
                            return
                        }
                )
            }
            ColourCommand.sendColourEmbed(event.channel, colours.blend()).queue()
        }
        else
        {
            event.channel.sendMessage("You must provide at least 1 colour!").queue()
        }
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

    var r = 0; var g = 0; var b = 0; var a = 0;
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