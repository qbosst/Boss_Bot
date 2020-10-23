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
): CommandManagerImpl()
{
    val name = name.replace(Regex("\\s+"), "")

    val usage: List<String> = usage.map { str -> "$fullName $str" }
    val examples: List<String> = examples.map { str -> "$fullName $str" }
    val aliases: List<String> = aliases.map { it.replace(Regex("\\s+"), "") }

    val botPermissions: List<Permission> = botPermissions.plus(Permission.MESSAGE_WRITE)

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
            forEachParent(this) { set.add(it.name) }
            return set.reversed().joinToString(" ")
        }

    /**
     *  Gets the full user permissions required of a command.
     */
    val fullUserPermissions: List<Permission>
        get() {
            val permissions = mutableListOf<Permission>()
            forEachParent(this) { permissions.addAll(it.userPermissions) }
            return permissions
        }

    /**
     *  Gets the full self user (bot) permissions required of a command.
     */
    val fullBotPermissions: List<Permission>
        get() {
            val permissions = mutableListOf<Permission>()
            forEachParent(this) { permissions.addAll(it.botPermissions) }
            return permissions
        }

    private fun forEachParent(command: Command, consumer: (Command) -> Unit)
    {
        var parent: Command? = command
        while (parent != null)
        {
            consumer.invoke(parent)
            parent = parent.parent
        }
    }

    /**
     *  Method used to execute a command
     *
     *  @param event The event that the command was received from
     *  @param args The arguments from the user for the command
     */
    abstract fun execute(event: MessageReceivedEvent, args: List<String>)

    final override fun addCommand(command: Command): CommandManagerImpl
    {
        command.parent = this
        return super.addCommand(command)
    }

    final override fun addCommands(commands: Collection<Command>): CommandManagerImpl = super.addCommands(commands)

    final override fun getCommand(name: String): Command? = super.getCommand(name)

    final override fun getCommands(): Collection<Command> = super.getCommands()

    final override fun equals(other: Any?): Boolean
    {
        return (other is Command) && (fullName == other.fullName)
    }

    final override fun hashCode(): Int = fullName.hashCode() * 7

    final override fun toString(): String = "Command($fullName|$description)"
}