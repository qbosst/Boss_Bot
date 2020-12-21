package me.qbosst.bossbot.bot.commands.dev

import me.qbosst.bossbot.bot.BossBot
import me.qbosst.bossbot.database.managers.getSettings
import me.qbosst.bossbot.database.managers.getUserData
import me.qbosst.bossbot.util.TimeUtil
import me.qbosst.bossbot.util.getGuildOrNull
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.*

object BotStatisticsCommand : DeveloperCommand(
        "botstatistics",
        description = "Shows some bot statistics",
        botPermissions = listOf(Permission.MESSAGE_EMBED_LINKS),
        aliases = listOf("botstats"),
        guildOnly = false
)
{
    private val startUp = OffsetDateTime.now()

    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        val totalMb = Runtime.getRuntime().totalMemory() / (1024*1024)
        val usedMb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024*1024)

        val zoneId = event.author.getUserData().zone_id ?: event.getGuildOrNull()?.getSettings()?.zone_id ?: ZoneId.systemDefault()
        val date = TimeUtil.dateTimeFormatter.format(startUp.atZoneSameInstant(zoneId))

        val embed = EmbedBuilder()
                .setTitle("${event.jda.selfUser.asTag} statistics")
                .addField("Startup Time", "$date ${TimeZone.getTimeZone(zoneId).getDisplayName(true, TimeZone.SHORT)}", true)
                .addField("Memory Usage", "${usedMb}MB / ${totalMb}MB", true)
                .addField("Guilds", event.jda.shardManager!!.guilds.size.toString(), true)
                .setColor(event.getGuildOrNull()?.selfMember?.color)

        event.channel.sendMessage(embed.build()).queue()
    }

}