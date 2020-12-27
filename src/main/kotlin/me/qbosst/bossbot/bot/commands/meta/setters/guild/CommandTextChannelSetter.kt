package me.qbosst.bossbot.bot.commands.meta.setters.guild

import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.util.getTextChannelByString
import me.qbosst.bossbot.util.maxLength
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

abstract class CommandTextChannelSetter(label: String,
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
                                        val textChannelPermissions: Collection<Permission> = listOf()
):
        CommandGuildSetter<TextChannel>(label, description, usages, examples, aliases, guildOnly, developerOnly,
                userPermissions, botPermissions, children, "$displayName Channel")
{
    override fun getValue(event: MessageReceivedEvent, args: List<String>, key: Guild): TextChannel?
    {
        val query = args.joinToString(" ")
        val tc = event.guild.getTextChannelByString(query)
        return when
        {
            tc == null ->
            {
                onUnsuccessfulSet(event.channel, "Could not find channel `${query.maxLength(32)}`")
                null
            }
            !event.guild.selfMember.hasPermission(tc, textChannelPermissions) ->
            {
                onUnsuccessfulSet(event.channel, "Missing Permissions ${textChannelPermissions} for ${tc.asMention}")
                null
            }
            else -> tc
        }
    }

    override fun getString(value: TextChannel?): String = value?.asMention ?: "`none`"
}