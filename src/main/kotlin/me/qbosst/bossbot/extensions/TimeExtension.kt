package me.qbosst.bossbot.extensions

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.commands.converters.Converter
import com.kotlindiscord.kord.extensions.commands.converters.impl.IntConverter
import com.kotlindiscord.kord.extensions.commands.converters.impl.StringConverter
import com.kotlindiscord.kord.extensions.commands.converters.impl.UnionConverter
import com.kotlindiscord.kord.extensions.commands.converters.impl.UserConverter
import com.kotlindiscord.kord.extensions.commands.converters.optionalCoalescedDuration
import com.kotlindiscord.kord.extensions.commands.converters.optionalUnion
import com.kotlindiscord.kord.extensions.commands.converters.union
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.authorId
import dev.kord.cache.api.put
import dev.kord.cache.api.query
import dev.kord.core.behavior.UserBehavior
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.User
import me.qbosst.bossbot.converters.coalescedZoneId
import me.qbosst.bossbot.converters.impl.DurationConverter
import me.qbosst.bossbot.converters.impl.ZoneIdConverter
import me.qbosst.bossbot.converters.optionalCoalescedDuration
import me.qbosst.bossbot.converters.toCoalescing
import me.qbosst.bossbot.database.models.UserData
import me.qbosst.bossbot.database.models.getOrRetrieveData
import me.qbosst.bossbot.database.tables.UserDataTable
import me.qbosst.bossbot.util.TimeUtil
import me.qbosst.bossbot.util.ext.reply
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class TimeExtension(bot: ExtensibleBot): Extension(bot) {
    override val name: String = "time"

    class TimeArgs: Arguments() {
        val arg by optionalUnion("arg name", "arg desc", "arg type", true, ZoneIdConverter(), UserConverter(), DurationConverter().toCoalescing())
        val duration by optionalCoalescedDuration("duration", "", true)
    }

    class SetTimeArgs: Arguments() {
        val zoneId by coalescedZoneId("zone id", "", shouldThrow = true)
    }

    override suspend fun setup() {
        group(::TimeArgs) {
            name = "time"

            action {
                val duration = arguments.duration
                when(val arg = arguments.arg) {
                    null -> handleUser(message.channel, message.author!!, true, null)
                    is ZoneId -> {
                        if(duration != null) {
                            displayTimeIn(message.channel, arg, null, false, duration)
                        } else {
                            displayCurrentTime(message.channel, arg, null, false)
                        }
                    }
                    is Duration -> handleUser(message.channel, message.author!!, true, arg)
                    is User -> {
                        val isSelf = arg.id == message.data.authorId
                        handleUser(message.channel, arg, isSelf, duration)
                    }
                }
            }

            command(::SetTimeArgs) {
                name = "set"

                action {
                    val id = message.data.authorId.value

                    val newZoneId = arguments.zoneId
                    val oldZoneId = user?.getOrRetrieveData()?.zoneId

                    message.reply(false) {
                        if(oldZoneId == newZoneId) {
                            content = "Your zone id is already set to `${oldZoneId}`"
                        } else {
                            content = "Your time zone has been updated from `${oldZoneId}` to `${newZoneId.id}`"

                            message.kord.cache
                                .query<UserData> { UserData::userId.eq(id) }
                                .update { data -> data.copy(zoneId = newZoneId) }
                        }
                    }
                }
            }

            command {
                name = "zones"

                action {
                    val zones = ZoneId.getAvailableZoneIds().sorted().joinToString("\n").byteInputStream()
                    message.reply(false) {
                        addFile("zones.txt", zones)
                    }
                }
            }
        }
    }

    private suspend fun handleUser(
        channel: MessageChannelBehavior,
        user: User,
        isSelf: Boolean,
        duration: Duration?
    ) {
        val zoneId = user.getZoneId()
        when {
            zoneId == null -> displayNoTimeZone(channel, if(isSelf) null else user)
            duration == null -> displayCurrentTime(channel, zoneId, user, isSelf)
            else -> displayTimeIn(channel, zoneId, user, isSelf, duration)
        }
    }

    private suspend fun UserBehavior.getZoneId(): ZoneId? = getOrRetrieveData().zoneId

    private suspend fun displayNoTimeZone(channel: MessageChannelBehavior, user: User?) {
        channel.createMessage {
            content = buildString {
                if(user != null) {
                    append("${user.tag} does not")
                } else {
                    append("You do not")
                }
                append(" have a time zone setup")
            }
        }
    }

    private suspend fun displayTimeIn(
        channel: MessageChannelBehavior,
        zoneId: ZoneId,
        user: User?,
        isSelf: Boolean,
        duration: Duration
    ) {
        channel.createMessage {
            content = buildString {
                append("The time for ${getTarget(zoneId, user, isSelf)} in ")
                val seconds = duration.seconds
                val date = ZonedDateTime.now(zoneId).plusSeconds(seconds)
                val time = TimeUtil.timeToString(seconds, TimeUnit.SECONDS, delimiter = ", ")
                append("$time will be `${date.format()}`")
            }
        }
    }

    private suspend fun displayCurrentTime(channel: MessageChannelBehavior, zoneId: ZoneId, user: User?, isSelf: Boolean) {
        channel.createMessage {
            content = buildString {
                append("The time for ${getTarget(zoneId, user, isSelf)}")
                val now = ZonedDateTime.now(zoneId)
                append(" is `${now.format()}`")
            }
        }
    }

    private fun getTarget(zoneId: ZoneId, user: User?, isSelf: Boolean) = when {
        isSelf -> "you `${zoneId.id}`"
        user != null -> "${user.tag} `${zoneId.id}`"
        else -> "`${zoneId.id}`"
    }

    private fun ZonedDateTime.format() = TimeUtil.DATE_TIME_FORMATTER.format(this)
}