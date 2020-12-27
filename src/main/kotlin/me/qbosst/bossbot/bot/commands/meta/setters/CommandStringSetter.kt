package me.qbosst.bossbot.bot.commands.meta.setters

import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.bot.commands.meta.CommandSetter
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

abstract class CommandStringSetter<K>(label: String,
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
                                      val maxLength: Int):

        CommandSetter<K, String>(label, description, usages, examples, aliases, guildOnly, developerOnly,
                userPermissions, botPermissions, children, displayName)
{
    final override fun getValue(event: MessageReceivedEvent, args: List<String>, key: K): String?
    {
        val string = args.joinToString(" ")
        return when
        {
            string.length > maxLength ->
            {
                onUnsuccessfulSet(event.channel, "$displayName cannot be longer than $maxLength characters")
                null
            }
            else -> string
        }
    }
}