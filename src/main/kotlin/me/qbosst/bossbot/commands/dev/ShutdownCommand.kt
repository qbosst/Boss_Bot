package me.qbosst.bossbot.commands.dev

import me.qbosst.jda.ext.commands.annotations.CommandFunction
import me.qbosst.jda.ext.commands.entities.Command
import me.qbosst.bossbot.entities.Context

class ShutdownCommand: Command() {

    override val label: String = "shutdown"
    override val developerOnly: Boolean = true

    @CommandFunction
    fun execute(ctx: Context) {
        ctx.jda.shardManager?.shutdown() ?: ctx.jda.shutdown()
        ctx.messageChannel.sendMessage("shutting down!").queue()
    }
}