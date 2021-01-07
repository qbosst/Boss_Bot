package me.qbosst.bossbot.commands.time

import me.qbosst.bossbot.database.manager.userData
import me.qbosst.jda.ext.commands.annotations.CommandFunction
import me.qbosst.jda.ext.commands.annotations.Greedy
import me.qbosst.jda.ext.commands.entities.Command
import me.qbosst.jda.ext.commands.entities.Context
import me.qbosst.jda.ext.util.TimeUtil
import me.qbosst.jda.ext.util.withSingleLineCode
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.User
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class TimeCommand: Command()
{
    override val label: String = "time"

    init
    {
        addChildren(listOf(TimeSetCommand(), TimeUntilCommand()))
    }

    @CommandFunction(priority=1)
    fun executeSelf(ctx: Context, @Greedy duration: Duration?)
    {
        val author = ctx.event.author
        val zoneId = author.userData.zoneId
        if(zoneId != null)
        {
            if(duration != null)
            {
                displayTimeIn(ctx.messageChannel, zoneId, author, true, duration)
            }
            else
            {
                displayCurrentTime(ctx.messageChannel, zoneId, author, true)
            }
        }
        else
        {
            displayNoTimeZone(ctx.messageChannel, author)
        }
    }

    @CommandFunction(priority=0)
    fun executeUser(ctx: Context, user: User, @Greedy duration: Duration?)
    {
        val zoneId = user.userData.zoneId
        val isSelf = ctx.author == user
        if(zoneId != null)
        {
            if(duration != null)
            {
                displayTimeIn(ctx.messageChannel, zoneId, user, isSelf, duration)
            }
            else
            {
                displayCurrentTime(ctx.messageChannel, zoneId, user, isSelf)
            }
        }
        else
        {
            displayNoTimeZone(ctx.messageChannel, user)
        }
    }

    @CommandFunction(priority=0)
    fun executeZoneId(ctx: Context, `zone id`: ZoneId, @Greedy duration: Duration?)
    {
        if(duration != null)
        {
            displayTimeIn(ctx.messageChannel, `zone id`, null, false, duration)
        }
        else
        {
            displayCurrentTime(ctx.messageChannel, `zone id`, null, false)
        }
    }

    private fun displayNoTimeZone(channel: MessageChannel, user: User?)
    {
        val sb = buildString {
            if(user != null) append("${user.asTag} does not") else append("You do not")
            append(" have a timezone setup")
        }
        channel.sendMessage(sb).queue()
    }

    private fun displayCurrentTime(channel: MessageChannel, zoneId: ZoneId, user: User?, isSelf: Boolean)
    {
        val sb = buildString {
            append("The time for ${getTarget(zoneId, user, isSelf)}")
            val now = ZonedDateTime.now(zoneId)
            append(" is ").append(now.format(TimeUtil.dateTimeFormatter).withSingleLineCode())
        }
        channel.sendMessage(sb).queue()
    }

    private fun displayTimeIn(channel: MessageChannel, zoneId: ZoneId, user: User?, isSelf: Boolean, duration: Duration)
    {
        val sb = buildString {
            append("The time for ${getTarget(zoneId, user, isSelf)} in ")
            val seconds = duration.seconds
            val date = ZonedDateTime.now(zoneId).plusSeconds(seconds)
            append(TimeUtil.timeToString(seconds, TimeUnit.SECONDS).withSingleLineCode()).append(" will be ")
            append(date.format(TimeUtil.dateTimeFormatter).withSingleLineCode())
        }

        channel.sendMessage(sb).queue()
    }

    private fun getTarget(zoneId: ZoneId, user: User?, isSelf: Boolean): String = when
    {
        isSelf -> "you `(${zoneId.id})`"
        user != null -> "${user.asTag} `(${zoneId.id})`"
        else -> zoneId.id.withSingleLineCode()
    }
}