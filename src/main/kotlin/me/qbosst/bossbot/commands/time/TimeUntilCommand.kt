package me.qbosst.bossbot.commands.time

import me.qbosst.jda.ext.commands.annotations.CommandFunction
import me.qbosst.jda.ext.commands.entities.Command
import me.qbosst.jda.ext.commands.entities.Context

class TimeUntilCommand: Command() {
    override val label: String = "until"

    @CommandFunction
    fun execute(ctx: Context, timestamp: String) {

    }
}