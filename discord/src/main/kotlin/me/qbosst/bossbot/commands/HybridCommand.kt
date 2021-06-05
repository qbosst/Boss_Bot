package me.qbosst.bossbot.commands

import com.kotlindiscord.kord.extensions.builders.ExtensibleBotBuilder
import com.kotlindiscord.kord.extensions.commands.Command
import com.kotlindiscord.kord.extensions.commands.MessageCommand
import com.kotlindiscord.kord.extensions.commands.parser.ArgumentParser
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.commands.slash.AutoAckType
import com.kotlindiscord.kord.extensions.commands.slash.SlashCommand
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.event.Event
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

open class HybridCommand<T: Arguments>(
    extension: Extension,
    open val arguments :(() -> T)? = null
): Command(extension), KoinComponent {

    private val settings: ExtensibleBotBuilder by inject()

    /** Kord instance, backing the ExtensibleBot. **/
    val kord: Kord by inject()

    open inner class SlashSettings {
        /** Type of automatic ack to use, if any. **/
        open var autoAck: AutoAckType = AutoAckType.NONE


        /** Guild ID this slash command is to be registered for, if any. **/
        open var guild: Snowflake? = settings.slashCommandsBuilder.defaultGuild

        /** Specify a specific guild for this slash command. **/
        open fun guild(guild: Snowflake) {
            this.guild = guild
        }

        /** Specify a specific guild for this slash command. **/
        open fun guild(guild: Long) {
            this.guild = Snowflake(guild)
        }

        /** Specify a specific guild for this slash command. **/
        open fun guild(guild: GuildBehavior) {
            this.guild = guild.id
        }
    }

    open inner class MessageSettings {
        /**
         * Whether this command is enabled and can be invoked.
         *
         * Disabled commands cannot be invoked, and won't be shown in help commands.
         *
         * This can be changed at runtime, if commands need to be enabled and disabled dynamically without being
         * reconstructed.
         */
        open var enabled: Boolean = true


        /**
         * Whether to hide this command from help command listings.
         *
         * By default, this is `false` - so the command will be shown.
         */
        open var hidden: Boolean = false


        /**
         * Alternative names that can be used to invoke your command.
         *
         * There's no limit on the number of aliases a command may have, but in the event of an alias matching
         * the [name] of a registered command, the command with the [name] takes priority.
         */
        open var aliases: Array<String> = arrayOf()
    }

    val slashSettings: SlashSettings = SlashSettings()

    val messageSettings: MessageSettings = MessageSettings()

    /**
     * @suppress
     */
    open lateinit var body: suspend HybridCommandContext<out T>.() -> Unit

    /** Command description, as displayed on Discord. **/
    open lateinit var description: String

    /**
     * @suppress
     */
    open val checkList: MutableList<suspend (Event) -> Boolean> = mutableListOf()

    override val parser: ArgumentParser = ArgumentParser()

    /** Permissions required to be able to run this command. **/
    open val requiredPerms: MutableSet<Permission> = mutableSetOf()

    fun slashSettings(init: SlashSettings.() -> Unit) {
        slashSettings.init()
    }

    fun messageSettings(init: MessageSettings.() -> Unit) {
        messageSettings.init()
    }

    /** If your bot requires permissions to be able to execute the command, add them using this function. **/
    fun requirePermissions(vararg perms: Permission) {
        perms.forEach { requiredPerms.add(it) }
    }

    /**
     * Define what will happen when your command is invoked.
     *
     * @param action The body of your command, which will be executed when your command is invoked.
     */
    open fun action(action: suspend HybridCommandContext<out T>.() -> Unit) {
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
    open fun check(vararg checks: suspend (Event) -> Boolean) {
        checks.forEach { checkList.add(it) }
    }

    /**
     * Overloaded check function to allow for DSL syntax.
     *
     * @param check Check to apply to this command.
     */
    open fun check(check: suspend (Event) -> Boolean) {
        checkList.add(check)
    }

    open fun toMessageCommand(): MessageCommand<T> {
        val commandObj = MessageCommand(extension, arguments)
        val settings = messageSettings

        commandObj.apply {
            name = this@HybridCommand.name
            description = this@HybridCommand.description

            requiredPerms.addAll(this@HybridCommand.requiredPerms)
            checkList.addAll(this@HybridCommand.checkList)

            enabled = settings.enabled
            hidden = settings.hidden
            aliases = settings.aliases

            action {
                val context = HybridCommandContext<T>(this)

                this@HybridCommand.body.invoke(context)
            }
        }

        return commandObj
    }

    open fun toSlashCommand(): SlashCommand<T> {
        val commandObj = SlashCommand(extension, arguments)
        val settings = slashSettings

        commandObj.apply {
            name = this@HybridCommand.name
            description = this@HybridCommand.description

            requiredPerms.addAll(this@HybridCommand.requiredPerms)
            checkList.addAll(this@HybridCommand.checkList)

            autoAck = settings.autoAck
            guild = settings.guild

            action {
                val context = HybridCommandContext<T>(this)

                this@HybridCommand.body.invoke(context)
            }
        }

        return commandObj
    }
}