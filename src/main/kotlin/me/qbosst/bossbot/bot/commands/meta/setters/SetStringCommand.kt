package me.qbosst.bossbot.bot.commands.meta.setters

import me.qbosst.bossbot.bot.commands.meta.SetterCommand
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

abstract class SetStringCommand(
        name: String,
        description: String = "none",
        usages: List<String> = listOf(),
        examples: List<String> = listOf(),
        aliases: List<String> = listOf(),
        userPermissions: List<Permission> = listOf(),
        botPermissions: List<Permission> = listOf(),
        displayName: String,
        private val maxLength: Int
): SetterCommand<String>(name, description, usages, examples, aliases, botPermissions, userPermissions, displayName)
{
    override fun getFromArguments(event: MessageReceivedEvent, args: List<String>): String?
    {
        val string = args.joinToString(" ")
        return when
        {
            string.length > maxLength ->
            {
                onUnSuccessfulSet(event.textChannel, "$displayName cannot be longer than $maxLength characters!")
                null
            }
            else -> string
        }
    }

    override fun getString(value: String?): String = value ?: "`none`"
}