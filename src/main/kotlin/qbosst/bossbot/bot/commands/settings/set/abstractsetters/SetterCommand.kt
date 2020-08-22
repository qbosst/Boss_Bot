package qbosst.bossbot.bot.commands.settings.set.abstractsetters

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import qbosst.bossbot.bot.commands.Command

abstract class SetterCommand<T>(
        name: String,
        description: String = "none",
        usage: List<String> = listOf(),
        examples: List<String> = listOf(),
        aliases: List<String> = listOf(),
        botPermissions: List<Permission> = listOf(),
        val displayName: String
):
        Command(
                name,
                description,
                usage,
                examples,
                aliases,
                true,
                listOf(Permission.ADMINISTRATOR),
                botPermissions
        ) {

    final override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        if(args.isNotEmpty())
        {
            if(args[0].toLowerCase() == "none" || args[0].toLowerCase() == "null")
            {
                set(event.guild, null)
                event.channel.sendMessage("Successfully cleared").queue()
            }
            else
            {
                if(isValid(event, args))
                {
                    set(event.guild, getFromArguments(event, args))
                    event.channel.sendMessage("successfully set").queue()
                }
                else
                {
                    event.channel.sendMessage("unsuccessful set").queue()
                }
            }
        }
        else
        {
            event.channel.sendMessage(getAsString(event.guild)).queue()
        }
    }

    protected abstract fun isValid(event: MessageReceivedEvent, args: List<String>): Boolean

    protected abstract fun set(guild: Guild, obj: T?): T?

    abstract fun get(guild: Guild): T?

    abstract fun getFromArguments(event: MessageReceivedEvent, args: List<String>): T?

    abstract fun getAsString(guild: Guild): String
}