package me.qbosst.bossbot.bot.commands.meta.setters.guild

import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.bot.commands.meta.CommandSetter
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

abstract class CommandGuildSetter<V>(label: String,
                                     description: String = "none",
                                     usages: Collection<String> = listOf(),
                                     examples: Collection<String> = listOf(),
                                     aliases: Collection<String> = listOf(),
                                     guildOnly: Boolean = true,
                                     developerOnly: Boolean = false,
                                     userPermissions: Collection<Permission> = listOf(),
                                     botPermissions: Collection<Permission> = listOf(),
                                     children: Collection<Command> = listOf(),
                                     displayName: String):
        CommandSetter<Guild, V>(label, description, usages, examples, aliases, guildOnly, developerOnly,
                userPermissions, botPermissions, children, displayName)
{
    final override fun getKey(event: MessageReceivedEvent, args: List<String>): Guild = event.guild
}