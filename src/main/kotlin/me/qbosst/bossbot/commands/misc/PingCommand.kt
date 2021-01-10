package me.qbosst.bossbot.commands.misc

import me.qbosst.jda.ext.commands.annotations.CommandFunction
import me.qbosst.jda.ext.commands.entities.Command
import me.qbosst.bossbot.entities.Context
import me.qbosst.jda.ext.async.await
import net.dv8tion.jda.api.Permission
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

class PingCommand: Command() {

    override val label: String = "ping"
    override val botPermissions: Collection<Permission> = listOf(Permission.MESSAGE_HISTORY)

    @ExperimentalTime
    @CommandFunction
    suspend fun execute(ctx: Context) {
        val (message, time) = measureTimedValue { ctx.message.reply("Pinging...").mentionRepliedUser(false).await() }
        message.editMessage("${time.toLongMilliseconds()}ms").queue()
    }
}