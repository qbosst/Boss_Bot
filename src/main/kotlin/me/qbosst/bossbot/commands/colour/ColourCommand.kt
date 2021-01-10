package me.qbosst.bossbot.commands.colour

import me.qbosst.bossbot.database.manager.colours
import me.qbosst.bossbot.util.ColourUtil
import me.qbosst.bossbot.util.ColourUtil.sendColourEmbed
import me.qbosst.jda.ext.commands.annotations.CommandFunction
import me.qbosst.jda.ext.commands.annotations.Greedy
import me.qbosst.jda.ext.commands.entities.Command
import me.qbosst.bossbot.entities.Context
import net.dv8tion.jda.api.Permission
import java.awt.Color as Colour

class ColourCommand: Command() {

    override val label: String = "colour"
    override val aliases: Collection<String> = listOf("color", "colours", "color")

    override val botPermissions: Collection<Permission> = listOf(
        Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES
    )

    init {
        addChildren(listOf(
            ColourRandomCommand(), ColourCreateCommand(), ColourUpdateCommand(), ColourRemoveCommand(),
            ColourBlendCommand(), ColourViewCommand()
        ))
    }

    @CommandFunction(priority=0)
    fun execute(ctx: Context, @Greedy colour: Colour) = ctx.messageChannel.sendColourEmbed(colour).queue()

    @CommandFunction(priority=1)
    fun execute(ctx: Context, name: String) {
        val colour = ColourUtil.systemColours[name] ?: ctx.guild?.colours?.get(name)
        if(colour != null) {
            ctx.messageChannel.sendColourEmbed(colour).queue()
        } else {
            ctx.messageChannel.sendMessage("invalid colour").queue()
        }
    }
}