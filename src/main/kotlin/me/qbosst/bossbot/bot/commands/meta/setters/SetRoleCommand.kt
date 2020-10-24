package me.qbosst.bossbot.bot.commands.meta.setters

import me.qbosst.bossbot.bot.commands.meta.SetterCommand
import me.qbosst.bossbot.util.getRoleByString
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

abstract class SetRoleCommand(
        name: String,
        description: String = "none",
        usages: List<String> = listOf(),
        examples: List<String> = listOf(),
        aliases: List<String> = listOf(),
        userPermissions: List<Permission> = listOf(),
        botPermissions: List<Permission> = listOf(),
        displayName: String,
        private val isInteractive: Boolean

): SetterCommand<Role>(name, description, usages, examples, aliases, botPermissions, userPermissions, displayName)
{
    final override fun getFromArguments(event: MessageReceivedEvent, args: List<String>): Role?
    {
        val string = args.joinToString(" ")
        val role = event.guild.getRoleByString(string)
        return when
        {
            role == null ->
            {
                onUnSuccessfulSet(event.textChannel, "Could not find role.")
                null
            }
            isInteractive && !event.guild.selfMember.canInteract(role) ->
            {
                onUnSuccessfulSet(event.textChannel, "I cannot interact with this role!")
                null
            }
            else -> role
        }
    }

    final override fun getString(value: Role?): String = value?.asMention ?: "`none`"

}