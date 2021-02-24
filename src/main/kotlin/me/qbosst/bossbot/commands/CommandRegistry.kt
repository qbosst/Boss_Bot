package me.qbosst.bossbot.commands

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.commands.MessageCommand
import com.kotlindiscord.kord.extensions.commands.MessageCommandRegistry
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import dev.kord.core.event.message.MessageCreateEvent

class CommandRegistry(bot: ExtensibleBot): MessageCommandRegistry(bot) {

    val globalChecks: MutableList<suspend (MessageCreateEvent) -> Boolean> = mutableListOf()

    override fun add(command: MessageCommand<out Arguments>) {
        super.add(command)
        command.checkList.addAll(0, globalChecks)
    }

    fun globalCheck(check: suspend (MessageCreateEvent) -> Boolean) {
        globalChecks.add(check)
    }

    fun globalChecks(vararg checks: suspend (MessageCreateEvent) -> Boolean) {
        globalChecks.addAll(checks)
    }

    companion object {
        operator fun invoke(bot: ExtensibleBot, init: CommandRegistry.() -> Unit) = CommandRegistry(bot).apply(init)
    }
}