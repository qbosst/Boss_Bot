package me.qbosst.bossbot.bot.commands.meta

interface ICommandManager
{
    fun getCommand(name: String): Command?

    fun getCommands(): Collection<Command>

    fun addCommand(command: Command)

    fun addCommands(vararg commands: Command)

    fun addCommands(commands: Collection<Command>)
}