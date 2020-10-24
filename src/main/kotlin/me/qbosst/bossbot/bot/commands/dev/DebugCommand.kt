package me.qbosst.bossbot.bot.commands.dev

import me.qbosst.bossbot.bot.BossBot
import me.qbosst.bossbot.database.managers.getSettings
import me.qbosst.bossbot.util.TimeUtil
import me.qbosst.bossbot.util.getGuildOrNull
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.time.ZoneId
import java.util.*

object DebugCommand : DeveloperCommand(
        "debug",
        botPermissions = listOf(Permission.MESSAGE_EMBED_LINKS)
)
{
    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        val totalMb = Runtime.getRuntime().totalMemory() / (1024*1024)
        val usedMb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024*1024)

        val zoneId = event.getGuildOrNull()?.getSettings()?.zone_id ?: ZoneId.systemDefault()
        val date = TimeUtil.DATE_TIME_FORMATTER.format(BossBot.START_UP.atZoneSameInstant(zoneId))

        val embed = EmbedBuilder()
                .setTitle("${event.jda.selfUser.asTag} statistics")
                .addField("Startup Time", "$date ${TimeZone.getTimeZone(zoneId).getDisplayName(true, TimeZone.SHORT)}", true)
                .addField("Memory Usage", "${usedMb}MB / ${totalMb}MB", true)
                .addField("Guilds", BossBot.SHARDS_MANAGER.guilds.size.toString(), true)

        event.channel.sendMessage(embed.build()).queue()
    }

}