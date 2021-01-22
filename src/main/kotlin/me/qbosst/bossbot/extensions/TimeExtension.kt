package me.qbosst.bossbot.extensions

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.commands.MessageCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.UserConverter
import com.kotlindiscord.kord.extensions.commands.converters.optionalCoalescedDuration
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.utils.authorId
import dev.kord.cache.api.put
import dev.kord.cache.api.query
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.User
import me.qbosst.bossbot.converters.coalescedZoneId
import me.qbosst.bossbot.converters.impl.DurationConverter
import me.qbosst.bossbot.converters.impl.ZoneIdConverter
import me.qbosst.bossbot.converters.optionalUnion
import me.qbosst.bossbot.converters.toCoalescing
import me.qbosst.bossbot.database.models.UserData
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

class TimeExtension(bot: ExtensibleBot): BaseExtension(bot) {
    override val name: String = "time"

    override suspend fun setup() {
        group(timeGroup())
    }

    private suspend fun timeGroup() = createGroup({
        class Args: Arguments() {
            val arg by optionalUnion("zone id | user", "", true, ZoneIdConverter(), UserConverter(), DurationConverter().toCoalescing())
            val duration by optionalCoalescedDuration("duration", "", false)
        }
        return@createGroup Args()
    }) {
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

        command(nowCommand())
        command(zonesCommand())
        command(setCommand())
    }

    private suspend fun nowCommand() = createCommand({
        class Args: Arguments() {
            val arg by optionalUnion("zon", "", true, ZoneIdConverter().toCoalescing(), UserConverter().toCoalescing())
        }
        return@createCommand Args()
    }) {
        name = "now"

        action {
            when(val arg = arguments.arg) {
                null -> handleUser(message.channel, message.author!!, true, null)
                is ZoneId -> displayCurrentTime(message.channel, arg, null, false)
                is User -> {
                    val isSelf = arg.id == message.data.authorId
                    handleUser(message.channel, arg, isSelf, null)
                }
            }
        }
    }

    private suspend fun zonesCommand() = createCommand {
        name = "zones"

        action {
            val zones = ZoneId.getAvailableZoneIds().sorted().joinToString("\n").byteInputStream()
            message.reply(false) {
                addFile("zones.txt", zones)
            }
        }
    }

    private suspend fun setCommand() = createCommand({
        class Args: Arguments() {
            val newZoneId by coalescedZoneId("zone id", "")
        }
        return@createCommand Args()
    }) {
        name = "set"

        action {
            val idLong = message.data.authorId.value

            val newZoneId = arguments.newZoneId
            val oldZoneId = transaction {
                val record = UserDataTable
                    .select { UserDataTable.userId.eq(idLong) }
                    .singleOrNull()

                val oldZoneId = record?.get(UserDataTable.zoneId)

                when {
                    record == null -> {
                        UserDataTable.insert {
                            it[UserDataTable.userId] = idLong
                            it[UserDataTable.zoneId] = newZoneId?.id
                        }
                    }
                    newZoneId.id != oldZoneId -> {
                        UserDataTable.update({ UserDataTable.userId.eq(idLong) }) {
                            it[UserDataTable.zoneId] = newZoneId?.id
                        }
                    }
                    else -> {}
                }
                return@transaction oldZoneId
            }

            message.reply(false) {
                if(oldZoneId == newZoneId.id) {
                    content = "Your zone id is already set to `${oldZoneId}`"
                } else {
                    content = "Your time zone has been updated from `${oldZoneId}` to `${newZoneId?.id}`"

                    message.kord.cache
                        .query<UserData> { UserData::userId.eq(idLong) }
                        .update { data -> data.copy(zoneId = newZoneId) }
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

    private suspend fun User.getZoneId(): ZoneId? {
        val idLong = this.id.value

        // try to get zone id from cache fist
        val cache = this.kord.cache
        val cachedData = cache.query<UserData> { UserData::userId.eq(idLong) }.singleOrNull()
        if(cachedData != null) {
            return cachedData.zoneId
        }

        // get user data from database
        val retrievedData = transaction {
            UserDataTable
                .select { UserDataTable.userId.eq(idLong) }
                .singleOrNull()
                ?.let { row ->
                    UserData(
                        userId = row[UserDataTable.userId],
                        zoneId = row[UserDataTable.zoneId]?.let { id -> ZoneId.of(id) }
                    )
                }
        }

        val data = retrievedData ?: UserData(userId = idLong)
        cache.put(data)
        return data.zoneId
    }

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