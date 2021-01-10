package me.qbosst.bossbot.commands.dev

import me.qbosst.jda.ext.commands.annotations.CommandFunction
import me.qbosst.jda.ext.commands.entities.Command
import me.qbosst.bossbot.entities.Context
import me.qbosst.jda.ext.util.maxLength
import net.dv8tion.jda.api.entities.Message

class CommandStatisticsCommand: Command() {
    override val label: String = "commandstatistics"
    override val aliases: Collection<String> = listOf("commandstats", "cmdstatistics", "cmdstats")
    override val developerOnly: Boolean = true

    @CommandFunction
    fun execute(ctx: Context) {
        val stats = ctx.client.stats.asSequence()
            .sortedByDescending { (_, value) -> value }
            .map { (key, value) -> Pair(key, value) }
            .toMap()


        ctx.messageChannel.sendMessage(stats.toString().maxLength(Message.MAX_CONTENT_LENGTH)).queue()
    }
}