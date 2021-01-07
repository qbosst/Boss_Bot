

package me.qbosst.bossbot.listeners.handlers

import me.qbosst.bossbot.BossBot
import me.qbosst.bossbot.database.manager.settings
import me.qbosst.jda.ext.commands.annotations.CommandFunction
import me.qbosst.jda.ext.commands.argument.ArgumentParser
import me.qbosst.jda.ext.commands.entities.*
import me.qbosst.jda.ext.commands.exceptions.BadArgument
import me.qbosst.jda.ext.commands.impl.DefaultCommandClientBuilder
import me.qbosst.jda.ext.commands.parsers.Parser
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.KParameter
import me.qbosst.jda.ext.commands.entities.CommandClient as ICommandClient

/**
 * Handles [Command] related functions
 *
 * @param commands List of [commands] to register to the [CommandClient]
 * @param developerIds List of [net.dv8tion.jda.api.entities.User] ids of the developers of Boss Bot
 */
@OptIn(ExperimentalStdlibApi::class)
class CommandClient(
    commands: Collection<Command>,
    override val developerIds: Collection<Long>
): ICommandClient, CommandEventListener {
    private val _commands: MutableMap<String, Command> = mutableMapOf()
    private val commandAliases: MutableMap<String, Command> = mutableMapOf()

    private val _stats: MutableMap<Command, Int> = mutableMapOf()

    /**
     * Contains a map of command usages, will be used to see what commands are popular
     */
    val stats: Map<Command, Int>
        get() = _stats

    init {
        // register commands
        commands.forEach { command ->
            _commands[command.label.toLowerCase()] = command
            command.aliases.forEach { alias ->
                commandAliases[alias.toLowerCase()] = command
            }
        }
    }

    override val commands: Map<String, Command>
        get() = _commands

    override val listeners: Collection<CommandEventListener> = listOf(this)

    override val prefixProvider: PrefixProvider = PrefixProviderImpl()

    /**
     * @return Whether the event was a command event or not, if this is a command event but fails a check,
     * like [Command.guildOnly] it will still return true.
     */
    suspend fun handle(event: MessageReceivedEvent): Boolean {
        // do not allow other bots to use commands
        if(event.author.isBot || event.isWebhookMessage) {
            dispatch { onNonCommandEvent(event) }
            return false
        }

        val message = event.message
        val prefixes = prefixProvider.provide(message)
        val content = message.contentRaw

        // gets the prefix and makes sure that the message length is longer than the prefix length
        val prefix = prefixes.firstOrNull { prefix -> content.startsWith(prefix) && content.length > prefix.length }
        if(prefix == null) {
            dispatch { onNonCommandEvent(event) }
            return false
        }

        val args = content.substring(prefix.length).split("\\s+".toRegex()).filter { !(it.isEmpty() || it.isBlank()) }
        val label = args[0]
        // returns final command and args to drop
        val (command, drop) = this[label]
                // get sub commands of this command (if parent is not null)
            ?.let { parent ->
                var index = 1
                var command: Command = parent
                while (index < args.size && command.children.isNotEmpty()) {
                    val child = command[args[index]] ?: break
                    command = child
                    index++
                }

                return@let Pair(command, index)
            }
            ?: kotlin.run {
                dispatch { onUnknownCommand(event, label, args) }
                return false
            }

        // update usages, keeps track of most frequently used commands
        val old = stats[command] ?: 0
        _stats[command] = old+1

        val ctx = Context(this, event, command)

        // command checks
        if(command.developerOnly && !developerIds.contains(ctx.author.idLong)) {
            dispatch { onCommandDeveloperOnly(ctx, command) }
            return true
        }

        if(message.isFromGuild) {
            if(command.userPermissions.isNotEmpty()) {
                val member = event.member!!
                val missingPermissions = command.userPermissions
                    .filterNot { permission -> member.hasPermission(event.textChannel, permission) }

                if(missingPermissions.isNotEmpty()) {
                    dispatch { onUserMissingPermissions(ctx, command, missingPermissions) }
                    return true
                }
            }

            if(command.botPermissions.isNotEmpty()) {
                val member = event.guild.selfMember
                val missingPermissions = command.botPermissions
                    .filterNot { permission -> member.hasPermission(event.textChannel, permission) }

                if(missingPermissions.isNotEmpty()) {
                    dispatch { onBotMissingPermissions(ctx, command, missingPermissions) }
                    return true
                }
            }
        }
        else if(command.guildOnly) {
            dispatch { onCommandGuildOnly(ctx, command) }
            return true
        }

        // parse command arguments
        val (method: CommandExecutable, arguments: HashMap<KParameter, Any?>) = kotlin.runCatching {
            // new args to work with, we drop the old arguments that were used to identify the command
            val arguments = args.drop(drop)
            when(command.methods.size) {
                0 -> throw IllegalArgumentException(buildString {
                    append(command::class.simpleName)
                    append(" must have at least 1 ")
                    append(CommandFunction::class.simpleName)
                })
                1 -> {
                    val method = command.methods.first()

                    return@runCatching kotlin.runCatching { parseArgs(method, ctx, arguments) }
                        .getOrElse { error ->
                            when(error) {
                                is BadArgument -> dispatch { onBadArgument(ctx, method, error) }
                                else -> dispatch { onParseError(ctx, method, error) }
                            }
                            return true
                    }
                }
                else -> {
                    // map of all errors that occurred while trying to parse,
                    val errors = buildMap<CommandExecutable, Throwable> {
                        command.methods
                            .map { method ->
                                kotlin.runCatching { parseArgs(method, ctx, arguments) }
                                    .onFailure { error -> put(method, error) }
                                        // if a successful parse, return this
                                    .onSuccess { success -> return@runCatching success }
                            }
                    }

                    // there were no successful parses, so we need to decide how to dispatch the errors
                    when {
                        errors.any { (_, error) -> error !is BadArgument } ->
                            dispatch { onMultipleParsingErrors(ctx, errors) }
                        else -> {
                            // filter errors to the ones with the highest argument index, for example,
                            // execute(ctx: CommandContext, isTrue: Boolean, user: User)
                            // execute(ctx: CommandContext, number: Int)
                            // if the user enters `isTrue` correctly, but `user` incorrectly,
                            // this should throw a BadArgument exception for that method instead of both of them

                            @Suppress("UNCHECKED_CAST")
                            val badArguments = (errors as Map<CommandExecutable, BadArgument>)
                                .run {
                                    val highest = maxOf { (_, error) -> error.expected.index }
                                    filter { (_, error) -> error.expected.index == highest }
                                }

                            if(badArguments.size == 1) {
                                val (method, error) = badArguments.entries.first()
                                dispatch { onBadArgument(ctx, method, error) }
                            }
                            else {
                                dispatch { onMultipleBadArguments(ctx, badArguments) }
                            }
                        }
                    }
                    return true
                }
            }
        }.getOrElse { error ->
            dispatch { onInternalError(error) }
            return true
        }

        // execute the method
        method.execute(ctx, arguments) { success, error ->
            error?.let { dispatch { onCommandError(ctx, method, error) } }
            dispatch { onCommandPostInvoke(ctx, method, !success) }
        }

        return true
    }

    /**
     * Checks if this [CommandClient] contains a [Command]
     */
    operator fun contains(label: String): Boolean = label.toLowerCase()
        .run { commands.containsKey(this) || commandAliases.containsKey(this) }

    /**
     * Gets a [Command] based on [label].
     * This will return null if no commands [Command.label] or [Command.aliases] is [label].
     */
    operator fun get(label: String): Command? = label.toLowerCase()
        .run { commands[this] ?: commandAliases[this] }

    private suspend fun dispatch(invoker: suspend CommandEventListener.() -> Unit) {
        listeners.forEach { listener ->
            try {
                invoker.invoke(listener)
            }
            catch (t: Throwable) {
                listeners.forEach { listenerInner ->
                    try {
                        listenerInner.onInternalError(t)
                    } catch (inner: Throwable) {
                        LOG.error("An uncaught exception has occurred during event dispatch", inner)
                    }
                }
            }
        }
    }

    private suspend fun parseArgs(
        method: CommandExecutable,
        ctx: Context,
        args: List<String>,
    ): Pair<CommandExecutable, HashMap<KParameter, Any?>> {
        return Pair(method, ArgumentParser.parseArguments(method, ctx, args, method.properties.delimiter))
    }

    private class PrefixProviderImpl: PrefixProvider {

        override fun provide(message: Message): Collection<String> = buildList {
            // bot mention
            add("<@${message.jda.selfUser.id}>")
            add("<@!${message.jda.selfUser.id}>")

            val guild = if(message.isFromGuild) message.guild else null
            val prefix = guild?.settings?.prefix ?: BossBot.config.defaultPrefix
            add(prefix)
        }
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(CommandClient::class.java)
    }

    override fun onBadArgument(ctx: Context, executable: CommandExecutable, error: BadArgument) {
        val isEmpty = error.provided.isEmpty() || error.provided.none { it.isNotBlank() || it.isNotEmpty() }

        if(isEmpty) {
            ctx.messageChannel.sendMessage("Please provider ${error.expected.name}").queue()
        } else {
            ctx.messageChannel.sendMessage("Bad Argument...").queue()
        }
    }

    class Builder {
        var commands: Collection<Command> = listOf()
        var developerIds: Collection<Long> = listOf()

        fun build(): CommandClient = CommandClient(commands, developerIds)

        inline fun <reified T> registerParser(parser: Parser<T>) {
            ArgumentParser.parsers[T::class.java] = parser
        }

        fun registerDefaultParsers() = also { DefaultCommandClientBuilder().registerDefaultParsers() }
    }
}

fun commandClient(init: CommandClient.Builder.() -> Unit): CommandClient {
    val builder = CommandClient.Builder()
    builder.init()
    return builder.build()
}