package me.qbosst.bossbot.commands.colour

import me.qbosst.bossbot.database.manager.colours
import me.qbosst.bossbot.util.ColourUtil
import me.qbosst.jda.ext.commands.annotations.CommandFunction
import me.qbosst.jda.ext.commands.entities.Command
import me.qbosst.jda.ext.commands.entities.Context
import me.qbosst.jda.ext.util.maxLength

class ColourViewCommand: Command() {

    override val label: String = "view"
    override val aliases: Collection<String> = listOf("options", "list")

    @CommandFunction
    fun execute(ctx: Context, `page number`: Int = 0, type: String = "all") {
        val colours = when(type.toLowerCase()) {
            "all" -> ColourUtil.systemColours.plus(ctx.guild?.colours?.map ?: mapOf())
            "guild" -> ctx.guild?.colours?.map ?: mapOf()
            "system" -> ColourUtil.systemColours
            else -> {
                ctx.messageChannel.sendMessage("unknown parameter type").queue()
                return
            }
        }

        ctx.messageChannel.sendMessage(colours.toString().maxLength(2000)).queue()
    }
}