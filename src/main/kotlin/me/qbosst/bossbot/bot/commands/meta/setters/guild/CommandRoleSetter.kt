package me.qbosst.bossbot.bot.commands.meta.setters.guild

import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.util.extensions.getRoleByString
import me.qbosst.bossbot.util.maxLength
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

abstract class CommandRoleSetter(label: String,
                                 description: String = "none",
                                 usages: Collection<String> = listOf(),
                                 examples: Collection<String> = listOf(),
                                 aliases: Collection<String> = listOf(),
                                 guildOnly: Boolean = true,
                                 developerOnly: Boolean = false,
                                 userPermissions: Collection<Permission> = listOf(),
                                 botPermissions: Collection<Permission> = listOf(),
                                 children: Collection<Command> = listOf(),
                                 displayName: String,
                                 val isInteractive: Boolean
):
        CommandGuildSetter<Role>(label, description, usages, examples, aliases, guildOnly, developerOnly,
                userPermissions, botPermissions, children, "$displayName Role")
{
    override fun getValue(event: MessageReceivedEvent, args: List<String>, key: Guild): Role?
    {
        val query = args.joinToString(" ")
        val role = event.guild.getRoleByString(query)
        return when
        {
            role == null ->
            {
                onUnsuccessfulSet(event.channel, "Could not find role `${query.maxLength(32)}`")
                null
            }
            isInteractive && !event.guild.selfMember.canInteract(role) ->
            {
                onUnsuccessfulSet(event.channel, "`I cannot interact with `${role.name}`")
                null
            }
            else -> role
        }
    }

    override fun getString(value: Role?): String = value?.asMention ?: "`none`"
}