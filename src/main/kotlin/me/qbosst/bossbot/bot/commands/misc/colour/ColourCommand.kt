package me.qbosst.bossbot.bot.commands.misc.colour

import me.qbosst.bossbot.bot.argumentInvalid
import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.util.*
import me.qbosst.bossbot.util.ColourUtil.sendColourEmbed
import me.qbosst.bossbot.util.extensions.getGuildOrNull
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object ColourCommand: Command(
        "colour",
        "Shows information about a specific colour",
        usages = listOf("<colour name | hex>"),
        examples = listOf("purple", "0feeed"),
        aliases = listOf("color"),
        guildOnly = false,
        botPermissions = listOf(Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES),
        children = listOf(ColourBlendCommand, ColourCreateCommand, ColourRandomCommand, ColourRemoveCommand)
)
{
    override fun execute(event: MessageReceivedEvent, args: List<String>, flags: Map<String, String?>)
    {
        // Tries to get a valid colour either from name or hex code
        if(args.isNotEmpty())
        {
            val colour = ColourUtil.getColourByQuery(args[0], event.getGuildOrNull())

            // If colour was found, send embed otherwise return error message
            if(colour != null)
                event.channel.sendColourEmbed(colour).queue()
            else
                event.channel.sendMessage(argumentInvalid(args[0], "colour")).queue()
        }
        else
            event.channel.sendMessage("Please enter the hex code or name of a colour!").queue()
    }
}