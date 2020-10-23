package me.qbosst.bossbot.bot.commands.meta

/**
 *  Class to organise commands
 */
open class CommandManagerImpl: ICommandManager
{
    private val commands = mutableMapOf<String, Command>()

    override fun addCommand(command: Command): CommandManagerImpl
    {
        commands[command.name.toLowerCase()] = command
        for(alias in command.aliases)
            commands[alias.toLowerCase()] = command
        return this
    }

    override fun addCommands(commands: Collection<Command>): CommandManagerImpl
    {
        for(command in commands)
            addCommand(command)
        return this
    }

    override fun getCommand(name: String): Command? = commands[name.toLowerCase()]

    override fun getCommands(): Collection<Command> = commands.values
}