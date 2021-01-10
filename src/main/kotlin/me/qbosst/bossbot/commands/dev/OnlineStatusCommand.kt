package me.qbosst.bossbot.commands.dev

import me.qbosst.jda.ext.commands.annotations.CommandFunction
import me.qbosst.jda.ext.commands.entities.Command
import me.qbosst.bossbot.entities.Context
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity

class OnlineStatusCommand: Command() {

    override val label: String = "onlinestatus"
    override val developerOnly: Boolean = true

    @CommandFunction(examples = ["online", "idle", "dnd", "invisible", "offline"])
    fun execute(ctx: Context, status: OnlineStatus) {
        ctx.jda.shardManager!!.setStatus(status)
        ctx.messageChannel.sendMessage("set online status").queue()
    }
}