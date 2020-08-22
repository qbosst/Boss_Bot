package qbosst.bossbot.bot.commands.settings.set.abstractsetters

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

abstract class SetStringCommand(
        name: String,
        description: String = "none",
        usage: List<String> = listOf(),
        examples: List<String> = listOf(),
        aliases: List<String> = listOf(),
        botPermissions: List<Permission> = listOf(),
        displayName: String,
        protected val maxLength: Int
):
        SetterCommand<String>(
                name,
                description,
                usage,
                examples,
                aliases,
                botPermissions,
                displayName
        ) {

    override fun isValid(event: MessageReceivedEvent, args: List<String>): Boolean
    {
        val string: String = args.joinToString(" ")
        return if(string.length > maxLength)
        {
            event.channel.sendMessage("$name cannot be longer than $maxLength characters.").queue()
            false
        }
        else
        {
            true
        }
    }

    final override fun getFromArguments(event: MessageReceivedEvent, args: List<String>): String?
    {
        return args.joinToString(" ")
    }

    override fun getAsString(guild: Guild): String
    {
        return get(guild) ?: "none"
    }
}