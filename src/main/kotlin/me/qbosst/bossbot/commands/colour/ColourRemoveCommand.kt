package me.qbosst.bossbot.commands.colour

import me.qbosst.bossbot.database.manager.GuildColoursManager
import me.qbosst.bossbot.database.manager.colours
import me.qbosst.bossbot.database.manager.settings
import me.qbosst.jda.ext.commands.annotations.CommandFunction
import me.qbosst.jda.ext.commands.entities.Command
import me.qbosst.bossbot.entities.Context

class ColourRemoveCommand: Command() {
    override val label: String = "remove"
    override val aliases: Collection<String> = listOf("delete")
    override val guildOnly: Boolean = true

    @CommandFunction(examples = ["cookie", "all"])
    fun execute(ctx: Context, colour: String = "all") {
        when (colour.toLowerCase()) {
            "all" -> {
                GuildColoursManager.delete(ctx.guild!!.idLong)
                ctx.messageChannel.sendMessage("Deleted all colours from this guild").queue()
            }
            !in ctx.guild!!.colours -> {
                ctx.messageChannel.sendMessage("this guild does not have this colour").queue()
            }
            else -> {
                GuildColoursManager.delete(ctx.guild!!.idLong, colour)
                ctx.messageChannel.sendMessage("done").queue()
            }
        }
    }
}