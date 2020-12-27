package me.qbosst.bossbot.bot.listeners.handlers

import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.util.getPrefix
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.slf4j.LoggerFactory
import java.util.regex.Pattern

class CommandHandler
{
    private val _commands = mutableMapOf<String, Command>()
    private val commandAlias = mutableMapOf<String, Command>()

    /**
     * Immutable map of commands
     */
    val commands: Map<String, Command>
        get() = _commands

    /**
     * Handles a possible command event
     *
     * @param event The event to handle
     *
     * @return Whether the event was a command event or not.
     */
    fun handle(event: MessageReceivedEvent): Boolean
    {
        // don't allow bots to use commands
        if(event.author.isBot) return false

        val content = event.message.contentRaw
        val prefix = event.message.getPrefix()

        // check if message starts with prefix
        if(!(content.startsWith(prefix) && content.length > prefix.length)) return false

        var args = content.substring(prefix.length).split("\\s+".toRegex())
        var command = get(args[0]) ?: return false

        // turn command into sub commands
        var index = 1
        while (args.size > index)
        {
            val new = command[(args[index])] ?: break
            command = new
            index++
        }

        // get the flags provided from arguments
        val flags = mutableMapOf<String, String?>()
                .apply {
                    val toMatch = args.drop(index).joinToString(" ")
                    val matcher = Pattern.compile(Command.FLAG_PATTERN).matcher(toMatch)
                    val sb = StringBuilder()

                    index = 0
                    while (matcher.find())
                    {
                        // puts key and value (nullable) in map
                        put(matcher.group(1), matcher.group(5) ?: matcher.group(4) ?: matcher.group(3))

                        // adds the
                        sb.append(toMatch.substring(index, matcher.start()))
                        index = matcher.end()
                    }
                    if(index < toMatch.length)
                        sb.append(toMatch.substring(index, toMatch.length))

                    // replace the args with the new version with no flags
                    args = sb.split("\\s+".toRegex()).filter { it.isNotBlank() }
                }

        // todo check special permissions

        if(event.isFromGuild)
        {
            //todo check permissions
        }
        else if(command.guildOnly)
        {
            event.channel.sendMessage("This command is a guild-only command!").queue()
            return false
        }

        try
        {
            command.execute(event, args, flags)
        }
        catch (e: Exception)
        {
            log.error("An error has occurred while trying to execute command '${command.fullName}'" +
                    " with the following args '${args}' and the following flags '${flags}'", e)
            event.channel.sendMessage("An error has occurred, the developer has been notified.").queue()
        }
        return true
    }

    operator fun plus(command: Command) = apply {
        _commands[command.label.toLowerCase()] = command

        command.aliases.forEach { alias ->
            commandAlias[alias.toLowerCase()] = command
        }
    }

    operator fun plus(commands: Collection<Command>) = also { commands.forEach { command -> plus(command) } }

    operator fun get(label: String) = let { label.toLowerCase() }
            .let { labelLower -> _commands[labelLower] ?: commandAlias[labelLower] }

    operator fun contains(label: String) = let { label.toLowerCase() }
            .let { labelLower -> _commands.containsKey(labelLower) || commandAlias.containsKey(labelLower) }

    operator fun contains(command: Command) = commands.containsValue(command)

    companion object
    {
        private val log = LoggerFactory.getLogger(this::class.java.simpleName)
    }
}