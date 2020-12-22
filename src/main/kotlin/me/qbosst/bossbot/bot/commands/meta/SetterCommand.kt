package me.qbosst.bossbot.bot.commands.meta

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

abstract class SetterCommand<T>(
        name: String,
        description: String = "none",
        usage: List<String> = listOf(),
        examples: List<String> = listOf(),
        aliases: List<String> = listOf(),
        userPermissions: List<Permission> = listOf(),
        botPermissions: List<Permission> = listOf(),
        val displayName: String

): Command(name, description,
        usage.plus("<${SET_DEFAULT.joinToString("|")}>"), examples.plus(SET_DEFAULT.random()), aliases, true, userPermissions, botPermissions)
{
    final override fun execute(event: MessageReceivedEvent, args: List<String>, flags: Map<String, String?>)
    {
        if(args.isNotEmpty())
        {
            // If the user wants to clear the current value, sets it to null
            if(SET_DEFAULT.firstOrNull { args[0].equals(it, true) } != null)
            {
                val old = set(event.guild, null)
                onSuccessfulSet(event.textChannel, old, null)
            }
            // Sets the new value
            else
            {
                val obj = getFromArguments(event, args) ?: return
                val old = set(event.guild, obj)
                onSuccessfulSet(event.textChannel, old, obj)
            }
        }
        // Displays the current value for that settings
        else
            event.channel.sendMessage(EmbedBuilder()
                    .setTitle(displayName)
                    .setDescription("Current Setting: ${getString(get(event.guild))}")
                    .setColor(event.guild.selfMember.colorRaw)
                    .build()
            ).queue()
    }

    /**
     *  Invoked when a new settings has been set
     *
     *  @param channel The channel to send the message to
     *  @param old The old value
     *  @param new The new value
     */
    private fun onSuccessfulSet(channel: TextChannel, old: T?, new: T?)
    {
        channel.sendMessage(EmbedBuilder()
                .setTitle("Successfully set: $displayName")
                .addField("Old", getString(old), true)
                .addField("New", getString(new), true)
                .setColor(channel.guild.selfMember.colorRaw)
                .build()
        ).queue()
    }

    /**
     *  Invoked when a setting could not be set due to a error
     *
     *  @param channel The channel to send the message to
     *  @param reason The reason why it was unsuccessful
     */
    protected fun onUnSuccessfulSet(channel: TextChannel, reason: String)
    {
        channel.sendMessage(EmbedBuilder()
                .setTitle(displayName)
                .setDescription(reason)
                .setColor(channel.guild.selfMember.colorRaw)
                .build()
        ).queue()
    }

    /**
     *  Gets the object as a string
     *
     *  @param value The object to convert into a string
     *
     *  @return String representation of the object
     */
    abstract fun getString(value: T?): String

    /**
     *  Sets the new settings for a guild
     *
     *  @param guild The guild to set the settings for
     *  @param newValue the new value to set it to
     *
     *  @return The old value
     */
    abstract fun set(guild: Guild, newValue: T?): T?

    /**
     *  Gets the current value
     *
     *  @param guild The guild to get the current value from
     *
     *  @return The current value. Null if the value is null or record for that guild does not exist.
     */
    abstract fun get(guild: Guild): T?

    /**
     *  Tries to get the new value from the arguments provided from the user
     *
     *  @param event The event
     *  @param args The arguments provided for the command
     *
     *  @return The new value. Null if there was an error getting the new value (i.e. user error)
     */
    abstract fun getFromArguments(event: MessageReceivedEvent, args: List<String>): T?

    companion object
    {
        private val SET_DEFAULT = listOf("default", "none", "null")
    }
}