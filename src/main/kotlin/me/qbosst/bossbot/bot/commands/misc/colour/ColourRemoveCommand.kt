package me.qbosst.bossbot.bot.commands.misc.colour

import me.qbosst.bossbot.bot.argumentMissing
import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.database.managers.GuildColoursManager
import me.qbosst.bossbot.util.maxLength
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object ColourRemoveCommand: Command(
        "remove",
        "Removes a custom guild colour",
        usage_raw = listOf("<name>"),
        userPermissions = listOf(Permission.MANAGE_EMOTES, Permission.MANAGE_SERVER),
        guildOnly = true
)
{

    override fun execute(event: MessageReceivedEvent, args: List<String>, flags: Map<String, String?>)
    {
        if(args.isNotEmpty())
        {
            // Name of colour
            val name = args[0]

            // Tries to remove colour from database.
            if(GuildColoursManager.remove(event.guild, name))
                event.channel.sendMessage("Successfully removed `${name.maxLength()}`").queue()
            else
                event.channel.sendMessage("There is no colour in this guild named `${name.maxLength()}`").queue()
        }
        else
            event.channel.sendMessage(argumentMissing("name of the colour you would like to remove")).queue()
    }
}