package me.qbosst.bossbot.bot.commands.misc.colour

import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.util.isBoolFalse
import me.qbosst.bossbot.util.isBoolTrue
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.awt.Color
import kotlin.random.Random

/**
 *  Generates a random colour
 */
object ColourRandomCommand: Command(
        "random",
        "Generates a random colour",
        usage_raw = listOf("[hasAlpha]"),
        examples_raw = listOf("[true / false]"),
        aliases_raw = listOf("rand"),
        guildOnly = false,
        botPermissions = listOf(Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES)
)
{

    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        // Checks to see if opacity of colour should be random or not.
        val hasAlpha = if(args.isNotEmpty()) when
        {
            args[0].isBoolTrue() -> true
            args[0].isBoolFalse() -> false
            else -> false
        } else
            false

        // Sends colour embed.
        ColourCommand.sendColourEmbed(event.channel, Random.nextColour(hasAlpha)).queue()
    }
}

/**
 *  Generates a random colour
 *
 *  @param hasAlpha whether the opacity of the colour should be randomly generated as well.
 */
fun Random.nextColour(hasAlpha: Boolean = false): Color
{
    return if(hasAlpha)
        Color(nextInt(255), nextInt(255), nextInt(255), nextInt(255))
    else
        Color(nextInt(0xffffff))
}