package me.qbosst.bossbot.commands.colour

import me.qbosst.bossbot.util.ColourUtil.nextColour
import me.qbosst.bossbot.util.ColourUtil.sendColourEmbed
import me.qbosst.jda.ext.commands.annotations.CommandFunction
import me.qbosst.jda.ext.commands.entities.Command
import me.qbosst.bossbot.entities.Context
import net.dv8tion.jda.api.Permission
import kotlin.random.Random

class ColourRandomCommand: Command() {

    override val label: String = "random"
    override val aliases: Collection<String> = listOf("rand")

    override val botPermissions: Collection<Permission> = listOf(
        Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES
    )

    @CommandFunction
    fun execute(ctx: Context, `has alpha`: Boolean = false) {
        ctx.messageChannel.sendColourEmbed(Random.nextColour(`has alpha`)).queue()
    }
}