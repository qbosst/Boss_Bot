package me.qbosst.bossbot.bot.commands.misc.time

import me.qbosst.bossbot.bot.argumentInvalid
import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.database.managers.getUserData
import me.qbosst.bossbot.util.*
import me.qbosst.bossbot.util.TimeUtil.formattedName
import me.qbosst.bossbot.util.extensions.getGuildOrNull
import me.qbosst.bossbot.util.extensions.getMemberByString
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

object TimeCommand: Command(
        "time",
        "Shows the time that users are in",
        usages = listOf("[@user|zoneId] [duration]"),
        examples = listOf("", "@boss", "@boss 3h", "europe/london -30 minutes", "us/eastern"),
        guildOnly = false,
        children = listOf(TimeSetCommand, TimeZonesCommand)
)
{
    override fun execute(event: MessageReceivedEvent, args: List<String>, flags: Map<String, String?>)
    {
        if(args.isNotEmpty())
        {
            val target = event.getGuildOrNull()?.getMemberByString(args[0])?.user
            if(target != null)
            {
                val targetZoneId = target.getUserData().zone_id
                val isSelfMention = target == event.author
                if(targetZoneId != null)
                {
                    val arguments = args.drop(1).joinToString(" ")
                    if(arguments.isNotBlank())
                    {
                        val seconds = TimeUtil.parseTime(arguments)
                        if(seconds != 0L)
                            event.channel.sendMessage(getTimeIn("${if(isSelfMention) "you" else target.asTag} `(${targetZoneId.id})`", targetZoneId, seconds)).queue()
                        else
                            event.channel.sendMessage(argumentInvalid(arguments, "duration")).queue()
                    }
                    else
                    {
                        val sb = StringBuilder("The time for ${if(isSelfMention) "you" else target.asTag} `(${targetZoneId.id})`")
                                .append("is `${getCurrentTime(targetZoneId)}`. ")

                        val selfZoneId = event.author.getUserData().zone_id
                        if(selfZoneId != null)
                        {
                            if(selfZoneId != targetZoneId)
                            {
                                val difference = getZoneDifference(selfZoneId, targetZoneId)
                                val time = TimeUtil.timeToString(difference) { unit, count -> "$count ${unit.formattedName}"}
                                val isBehind = time.startsWith("-")
                                sb.append("${target.asTag} is `${if(isBehind) time.substring(1) else time}` ")
                                        .append("`${if(isBehind) "behind" else "ahead of"}` you.")
                            }
                            else if(!isSelfMention)
                                sb.append("${target.asTag} is in the same time zone as you")
                        }

                        event.channel.sendMessage(sb).queue()
                    }
                }
                else
                    event.channel.sendMessage(noTimeZone(if(isSelfMention) "You do" else "${target.asTag} does")).queue()
            }
            else
            {
                val targetZoneId = TimeUtil.filterZones(args[0]).firstOrNull()
                if(targetZoneId != null)
                {
                    val arguments = args.drop(1).joinToString(" ")
                    if(arguments.isNotBlank())
                    {
                        val seconds = TimeUtil.parseTime(arguments)
                        if(seconds != 0L)
                            event.channel.sendMessage(getTimeIn("`${targetZoneId.id}`", targetZoneId, seconds)).queue()
                        else
                            event.channel.sendMessage(argumentInvalid(arguments, "duration")).queue()
                    }
                    else
                    {
                        val sb = StringBuilder("The time for `${targetZoneId.id}` is `${getCurrentTime(targetZoneId)}`. ")
                        val selfZoneId = event.author.getUserData().zone_id
                        if(selfZoneId != null && selfZoneId != targetZoneId)
                        {
                            val difference = getZoneDifference(selfZoneId, targetZoneId)
                            val time = TimeUtil.timeToString(difference) { unit, count -> "$count ${unit.formattedName}"}
                            val isBehind = time.startsWith("-")
                            sb.append("`${targetZoneId.id}` is `${if(isBehind) time.substring(1) else time}` ")
                                    .append("`${if(isBehind) "behind" else "ahead of"}` you.")
                        }

                        event.channel.sendMessage(sb).queue()
                    }
                }
                else
                {
                    val selfZoneId = event.author.getUserData().zone_id
                    if(selfZoneId != null)
                    {
                        val arguments = args.joinToString(" ")
                        val seconds = TimeUtil.parseTime(arguments)
                        if(seconds != 0L)
                            event.channel.sendMessage(getTimeIn("you `(${selfZoneId.id})`", selfZoneId, seconds)).queue()
                        else
                            event.channel.sendMessage(argumentInvalid(args[0], "user, zone or duration")).queue()
                    }
                    else
                        event.channel.sendMessage(noTimeZone("You do")).queue()
                }
            }
        }
        else
        {
            val targetZoneId = event.author.getUserData().zone_id
            if(targetZoneId != null)
            {
                val sb = StringBuilder("The time for you `(${targetZoneId.id})` is `${getCurrentTime(targetZoneId)}`")
                event.channel.sendMessage(sb).queue()
            }
            else
                event.channel.sendMessage(noTimeZone("You do")).queue()
        }
    }

    private fun getTimeIn(who: String, zoneId: ZoneId, seconds: Long): String
    {
        var date = ZonedDateTime.now(zoneId)
        if(seconds > 0)
            date = date.plusSeconds(seconds)
        else if(seconds < 0)
            date = date.minusSeconds(-seconds)

        return StringBuilder()
                .append("The time for $who in ")
                .append("`${TimeUtil.timeToString(seconds) {unit, count -> "$count ${unit.formattedName}"}}` ")
                .append("will be `${formatDate(date)}`.")
                .toString()
    }

    /**
     *  Gets the time difference between two different zones
     *
     *  @param zoneId1 The first zoneId
     *  @param zoneId2 The second zoneId
     *
     *  @return The difference in time between the two zones given in seconds
     */
    private fun getZoneDifference(zoneId1: ZoneId, zoneId2: ZoneId): Int
    {
        val now = OffsetDateTime.now()
        val nowZone1 = now.atZoneSameInstant(zoneId1)
        val nowZone2 = now.atZoneSameInstant(zoneId2)
        return nowZone2.offset.totalSeconds - nowZone1.offset.totalSeconds
    }

    private fun getCurrentTime(zoneId: ZoneId): String = formatDate(ZonedDateTime.now(zoneId))

    private fun formatDate(date: ZonedDateTime): String = TimeUtil.dateTimeFormatter.format(date)

    /**
     *  Message sent for when a user does not have a time zone setup
     *
     *  @param who Who doesn't have a time zone setup
     *
     *  @return Error message for not having a time zone setup.
     */
    private fun noTimeZone(who: String): String = "$who not have a time zone setup"
}