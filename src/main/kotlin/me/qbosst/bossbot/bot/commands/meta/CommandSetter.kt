package me.qbosst.bossbot.bot.commands.meta

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

abstract class CommandSetter<K, V>(label: String,
                                   description: String = "none",
                                   usages: Collection<String> = listOf(),
                                   examples: Collection<String> = listOf(),
                                   aliases: Collection<String> = listOf(),
                                   guildOnly: Boolean = true,
                                   developerOnly: Boolean = false,
                                   userPermissions: Collection<Permission> = listOf(),
                                   botPermissions: Collection<Permission> = listOf(),
                                   children: Collection<Command> = listOf(),
                                   val displayName: String

): Command(label, description,
        usages.plus("<${CLEAR_KEYWORDS.joinToString(" | ")}>"),
        examples.plus(CLEAR_KEYWORDS.random()),
        aliases, guildOnly, developerOnly, userPermissions, botPermissions, children)
{
    final override fun execute(event: MessageReceivedEvent, args: List<String>, flags: Map<String, String?>)
    {
        val key = getKey(event, args)
        if(args.isNotEmpty())
        {
            val new = (if(CLEAR_KEYWORDS.contains(args[0].toLowerCase()))
                null else (getValue(event, args, key) ?: return))
            val old = set(key, new)
            if(old == new)
                onAlreadySet(event.channel, old)
            else
                onSuccessfulSet(event.channel, old, new)
        }
        else
        {
            displayCurrent(event.channel, key, get(key))
        }
    }

    abstract fun set(key: K, value: V?): V?

    abstract fun get(key: K): V?

    protected abstract fun getValue(event: MessageReceivedEvent, args: List<String>, key: K): V?

    protected abstract fun getKey(event: MessageReceivedEvent, args: List<String>): K

    open fun getString(value: V?): String = value?.toString() ?: CLEAR_WORD

    open fun onSuccessfulSet(channel: MessageChannel, old: V?, new: V?)
    {
        channel.sendMessage("Successfully set $displayName to ${getString(new)}").queue()
    }

    open fun onUnsuccessfulSet(channel: MessageChannel, reason: String)
    {
        channel.sendMessage("Could not set $displayName because: $reason").queue()
    }

    open fun displayCurrent(channel: MessageChannel, key: K, value: V?)
    {
        channel.sendMessage("The current $displayName is ${getString(value)}").queue()
    }

    open fun onAlreadySet(channel: MessageChannel, value: V?)
    {
        channel.sendMessage("$displayName is already set to ${getString(value)}").queue()
    }

    companion object
    {
        @JvmStatic
        protected val CLEAR_KEYWORDS = listOf("default", "none", "null")

        protected const val CLEAR_WORD = "`none`"
    }
}