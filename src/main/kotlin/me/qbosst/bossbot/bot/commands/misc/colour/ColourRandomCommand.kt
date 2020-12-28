package me.qbosst.bossbot.bot.commands.misc.colour

import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.util.ColourUtil.nextColour
import me.qbosst.bossbot.util.ColourUtil.sendColourEmbed
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
        usages = listOf("[hasAlpha]"),
        examples = listOf("[true / false]"),
        aliases = listOf("rand"),
        guildOnly = false,
        botPermissions = listOf(Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES)
)
{

    override fun execute(event: MessageReceivedEvent, args: List<String>, flags: Map<String, String?>)
    {
        // checks to see if opacity of colour should be random or not.
        val hasAlpha = if(args.isNotEmpty()) args[0].isBoolTrue() else false

        // sends colour embed.
        event.channel.sendColourEmbed(Random.nextColour(hasAlpha)).queue()
    }
}