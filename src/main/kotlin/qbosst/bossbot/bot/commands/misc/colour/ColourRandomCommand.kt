package qbosst.bossbot.bot.commands.misc.colour

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import qbosst.bossbot.bot.commands.Command
import qbosst.bossbot.util.isBoolFalse
import qbosst.bossbot.util.isBoolTrue
import java.awt.Color
import kotlin.random.Random

object ColourRandomCommand: Command(
        "random",
        "Generates a random colour",
        usage = listOf("[hasAlpha]"),
        examples = listOf("[true / false]"),
        aliases = listOf("rand"),
        guildOnly = false,
        botPermissions = listOf(Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES)
) {
    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        val hasAlpha = if(args.isNotEmpty()) when
        {
            args[0].isBoolTrue() -> true
            args[0].isBoolFalse() -> false
            else -> false
        } else false

        ColourCommand.sendColourEmbed(event.channel, Random.nextColour(hasAlpha)).queue()
    }
}

fun Random.nextColour(hasAlpha: Boolean = false): Color
{
    return if(hasAlpha)
    {
        Color(nextInt(255), nextInt(255), nextInt(255), nextInt(255))
    }
    else
    {
        Color(nextInt(0xffffff))
    }
}