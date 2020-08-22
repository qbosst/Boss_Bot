package qbosst.bossbot.bot.commands.settings.set

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import qbosst.bossbot.bot.commands.Command
import qbosst.bossbot.bot.commands.settings.set.abstractsetters.SetterCommand
import qbosst.bossbot.bot.commands.settings.set.setters.*
import qbosst.bossbot.util.makeSafe

object SetCommand: Command(
        "set",
        userPermissions = listOf(Permission.ADMINISTRATOR)
) {

    init
    {
        addCommands(
                SetMessageLogsChannelCommand, SetMuteRoleCommand, SetPrefixCommand, SetSuggestionChannelCommand,
                SetTimeZoneCommand, SetVoiceLogsChannelCommand, SetModerationLogsChannelCommand,
                SetWelcomeChannelCommand, SetWelcomeMessageCommand
        )
    }

    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        val embed = EmbedBuilder()
        for(command in getCommands())
        {
            embed.addField((command as SetterCommand<*>).displayName, command.getAsString(event.guild).makeSafe(32), true)
        }
        if(embed.fields.size % 3 == 2)
        {
            embed.addBlankField(true)
        }
        event.channel.sendMessage(embed.build()).queue()
    }
}