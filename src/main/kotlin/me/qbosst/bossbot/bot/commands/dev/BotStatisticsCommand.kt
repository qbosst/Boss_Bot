package me.qbosst.bossbot.bot.commands.dev

import me.qbosst.bossbot.bot.commands.meta.Command
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

object BotStatisticsCommand : Command(
        "botstatistics",
        description = "Shows some bot statistics",
        botPermissions = listOf(Permission.MESSAGE_EMBED_LINKS),
        aliases = listOf("botstats"),
        guildOnly = false,
        developerOnly = true
)
{
    private val startUp = OffsetDateTime.now()

    override fun execute(event: MessageReceivedEvent, args: List<String>, flags: Map<String, String?>)
    {
        val totalMb = Runtime.getRuntime().totalMemory() / (1024*1024)
        val usedMb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024*1024)

        val zoneId = event.author.getUserData().zone_id ?: ZoneId.systemDefault()
        val date = TimeUtil.dateTimeFormatter.format(startUp.atZoneSameInstant(zoneId))
        val timeZone = TimeZone.getTimeZone(zoneId)
        val useDaylight = timeZone.inDaylightTime(Date())

        val embed = EmbedBuilder()
                .setTitle("${event.jda.selfUser.asTag} statistics")
                .addField("Startup Time", "$date ${timeZone.getDisplayName(useDaylight, TimeZone.SHORT)}", true)
                .addField("Memory Usage", "${usedMb}MB / ${totalMb}MB", true)
                .addField("Guilds", event.jda.shardManager!!.guilds.size.toString(), true)
                .setColor(event.getGuildOrNull()?.selfMember?.color)

        event.channel.sendMessage(embed.build()).queue()
    }

}