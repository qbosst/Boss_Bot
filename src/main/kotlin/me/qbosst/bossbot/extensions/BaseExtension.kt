package me.qbosst.bossbot.extensions

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ExtensionState
import dev.kord.core.event.message.MessageCreateEvent

abstract class BaseExtension(bot: ExtensibleBot): Extension(bot) {

    val localChecks: MutableList<suspend (MessageCreateEvent) -> Boolean> = mutableListOf()

    /**
     * Local command checks that will be applied to all Message Commands in this extension
     */
    fun commandCheck(check: suspend (MessageCreateEvent) -> Boolean) {
        localChecks.add(check)
    }

    /**
     * Local command checks that will be applied to all Message Commands in this extension
     */
    fun commandChecks(vararg checks: suspend (MessageCreateEvent) -> Boolean) {
        localChecks.addAll(checks)
    }

    override suspend fun doSetup() {
        this.setState(ExtensionState.LOADING)

        @Suppress("TooGenericExceptionCaught")
        try {
            this.setup()
            // add local command checks to all commands in this extension
            commands.forEach { command ->
                command.checkList.addAll(localChecks)
            }

        } catch (t: Throwable) {
            this.setState(ExtensionState.FAILED_LOADING)
            throw t
        }

        this.setState(ExtensionState.LOADED)
    }
}