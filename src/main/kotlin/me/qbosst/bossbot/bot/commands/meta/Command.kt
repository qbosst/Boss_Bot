package me.qbosst.bossbot.bot.commands.meta

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

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

    val name = name.replace("\\s+".toRegex(), "")

    private val commands = mutableMapOf<String, Command>()
    private val commandsAlias = mutableMapOf<String, Command>()

    var parent: Command? = null
        private set

    val usage: List<String> = usage.map { str -> "$fullName $str" }
    val examples: List<String> = examples.map { str -> "$fullName $str" }
    val aliases: List<String> = aliases.map { it.replace(" ", "") }

    val botPermissions: List<Permission> = botPermissions.plus(Permission.MESSAGE_WRITE)

    open fun hasPermission(guild: Guild?, user: User): Boolean
    {
        return true
    }

    val fullName: String
        get() {
            val set = mutableSetOf<String>()
            var command: Command? = this
            while(command != null)
            {
                set.add(command.name)
                command = command.parent
            }
            return set.reversed().joinToString(" ")
        }

    val fullUserPermissions: List<Permission> = kotlin.run()
    {
        val set = mutableSetOf<Permission>()
        var command: Command? = this
        while(command != null)
        {
            set.addAll(command.userPermissions)
            command = command.parent
        }
        set.distinct()
    }

    val fullBotPermissions: List<Permission> = kotlin.run()
    {
        val set = mutableSetOf<Permission>()
        var command: Command? = this
        while(command != null)
        {
            set.addAll(command.botPermissions)
            command = command.parent
        }
        set.distinct()
    }

    final override fun addCommand(command: Command)
    {
        command.parent = this
        commands[command.name.toLowerCase()] = command

        for(alias in command.aliases)
        {
            commandsAlias[alias.toLowerCase()] = command
        }
    }

    final override fun getCommand(name: String): Command?
    {
        val label = name.toLowerCase()
        return commands[label] ?: commandsAlias[label]
    }

    final override fun getCommands(): Collection<Command>
    {
        return commands.values
    }

    final override fun addCommands(vararg commands: Command)
    {
        for(command in commands) addCommand(command)
    }

    final override fun addCommands(commands: Collection<Command>)
    {
        for(command in commands) addCommand(command)
    }

    abstract fun execute(event: MessageReceivedEvent, args: List<String>)

    final override fun equals(other: Any?): Boolean
    {
        return if(other is Command)
        {
            fullName == other.fullName || super.equals(other)
        }
        else
        {
            super.equals(other)
        }
    }

    final override fun hashCode(): Int {
        return fullName.hashCode()
    }

    final override fun toString(): String {
        return fullName
    }
}