package me.qbosst.bossbot.commands.misc

import dev.minn.jda.ktx.await
import me.qbosst.jda.ext.commands.annotations.CommandFunction
import me.qbosst.jda.ext.commands.entities.Command
import me.qbosst.jda.ext.commands.entities.Context
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

class PingCommand: Command() {

    override val label: String = "ping"

    @ExperimentalTime
    @CommandFunction
    suspend fun execute(ctx: Context) {
        val (message, time) = measureTimedValue { ctx.messageChannel.sendMessage("Pinging...").await() }
        message.editMessage("${time.toLongMilliseconds()}ms").queue()
    }
}