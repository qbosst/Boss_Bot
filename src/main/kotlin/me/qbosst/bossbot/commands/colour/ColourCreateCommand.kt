package me.qbosst.bossbot.commands.colour

import me.qbosst.bossbot.database.manager.GuildColoursManager
import me.qbosst.bossbot.database.manager.colours
import me.qbosst.bossbot.database.tables.GuildColoursTable
import me.qbosst.bossbot.util.ColourUtil
import me.qbosst.jda.ext.commands.annotations.CommandFunction
import me.qbosst.jda.ext.commands.annotations.Greedy
import me.qbosst.jda.ext.commands.entities.Command
import me.qbosst.bossbot.entities.Context
import java.awt.Color as Colour

class ColourCreateCommand: Command() {
    override val label: String = "create"
    override val guildOnly: Boolean = true

    @CommandFunction
    fun execute(ctx: Context, name: String, @Greedy colour: Colour) {
        when {
            ctx.guild!!.colours.size >= MAX_COLOURS_PER_GUILD ->
                TODO()

            name.toLowerCase() in ColourUtil.systemColours ->
                TODO()

            name.length > GuildColoursTable.MAX_COLOUR_NAME_LENGTH ->
                TODO()

            else -> {
                val created = GuildColoursManager.create(ctx.guild!!.idLong, name, colour)

                if(created) {
                    ctx.messageChannel.sendMessage("Colour has been created").queue()
                }
                else {
                    ctx.messageChannel.sendMessage("This colour already exists!").queue()
                }
            }
        }
    }

    companion object {
        private const val MAX_COLOURS_PER_GUILD = 100
    }
}