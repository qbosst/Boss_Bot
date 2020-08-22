package me.qbosst.bossbot.bot.commands.misc.colour

import me.qbosst.bossbot.bot.commands.Command
import me.qbosst.bossbot.database.data.GuildColoursData
import me.qbosst.bossbot.util.makeSafe
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object ColourRemoveCommand: Command(
        "remove",
        "Removes a custom guild colour",
        usage = listOf("<name>"),
        userPermissions = listOf(Permission.MANAGE_EMOTES, Permission.MANAGE_SERVER)
) {
    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        if(args.isNotEmpty())
        {
            if(GuildColoursData.remove(event.guild, args[0]))
            {
                event.channel.sendMessage("successfully removed ${args[0].makeSafe()}").queue()
            }
            else
            {
                event.channel.sendMessage("could not find ${args[0].makeSafe()}").queue()
            }
        }
        else
        {
            event.channel.sendMessage("Please provide the name of the colour you would like to remove.").queue()
        }
    }
}