package me.qbosst.bossbot.commands.colour

import me.qbosst.bossbot.database.manager.GuildColoursManager
import me.qbosst.jda.ext.commands.annotations.CommandFunction
import me.qbosst.jda.ext.commands.entities.Command
import me.qbosst.bossbot.entities.Context
import net.dv8tion.jda.api.entities.User
import java.awt.Color as Colour

class ColourUpdateCommand: Command() {
    override val label: String = "update"
    override val guildOnly: Boolean = true

    @CommandFunction
    fun execute(ctx: Context, name: String, `new colour`: Colour) {
        val old = GuildColoursManager.update(ctx.guild!!.idLong, name, `new colour`)
        if(old == null) {
            ctx.messageChannel.sendMessage("colour does not exist").queue()
        }
        else {
            ctx.messageChannel.sendMessage("updated to $`new colour`").queue()
        }
    }
}