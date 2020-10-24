package me.qbosst.bossbot.bot.commands.meta.setters

import me.qbosst.bossbot.bot.commands.meta.SetterCommand
import me.qbosst.bossbot.util.getTextChannelByString
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

abstract class SetTextChannelCommand(
        name: String,
        description: String = "none",
        usages: List<String> = listOf(),
        examples: List<String> = listOf(),
        aliases: List<String> = listOf(),
        userPermissions: List<Permission> = listOf(),
        botPermissions: List<Permission> = listOf(),
        displayName: String,
        private val textChannelPermissions: Array<Permission> = arrayOf()

): SetterCommand<TextChannel>(name, description, usages, examples, aliases, botPermissions, userPermissions, displayName)
{
    final override fun getFromArguments(event: MessageReceivedEvent, args: List<String>): TextChannel?
    {
        val string = args.joinToString(" ")
        val tc = event.guild.getTextChannelByString(string)
        return when
        {
            tc == null ->
            {
                onUnSuccessfulSet(event.textChannel, "Could not find channel")
                null
            }
            !event.guild.selfMember.hasPermission(tc, textChannelPermissions.toMutableSet()) ->
            {
                onUnSuccessfulSet(event.textChannel, "I do not have the required permissions for this channel.")
                null
            }
            else -> tc
        }
    }

    final override fun getString(value: TextChannel?): String = value?.asMention ?: "`none`"
}