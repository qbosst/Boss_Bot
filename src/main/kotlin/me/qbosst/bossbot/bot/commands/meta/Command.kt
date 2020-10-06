package me.qbosst.bossbot.bot.commands.meta

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

/**
 *  Abstract class for a command invoked by users
 *
 *  @param name The name of the command
 *  @param description Brief description of what the command does (default = "none")
 *  @param usage Usages of the command (optional)
 *  @param examples Examples of the usage of the command (optional)
 *  @param aliases Different names that can be used to invoke the command (optional)
 *  @param guildOnly Whether the command can only be used in guilds or not (default = true)
 *  @param userPermissions The permissions that the user invoking the command requires (optional)
 *  @param botPermissions The permissions that the self user (bot) will need to execute the command
 */
abstract class Command (
        name: String,
        val description: String = "none",
        usage: List<String> = listOf(),
        examples: List<String> = listOf(),
        aliases: List<String> = listOf(),
        val guildOnly: Boolean = true,
        private val userPermissions: List<Permission> = listOf(),
        botPermissions: List<Permission> = listOf()
): ICommandManager
{
    val name = name.replace(Regex("\\s+"), "")

    val usage: List<String> = usage.map { str -> "$fullName $str" }
    val examples: List<String> = examples.map { str -> "$fullName $str" }
    val aliases: List<String> = aliases.map { it.replace(Regex("\\s+"), "") }

    val botPermissions: List<Permission> = botPermissions.plus(Permission.MESSAGE_WRITE)

    private val commands = mutableMapOf<String, Command>()
    var parent: Command? = null
        private set

    /**
     *  Checks for additional permission that a user may require to use the command (i.e. be a developer).
     *  By default will return true
     *
     *  @param guild Guild of the user that has invoked the command. Null if the command was not invoked in a guild
     *  @param user The user that is trying to invoke the command
     *
     *  @return Whether the user has permission or not
     */
    open fun hasPermission(guild: Guild?, user: User): Boolean
    {
        return true
    }

    /**
     *  Gets the full name of a command.
     */
    val fullName: String
        get() {
            val set = mutableSetOf<String>()
            var command: Command? = this

            while(command != null)
            {
                // Adds the current command's name to the list and sets the new command to the current command's parent
                set.add(command.name)
                command = command.parent
            }
            return set.reversed().joinToString(" ")
        }

    /**
     *  Gets the full user permissions required of a command.
     */
    val fullUserPermissions: List<Permission>
        get() {
            val permissions = mutableListOf<Permission>()
            var command: Command? = this
            while(command != null)
            {
                // Adds the current command's permissions to the list and sets the new command to the current command's parent
                permissions.addAll(command.userPermissions)
                command = command.parent
            }
            return permissions
        }

    /**
     *  Gets the full self user (bot) permissions required of a command.
     */
    val fullBotPermissions: List<Permission>
        get() {
            val permissions = mutableListOf<Permission>()
            var command: Command? = this
            while(command != null)
            {
                // Adds the current command's permissions to the list and sets the new command to the current command's parent
                permissions.addAll(command.botPermissions)
                command = command.parent
            }
            return permissions
        }

    /**
     *  Method used to execute a command
     *
     *  @param event The event that the command was received from
     *  @param args The arguments from the user for the command
     */
    abstract fun execute(event: MessageReceivedEvent, args: List<String>)

    final override fun addCommand(command: Command)
    {
        command.parent = this
        commands[command.name.toLowerCase()] = command

        for(alias in command.aliases)
            commands[alias.toLowerCase()] = command
    }

    final override fun getCommand(name: String): Command?
    {
        val label = name.toLowerCase()
        return commands[label] ?: commands[label]
    }

    final override fun getCommands(): Collection<Command>
    {
        return commands.values.distinct()
    }

    final override fun addCommands(commands: Collection<Command>)
    {
        for(command in commands) addCommand(command)
    }

    final override fun equals(other: Any?): Boolean
    {
        return (other is Command) && (fullName == other.fullName)
    }

    final override fun hashCode(): Int
    {
        return fullName.hashCode() * 7
    }

    final override fun toString(): String
    {
        return "Command(${fullName})"
    }
}