package me.qbosst.bossbot.bot.commands.settings.set.abstractsetters

import me.qbosst.bossbot.util.getRoleByString
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

abstract class SetRoleCommand(
        name: String,
        description: String = "none",
        usage: List<String> = listOf(),
        examples: List<String> = listOf(),
        aliases: List<String> = listOf(),
        botPermissions: List<Permission> = listOf(),
        displayName: String,
        private val isInteractive: Boolean
):
        SetterCommand<Role>(
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
        val role: Role? = event.guild.getRoleByString(string)
        return when
        {
            role == null ->
            {
                event.channel.sendMessage("Could not find role.").queue();
                false
            }
            isInteractive && !event.guild.selfMember.canInteract(role) ->
            {
                event.channel.sendMessage("I will not be able to interact with this role!").queue();
                false
            }
            else -> true
        }
    }

    final override fun getFromArguments(event: MessageReceivedEvent, args: List<String>): Role?
    {
        return event.guild.getRoleByString(args.joinToString(" "))
    }

    override fun getAsString(guild: Guild): String
    {
        val role: Role? = get(guild)
        return role?.asMention ?: "none"
    }
}