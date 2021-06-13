package me.qbosst.bossbot.commands

import com.kotlindiscord.kord.extensions.builders.ExtensibleBotBuilder
import com.kotlindiscord.kord.extensions.commands.GroupCommand
import com.kotlindiscord.kord.extensions.commands.MessageCommand
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.AutoAckType
import com.kotlindiscord.kord.extensions.commands.slash.SlashCommand
import com.kotlindiscord.kord.extensions.commands.slash.SlashGroup
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.entity.Permission
import dev.kord.core.Kord
import dev.kord.core.event.Event
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

open class HybridCommand<T: Arguments>(
    val extension: Extension,
    val arguments: (() -> T)? = null,
    val parentCommand: HybridCommand<out Arguments>? = null,
    val parentGroup: HybridGroupCommand<out Arguments>? = null
): KoinComponent {
    inner class SlashSettings {
        var autoAck: AutoAckType = AutoAckType.NONE
    }

    inner class MessageSettings {
        var enabled: Boolean = true
        var hidden: Boolean = false
        var aliases: Array<String> = arrayOf()
    }

    val settings: ExtensibleBotBuilder by inject()
    val kord: Kord by inject()

    val slashSettings: SlashSettings = SlashSettings()
    val messageSettings: MessageSettings = MessageSettings()

    lateinit var name: String
    lateinit var description: String

    lateinit var body: suspend HybridCommandContext<out T>.() -> Unit

    val checkList: MutableList<suspend (Event) -> Boolean> = mutableListOf()
    val requiredPerms: MutableSet<Permission> = mutableSetOf()
    val commands: MutableList<HybridCommand<out Arguments>> = mutableListOf()
    val groups: MutableList<HybridGroupCommand<out Arguments>> = mutableListOf()

    fun slashSettings(init: SlashSettings.() -> Unit) {
        slashSettings.apply(init)
    }

    fun messageSettings(init: MessageSettings.() -> Unit) {
        messageSettings.apply(init)
    }

    /**
     * Define what will happen when your command is invoked.
     *
     * @param action The body of your command, which will be executed when your command is invoked.
     */
    fun action(action: suspend HybridCommandContext<out T>.() -> Unit) {
        this.body = action
    }

    /**
     * Define a check which must pass for the command to be executed.
     *
     * A command may have multiple checks - all checks must pass for the command to be executed.
     * Checks will be run in the order that they're defined.
     *
     * This function can be used DSL-style with a given body, or it can be passed one or more
     * predefined functions. See the samples for more information.
     *
     * @param checks Checks to apply to this command.
     */
    fun check(vararg checks: suspend (Event) -> Boolean) {
        checkList += checks
    }

    /**
     * Overloaded check function to allow for DSL syntax.
     *
     * @param check Check to apply to this command.
     */
    fun check(check: suspend (Event) -> Boolean) {
        checkList += check
    }

    /** If your bot requires permissions to be able to execute the command, add them using this function. **/
    fun requirePermissions(vararg perms: Permission) {
        requiredPerms += perms
    }

    suspend fun <R: Arguments> group(
        arguments: (() -> R)?,
        body: suspend HybridGroupCommand<R>.() -> Unit
    ): HybridGroupCommand<R> {
        val commandObj = HybridGroupCommand(extension, arguments, parentCommand = this)
        body.invoke(commandObj)

        return group(commandObj)
    }

    suspend fun group(body: suspend HybridGroupCommand<Arguments>.() -> Unit): HybridGroupCommand<Arguments> {
        val commandObj = HybridGroupCommand<Arguments>(extension, null, parentCommand = this)
        body.invoke(commandObj)

        return group(commandObj)
    }

    suspend fun <R: Arguments> group(commandObj: HybridGroupCommand<R>): HybridGroupCommand<R> {
        groups.add(commandObj)
        return commandObj
    }

    suspend fun <R: Arguments> subCommand(
        arguments: (() -> R)?,
        body: suspend HybridCommand<R>.() -> Unit
    ): HybridCommand<R> {
        val commandObj = HybridCommand(extension, arguments, parentCommand = this)
        body.invoke(commandObj)

        return subCommand(commandObj)
    }

    suspend fun subCommand(body: suspend HybridCommand<Arguments>.() -> Unit): HybridCommand<Arguments> {
        val commandObj = HybridCommand<Arguments>(extension, null, parentCommand = this)
        body.invoke(commandObj)

        return subCommand(commandObj)
    }

    suspend fun <R: Arguments> subCommand(commandObj: HybridCommand<R>): HybridCommand<R> {
        commands.add(commandObj)
        return commandObj
    }

    fun toSlashCommand(
        parentCommand: SlashCommand<out Arguments>? = null,
        parentGroup: SlashGroup? = null
    ): SlashCommand<T> = SlashCommand(extension, arguments, parentCommand, parentGroup).apply {
        name = this@HybridCommand.name
        description = this@HybridCommand.description
        checkList += this@HybridCommand.checkList
        requiredPerms += this@HybridCommand.requiredPerms
        autoAck = this@HybridCommand.slashSettings.autoAck

        when {
            this@HybridCommand.groups.isNotEmpty() -> {
                groups.putAll(this@HybridCommand.groups.map { it.name to it.toSlashGroup(this) })
            }
            this@HybridCommand.commands.isNotEmpty() -> {
                subCommands.addAll(this@HybridCommand.commands.map { it.toSlashCommand(parentCommand = this) })
            }
            else -> {
                action { this@HybridCommand.body.invoke(HybridCommandContext(this)) }
            }
        }
    }

    fun toMessageCommand(): MessageCommand<T> {
        val commandObj = if(commands.isNotEmpty() || groups.isNotEmpty()) {
            GroupCommand(extension, arguments).apply {
                commands.addAll(this@HybridCommand.commands.map(HybridCommand<*>::toMessageCommand))
                commands.addAll(this@HybridCommand.groups.map(HybridGroupCommand<*>::toMessageCommand))
                if(!this@HybridCommand::body.isInitialized) {
                    action { sendHelp() }
                } else {
                    action { this@HybridCommand.body.invoke(HybridCommandContext(this)) }
                }
            }
        } else {
            MessageCommand(extension, arguments).apply {
                action { this@HybridCommand.body.invoke(HybridCommandContext(this)) }
            }
        }

        return commandObj.apply {
            name = this@HybridCommand.name
            description = this@HybridCommand.description
            enabled = this@HybridCommand.messageSettings.enabled
            hidden = this@HybridCommand.messageSettings.hidden
            aliases = this@HybridCommand.messageSettings.aliases
            checkList += this@HybridCommand.checkList
            requiredPerms += this@HybridCommand.requiredPerms
        }
    }
}