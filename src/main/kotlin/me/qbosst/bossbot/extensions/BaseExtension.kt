package me.qbosst.bossbot.extensions

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.commands.GroupCommand
import com.kotlindiscord.kord.extensions.commands.MessageCommand
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension

abstract class BaseExtension(bot: ExtensibleBot): Extension(bot) {

    /**
     * Creates a [MessageCommand] object but does not register it.
     * This does not guarantee that the command is valid
     */
    suspend fun <T: Arguments> createCommand(
        arguments: (() -> T)?,
        body: suspend MessageCommand<T>.() -> Unit
    ): MessageCommand<T> {
        val commandObj = MessageCommand(this, arguments)
        body.invoke(commandObj)

        return commandObj
    }

    /**
     * Creates a [MessageCommand] object but does not register it.
     * This does not guarantee that the command is valid
     */
    suspend fun createCommand(
        body: suspend MessageCommand<Arguments>.() -> Unit
    ): MessageCommand<Arguments> {
        val commandObj = MessageCommand<Arguments>(this)
        body.invoke(commandObj)

        return commandObj
    }

    /**
     * Creates a [GroupCommand] object but does not register it.
     * This does not guarantee that the command is valid.
     */
    suspend fun <T: Arguments> createGroup(
        arguments: (() -> T)?,
        body: suspend GroupCommand<T>.() -> Unit
    ): GroupCommand<T> {
        val commandObj = GroupCommand(this, arguments)
        body.invoke(commandObj)

        return commandObj
    }

    /**
     * Creates a [GroupCommand] object but does not register it.
     * This does not guarantee that the command is valid.
     */
    suspend fun createGroup(
        body: suspend GroupCommand<Arguments>.() -> Unit
    ): GroupCommand<Arguments> {
        val commandObj = GroupCommand<Arguments>(this)
        body.invoke(commandObj)

        return commandObj
    }

    suspend fun <T: Arguments> group(commandObj: GroupCommand<T>): GroupCommand<T> = command(commandObj) as GroupCommand
}