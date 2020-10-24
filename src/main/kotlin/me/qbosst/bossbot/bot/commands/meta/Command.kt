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
 *  @param usage_raw Usages of the command (optional)
 *  @param examples_raw Examples of the usage of the command (optional)
 *  @param aliases_raw Different names that can be used to invoke the command (optional)
 *  @param guildOnly Whether the command can only be used in guilds or not (default = true)
 *  @param userPermissions The permissions that the user invoking the command requires (optional)
 *  @param botPermissions The permissions that the self user (bot) will need to execute the command
 */
abstract class Command (
        name: String,
        val description: String = "none",
        val usage_raw: List<String> = listOf(),
        val examples_raw: List<String> = listOf(),
        val aliases_raw: List<String> = listOf(),
        val guildOnly: Boolean = true,
        private val userPermissions: List<Permission> = listOf(),
        botPermissions: List<Permission> = listOf()
): CommandManagerImpl()
{
    val name = name.replace(Regex("\\s+"), "")

    val usage: List<String>
        get() = usage_raw.map { str -> "$fullName $str" }

    val examples: List<String>
        get() = examples_raw.map { str -> "$fullName $str" }

    val aliases: List<String>
        get() = aliases_raw.map { it.replace(Regex("\\s+"), "") }

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
    open fun hasPermission(guild: Guild?, user: User): Boolean = true

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
        if(other !is Command)
            return false
        return other.name == name &&
                other.description == description &&
                other.usage_raw == usage_raw &&
                other.examples_raw == examples_raw &&
                other.aliases_raw == aliases_raw &&
                other.guildOnly == guildOnly &&
                other.userPermissions == userPermissions &&
                other.botPermissions == botPermissions &&
                other.parent == parent
    }

    final override fun hashCode(): Int
    {
        var result = name.hashCode()
        result = 31 * result + usage_raw.hashCode()
        result = 31 * result + examples_raw.hashCode()
        result = 31 * result + aliases_raw.hashCode()
        result = 31 * result + guildOnly.hashCode()
        result = 31 * result + userPermissions.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + botPermissions.hashCode()
        result = 31 * result + (parent?.hashCode() ?: 0)
        return result
    }
}