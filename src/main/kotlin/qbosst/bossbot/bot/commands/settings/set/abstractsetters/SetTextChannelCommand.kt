package qbosst.bossbot.bot.commands.settings.set.abstractsetters

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import qbosst.bossbot.util.getTextChannelByString

abstract class SetTextChannelCommand(
        name: String,
        description: String = "none",
        usage: List<String> = listOf(),
        examples: List<String> = listOf(),
        aliases: List<String> = listOf(),
        botPermissions: List<Permission> = listOf(),
        displayName: String,
        private val textChannelPermissions: Set<Permission> = setOf()
):
        SetterCommand<TextChannel>(
                name,
                description,
                usage,
                examples,
                aliases,
                botPermissions,
                displayName
        ) {

    final override fun isValid(event: MessageReceivedEvent, args: List<String>): Boolean
    {
        val string: String = args.joinToString(" ")
        val tc: TextChannel? = event.guild.getTextChannelByString(string)
        return when
        {
            tc == null ->
            {
                event.channel.sendMessage("Could not find channel.").queue()
                false
            }
            !event.guild.selfMember.hasPermission(tc, textChannelPermissions) ->
            {
                event.channel.sendMessage("I do not have the required permissions for this channel: `${textChannelPermissions.joinToString("`, `")}`").queue()
                false
            }
            else -> true
        }
    }

    final override fun getFromArguments(event: MessageReceivedEvent, args: List<String>): TextChannel?
    {
        return event.guild.getTextChannelByString(args.joinToString(" "))
    }

    override fun getAsString(guild: Guild): String
    {
        val tc: TextChannel? = get(guild)
        return tc?.asMention ?: "none"
    }
}