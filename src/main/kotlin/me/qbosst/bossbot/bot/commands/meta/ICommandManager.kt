package me.qbosst.bossbot.bot.commands.meta

interface ICommandManager
{
    /**
     *  Gets a command based on the name
     *  @param name The name of the command
     *
     *  @return Command instance. Null if no command was found with that name
     */
    fun getCommand(name: String): Command?

    /**
     *  Gets all the commands
     *
     *  @return All a collection of non-null commands.
     */
    fun getCommands(): Collection<Command>

    /**
     *  Adds a command
     *
     *  @param command The command to add
     *
     *  @return Command Manager
     */
    fun addCommand(command: Command): ICommandManager

    /**
     *  Adds multiple commands at once
     *
     *  @param commands The collection of commands to add.
     *
     *  @return Command Manager
     */
    fun addCommands(commands: Collection<Command>): ICommandManager
}