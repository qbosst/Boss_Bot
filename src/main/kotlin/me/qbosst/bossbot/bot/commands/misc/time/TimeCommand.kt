package me.qbosst.bossbot.bot.commands.misc.time

import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.bot.userNotFound
import me.qbosst.bossbot.entities.database.UserData
import me.qbosst.bossbot.util.TimeUtil
import me.qbosst.bossbot.util.getMemberByString
import me.qbosst.bossbot.util.loadObjects
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object TimeCommand: Command(
        "time",
        "Shows the time that users are in",
        usage = listOf("[@user] [duration]"),
        examples = listOf("@boss", "@boss 3h")
)
{
    init
    {
        addCommands(loadObjects(this::class.java.`package`.name, Command::class.java).filter { it != this })
    }

    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        val authorZoneId = getZoneId(event.author)
        if(args.isNotEmpty())
        {
            val target = event.guild.getMemberByString(args[0])?.user
            if(target == null)
                event.channel.sendMessage(userNotFound(args[0])).queue()
            else if(args.size > 1)
            {
                val targetZoneId = getZoneId(target)
                if(targetZoneId == null)
                    event.channel.sendMessage("${target.asTag} does not have a timezone setup").queue()
                else
                {
                    val seconds = TimeUtil.parseTime(args.drop(1).joinToString(" "))
                    var date = ZonedDateTime.now(targetZoneId)
                    if(seconds > 0)
                        date = date.plusSeconds(seconds)
                    else if(seconds < 0)
                        date = date.minusSeconds(-seconds)

                    event.channel.sendMessage("The time for ${target.asTag} in `${TimeUtil.secondsToString(seconds)}` will be `${formatZonedDateTime(date)}`").queue()
                }
            }
            else
                event.channel.sendMessage(getZoneInfo(Pair(event.author, authorZoneId), Pair(target, getZoneId(target)))).queue()
        }
        else
        {
            val zoneId = getZoneId(event.author)
            event.channel.sendMessage(getZoneInfo(Pair(event.author, zoneId))).queue()
        }
    }

    private fun getZoneId(user: User): ZoneId? = UserData.get(user).zone_id

    private fun getZoneInfo(author: Pair<User, ZoneId?>, target: Pair<User, ZoneId?> = author): String
    {
        val isSelf = author.first == target.first
        if(target.second == null)
            return "${if(isSelf) "You" else target.first.asTag} does not have a timezone setup"

        val sb = StringBuilder()
                .append("The time for ${if(isSelf) "you" else target.first.asTag} is `${getCurrentTime(target.second!!)}`. ")
                .append("${if(isSelf) "Your" else target.first.asTag.plus("'s")} time zone is `${target.second!!.id}`. ")

        if(author.second != null)
            if(author.second != target.second)
            {
                val differenceInSeconds = getZoneDifference(author.second!!, target.second!!)
                sb.append("${target.first.asTag} is `${TimeUtil.secondsToString(if(differenceInSeconds > 0) differenceInSeconds else -differenceInSeconds ) { it.longName }}` ${if(differenceInSeconds > 0) "ahead of" else "behind"} you")
            }
            else if(!isSelf)
                sb.append("They are in the same timezone as you!")

        return sb.toString()
    }

    private fun getZoneDifference(zone1: ZoneId, zone2: ZoneId): Long
    {
        val now = LocalDateTime.now()
        val zone1Time = now.atZone(zone1)
        val zone2Time = now.atZone(zone2)
        if(zone1Time.offset.totalSeconds > zone2Time.offset.totalSeconds)
            return -Duration.between(zone1Time, zone2Time).seconds
        else
            return Duration.between(zone2Time, zone1Time).seconds
    }

    private fun getCurrentTime(zoneId: ZoneId): String = formatZonedDateTime(ZonedDateTime.now(zoneId))

    private fun formatZonedDateTime(time: ZonedDateTime): String = time.format(DateTimeFormatter.ofPattern("HH:mm:ss | dd/MM/yyyy"))
}