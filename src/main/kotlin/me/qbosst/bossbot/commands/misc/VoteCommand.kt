package me.qbosst.bossbot.commands.misc

import me.qbosst.bossbot.BossBot
import me.qbosst.jda.ext.commands.annotations.CommandFunction
import me.qbosst.jda.ext.commands.entities.Command
import me.qbosst.jda.ext.commands.entities.Context
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission

class VoteCommand: Command() {
    override val label: String = "vote"
    override val botPermissions: Collection<Permission> = listOf(Permission.MESSAGE_EMBED_LINKS)

    @CommandFunction
    fun execute(ctx: Context) {
        val embed = EmbedBuilder()
            .setAuthor("Enjoying Boss Bot? Upvote it here!", null, ctx.jda.selfUser.effectiveAvatarUrl)
            .setColor(ctx.guild?.selfMember?.color)
            .setFooter("Thank you!")
            .apply {
                BossBot.config.voteLinks.forEach { link ->
                    appendDescription("${link}\n")
                }
            }
            .build()

        ctx.messageChannel.sendMessage(embed).queue()
    }
}