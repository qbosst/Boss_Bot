package me.qbosst.bossbot.bot.commands.misc.colour

import me.qbosst.bossbot.bot.argumentInvalid
import me.qbosst.bossbot.bot.argumentMissing
import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.util.ColourUtil
import me.qbosst.bossbot.util.ColourUtil.blend
import me.qbosst.bossbot.util.ColourUtil.nextColour
import me.qbosst.bossbot.util.ColourUtil.sendColourEmbed
import me.qbosst.bossbot.util.extensions.getGuildOrNull
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.awt.Color
import kotlin.random.Random

/**
 *  This command will take in a list of colour and mix them all together equally.
 */
object ColourBlendCommand : Command(
    "blend",
    "Mixes the provided colours equally",
    guildOnly = false,
    usages = listOf("[colours...]"),
    examples = listOf("red green ffeedd", "ff0e329a orange a2f6e3"),
    botPermissions = listOf(Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES)
)
{
    override fun execute(event: MessageReceivedEvent, args: List<String>, flags: Map<String, String?>)
    {
        if(args.isNotEmpty())
        {
            val colours = mutableSetOf<Color>()

            // Gets all the colours the user wants to mix
            for(arg in args)
            {
                val colour = ColourUtil.getColourByQuery(arg, event.getGuildOrNull())
                    ?: if(arg.toLowerCase().matches(Regex("rand(om)?"))) Random.nextColour(false) else null
                if(colour != null)
                    colours.add(colour)
                else
                {
                    event.channel.sendMessage(argumentInvalid(arg, "colour")).queue()
                    return
                }
            }

            // Sends the mixed colour result
            event.channel.sendColourEmbed(colours.blend()).queue()
        }
        else
            event.channel.sendMessage(argumentMissing("at least one colour")).queue()
    }
}