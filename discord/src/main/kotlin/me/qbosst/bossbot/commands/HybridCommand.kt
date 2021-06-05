package me.qbosst.bossbot.commands

import com.kotlindiscord.kord.extensions.commands.Command
import com.kotlindiscord.kord.extensions.commands.MessageCommand
import com.kotlindiscord.kord.extensions.commands.parser.ArgumentParser
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.AutoAckType
import com.kotlindiscord.kord.extensions.commands.slash.SlashCommand
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.entity.Permission
import dev.kord.core.Kord
import dev.kord.core.event.Event
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

open class HybridCommand<T: Arguments>(
    extension: Extension,
    open val arguments :(() -> T)? = null
): Command(extension), KoinComponent {

    /** Kord instance, backing the ExtensibleBot. **/
    public val kord: Kord by inject()

    /**
     * @suppress
     */
    public open lateinit var body: suspend HybridCommandContext<out T>.() -> Unit

    /** Command description, as displayed on Discord. **/
    public open lateinit var description: String

    /**
     * @suppress
     */
    public open val checkList: MutableList<suspend (Event) -> Boolean> = mutableListOf()

    override val parser: ArgumentParser = ArgumentParser()

    /** Permissions required to be able to run this command. **/
    public open val requiredPerms: MutableSet<Permission> = mutableSetOf()

    /** If your bot requires permissions to be able to execute the command, add them using this function. **/
    public fun requirePermissions(vararg perms: Permission) {
        perms.forEach { requiredPerms.add(it) }
    }

    /**
     * Define what will happen when your command is invoked.
     *
     * @param action The body of your command, which will be executed when your command is invoked.
     */
    public open fun action(action: suspend HybridCommandContext<out T>.() -> Unit) {
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
    public open fun check(vararg checks: suspend (Event) -> Boolean) {
        checks.forEach { checkList.add(it) }
    }

    /**
     * Overloaded check function to allow for DSL syntax.
     *
     * @param check Check to apply to this command.
     */
    public open fun check(check: suspend (Event) -> Boolean) {
        checkList.add(check)
    }

    public open fun toMessageCommand(): MessageCommand<T> {
        val commandObj = MessageCommand(extension, arguments)

        commandObj.apply {
            name = this@HybridCommand.name
            description = this@HybridCommand.description

            requiredPerms.addAll(this@HybridCommand.requiredPerms)
            checkList.addAll(this@HybridCommand.checkList)

            action {
                val context = HybridCommandContext<T>(this)

                this@HybridCommand.body.invoke(context)
            }
        }

        return commandObj
    }

    public open fun toSlashCommand(): SlashCommand<T> {
        val commandObj = SlashCommand(extension, arguments)

        commandObj.apply {
            name = this@HybridCommand.name
            description = this@HybridCommand.description
            autoAck = AutoAckType.NONE

            requiredPerms.addAll(this@HybridCommand.requiredPerms)
            checkList.addAll(this@HybridCommand.checkList)

            action {
                val context = HybridCommandContext<T>(this)

                this@HybridCommand.body.invoke(context)
            }
        }

        return commandObj
    }
}