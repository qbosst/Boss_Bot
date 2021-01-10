package me.qbosst.bossbot.commands.dev

import me.qbosst.jda.ext.commands.annotations.CommandFunction
import me.qbosst.jda.ext.commands.entities.Command
import me.qbosst.bossbot.entities.Context
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import java.time.OffsetDateTime

class BotStatisticsCommand: Command() {

    override val label: String = "botstatistics"
    override val aliases: Collection<String> = listOf("botstats")
    override val developerOnly: Boolean = true
    override val botPermissions: Collection<Permission> = listOf(Permission.MESSAGE_EMBED_LINKS)

    @CommandFunction
    fun execute(ctx: Context) {
        val embed = EmbedBuilder()
            .setColor(ctx.guild?.selfMember?.color)
            .addField("Guilds", getGuilds(ctx.jda).toString(), true)
            .addField("Cached Users", getCachedUsers(ctx.jda).toString(), true)
            .addField("Uptime", startDateTime.toString(), true)
            .build()

        ctx.messageChannel.sendMessage(embed).queue()
    }

    private fun getGuilds(jda: JDA): Long =
        jda.shardManager?.guildCache?.size() ?: jda.guildCache.size()

    private fun getCachedUsers(jda: JDA): Long =
        jda.shardManager?.userCache?.size() ?: jda.userCache.size()

    companion object {
        private val startDateTime = OffsetDateTime.now()
    }
}