package me.qbosst.bossbot.extensions

import com.kotlindiscord.kord.extensions.commands.converters.impl.MemberConverter
import com.kotlindiscord.kord.extensions.commands.converters.impl.member
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalUnion
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.core.behavior.UserBehavior
import dev.kord.core.entity.Member
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaZoneId
import me.qbosst.bossbot.commands.hybrid.HybridCommandContext
import me.qbosst.bossbot.converters.TimeZoneConverter
import me.qbosst.bossbot.converters.optionalCoalescingDuration
import me.qbosst.bossbot.converters.optionalDuration
import me.qbosst.bossbot.converters.timeZone
import me.qbosst.bossbot.database.dao.getUserDAO
import me.qbosst.bossbot.util.hybridCommand
import java.time.ZonedDateTime
import kotlin.time.Duration

class TimeExtension: Extension() {
    override val name: String get() = "time"

    class TimeArgs: Arguments() {
        val arg by optionalUnion(
            "arg",
            "union arg",
            converters = arrayOf(MemberConverter(), TimeZoneConverter())
        )
        val duration by optionalDuration("duration", "duration from now")
    }

    class TimeUserArgs: Arguments() {
        val user by member("user", "The user's whose time you want to display")
        val duration by optionalDuration("duration", "duration from now")
    }

    class TimeZoneArgs: Arguments() {
        val timeZone by timeZone("timezone", "The time zone you want to see")
        val duration by optionalDuration("duration", "duration from now")
    }

    class TimeNowArgs: Arguments() {
        val duration by optionalDuration("duration", "duration from now")
    }

    suspend fun HybridCommandContext<*>.handleUser(target: UserBehavior, duration: Duration?) {
        val targetZone = target.getUserDAO().timeZone
        when {
            targetZone == null -> replyNoTimeZone(this, target)
            duration == null -> replyCurrentTime(this, target, targetZone)
            else -> replyTimeIn(this, target, targetZone, duration)
        }
    }

    suspend fun HybridCommandContext<*>.handleTimeZone(timeZone: TimeZone, duration: Duration?) {
        if(duration == null) {
            replyCurrentTime(this, null, timeZone)
        } else {
            replyTimeIn(this, null, timeZone, duration)
        }
    }

    private suspend fun replyNoTimeZone(ctx: HybridCommandContext<*>, target: UserBehavior) = ctx.publicFollowUp {
        val isSelf = ctx.user!! == target
        content = if(isSelf) "You do not" else "${target.mention} does not" + " have a time zone setup."
    }

    private suspend fun replyCurrentTime(
        ctx: HybridCommandContext<*>,
        targetUser: UserBehavior?,
        timeZone: TimeZone
    ) = ctx.publicFollowUp {
        val now = ZonedDateTime.now(timeZone.toJavaZoneId())

        val target = when {
            ctx.user!! == targetUser -> "you `(${timeZone.id})`"
            targetUser != null -> "${targetUser.mention} `(${timeZone.id})`"
            else -> "`${timeZone.id}`"
        }

        content = "The time for $target is `$now`"
    }

    private suspend fun replyTimeIn(
        ctx: HybridCommandContext<*>,
        targetUser: UserBehavior?,
        timeZone: TimeZone,
        duration: Duration
    ) = ctx.publicFollowUp {
        val time = ZonedDateTime.now(timeZone.toJavaZoneId()).plusSeconds(duration.inWholeSeconds)

        val target = when {
            ctx.user!! == targetUser -> "you `(${timeZone.id})`"
            targetUser != null -> "${targetUser.mention} `(${timeZone.id})`"
            else -> "`${timeZone}`"
        }

        content = "The time for $target in `$duration` will be $time"
    }

    override suspend fun setup() {
        hybridCommand(::TimeArgs) {
            name = "time"
            description = "Time related command"

            action {
                when(arguments.arg) {
                    is TimeZone -> handleTimeZone(arguments.arg as TimeZone, arguments.duration)
                    is Member -> handleUser(arguments.arg as Member, arguments.duration)
                    else -> handleUser(user!!, arguments.duration)
                }
            }

            subCommand(::TimeUserArgs) {
                name = "user"
                description = "Views the time for a user"

                action {
                    handleUser(arguments.user, arguments.duration)
                }
            }

            subCommand(::TimeNowArgs) {
                name = "now"
                description = "Views the time for you"

                action {
                    handleUser(user!!, arguments.duration)
                }
            }

            subCommand(::TimeZoneArgs) {
                name = "zone"
                description = "Tests stuff"

                action {
                    handleTimeZone(arguments.timeZone, arguments.duration)
                }
            }

            subCommand {
                name = "zones"
                description = "Views the different time zones that are available"

                action {
                    val zones = TimeZone.availableZoneIds.sorted().joinToString("\n").byteInputStream()
                    publicFollowUp {
                        content = "Here is a list of time zones you can use..."
                        addFile("zones.txt", zones)
                    }
                    zones.close()
                }
            }
        }
    }
}