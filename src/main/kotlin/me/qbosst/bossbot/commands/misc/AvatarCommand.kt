package me.qbosst.bossbot.commands.misc

import me.qbosst.jda.ext.commands.annotations.CommandFunction
import me.qbosst.jda.ext.commands.annotations.Greedy
import me.qbosst.jda.ext.commands.entities.Command
import me.qbosst.jda.ext.commands.entities.Context
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.User

class AvatarCommand: Command() {
    override val label: String = "avatar"
    override val aliases: Collection<String> = listOf("av")

    @CommandFunction
    fun execute(ctx: Context, @Greedy user: User = ctx.author) {

        val embed = EmbedBuilder()
            .setDescription("[${user.asTag}](${user.effectiveAvatarUrl})")
            .setImage("${user.effectiveAvatarUrl}?size=256")
            .setColor(ctx.guild?.selfMember?.color)
            .build()

        ctx.messageChannel.sendMessage(embed).queue()
    }
}