package me.qbosst.bossbot.bot.commands.meta

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

/**
 *  Abstract class for a command invoked by users
 *
 *  @param label The name of the command
 *  @param description Brief description of what the command does (default = "none")
 *  @param usages Usages of the command (optional)
 *  @param examples Examples of the usage of the command (optional)
 *  @param aliases Different names that can be used to invoke the command (optional)
 *  @param guildOnly Whether the command can only be used in guilds or not (default = true)
 *  @param userPermissions The permissions that the user invoking the command requires (optional)
 *  @param botPermissions The permissions that the self user (bot) will need to execute the command
 */
abstract class Command(val label: String,
                       val description: String = "none",
                       val usages: Collection<String> = listOf(),
                       val examples: Collection<String> = listOf(),
                       val aliases: Collection<String> = listOf(),
                       val guildOnly: Boolean = true,
                       val developerOnly: Boolean = false,
                       val userPermissions: Collection<Permission> = listOf(),
                       val botPermissions: Collection<Permission> = listOf(),
                       val children: Collection<Command> = listOf()
)
{
    var parent: Command? = null
        private set

    init {
        children.forEach { child -> child.parent = this }
    }

    /**
     *  Gets the full name of a command.
     */
    val fullName: String
        get() = mutableListOf<String>().apply { forEachParent { cmd -> add(cmd.label) } }.reversed().joinToString(" ")

    /**
     *  Gets the full user permissions required of a command.
     */
    val fullUserPermissions: List<Permission>
        get() = mutableListOf<Permission>().apply { forEachParent { cmd -> addAll(cmd.userPermissions) } }.distinct()

    /**
     *  Gets the full self user (bot) permissions required of a command.
     */
    val fullBotPermissions: List<Permission>
        get() = mutableListOf<Permission>().apply { forEachParent { cmd -> addAll(cmd.botPermissions) } }.distinct()

    operator fun get(label: String): Command? = children.firstOrNull { child -> child.label.equals(label, true) ||
            child.aliases.any { alias -> alias.equals(label, true) } }

    private fun forEachParent(consumer: (Command) -> Unit)
    {
        var parent: Command? = this
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
    abstract fun execute(event: MessageReceivedEvent, args: List<String>, flags: Map<String, String?>)

    companion object
    {
        const val FLAG_PATTERN = "--([a-zA-Z]+)(=([^\"'\\s]+|\"((?:[^\"\\\\]|\\\\.)*)\"|'((?:[^'\\\\]|\\\\.)*)'))?"
    }
}