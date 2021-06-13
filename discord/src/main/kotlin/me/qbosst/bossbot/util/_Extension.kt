package me.qbosst.bossbot.util

import com.kotlindiscord.kord.extensions.annotations.ExtensionDSL
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.core.event.Event
import me.qbosst.bossbot.commands.HybridCommand

@ExtensionDSL
suspend fun <T: Arguments> Extension.hybridCommand(
    arguments: (() -> T)?,
    body: suspend HybridCommand<T>.() -> Unit
): HybridCommand<T> {
    val hybridCommandObj = HybridCommand(this, arguments)
    body.invoke(hybridCommandObj)

    // create a message command
    val messageCommandObj = hybridCommandObj.toMessageCommand()
    command(messageCommandObj)

    // create a slash command
    val slashCommandObj = hybridCommandObj.toSlashCommand()
    slashCommand(slashCommandObj)

    return hybridCommandObj
}

@ExtensionDSL
suspend fun Extension.hybridCommand(body: suspend HybridCommand<Arguments>.() -> Unit): HybridCommand<Arguments> {
    val hybridCommandObj = HybridCommand<Arguments>(this)
    body.invoke(hybridCommandObj)

    // create a message command
    val messageCommandObj = hybridCommandObj.toMessageCommand()
    command(messageCommandObj)

    // create a slash command
    val slashCommandObj = hybridCommandObj.toSlashCommand()
    slashCommand(slashCommandObj)

    return hybridCommandObj
}

@ExtensionDSL
fun Extension.hybridCheck(body: suspend (Event) -> Boolean) {
    commandChecks.add(body)
    slashCommandChecks.add(body)
}

@ExtensionDSL
fun Extension.hybridCheck(vararg checks: suspend (Event) -> Boolean) {
    commandChecks.addAll(checks)
    slashCommandChecks.addAll(checks)
}