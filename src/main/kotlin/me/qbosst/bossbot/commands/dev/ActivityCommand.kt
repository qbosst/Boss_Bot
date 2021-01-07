package me.qbosst.bossbot.commands.dev

import me.qbosst.jda.ext.commands.annotations.CommandFunction
import me.qbosst.jda.ext.commands.entities.Command
import me.qbosst.jda.ext.commands.entities.Context
import net.dv8tion.jda.api.entities.Activity

class ActivityCommand: Command() {

    override val label: String = "activity"
    override val developerOnly: Boolean = true

    @CommandFunction
    fun execute(ctx: Context, activity: Activity) {
        ctx.jda.shardManager!!.setActivity(activity)
        ctx.messageChannel.sendMessage("set activity").queue()
    }
}