package me.qbosst.bossbot.extensions

import com.gitlab.kordlib.cache.api.put
import com.gitlab.kordlib.cache.api.query
import com.gitlab.kordlib.cache.api.remove
import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.commands.Command
import com.kotlindiscord.kord.extensions.commands.GroupCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.UserConverter
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.authorId
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.User
import me.qbosst.bossbot.converters.*
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

class TimeExtension(bot: ExtensibleBot): Extension(bot) {

    override val name: String = "time"

    override suspend fun setup() {
        group(timeGroup)
    }

    private val timeGroup: suspend GroupCommand.() -> Unit = {
        class Args: Arguments() {
            val arg by optionalUnionOf("zone id | user", ZoneIdConverter(), UserConverter())
            val duration by optionalCoalescingDuration("duration")
        }

        name = "time"

        action {
            with(parse(::Args)) {
                when(arg) {
                    null -> {
                        handleUser(message.channel, message.author!!, true, duration)
                    }
                    is ZoneId -> {
                        if(duration != null) {
                            displayTimeIn(message.channel, arg as ZoneId, null, false, duration!!)
                        } else {
                            displayCurrentTime(message.channel, arg as ZoneId, null, false)
                        }
                    }
                    is User -> {
                        val user = arg as User
                        val isSelf = user.id == message.data.authorId
                        handleUser(message.channel, user, isSelf, duration)
                    }
                }
            }
        }

        command(setCommand)
        command(zonesCommand)
        command(nowCommand)
    }

    private val nowCommand: suspend Command.() -> Unit = {
        class Args: Arguments() {
            val arg by optionalUnionOf("zone id | user", ZoneIdConverter().toCoalescing(), UserConverter().toCoalescing())
        }

        name = "now"

        action {
            with(parse(::Args)) {
                when(arg) {
                    null -> handleUser(message.channel, message.author!!, true, null)

                    is ZoneId -> displayCurrentTime(message.channel, arg as ZoneId, null, false)

                    is User -> {
                        val user = arg as User
                        val isSelf = user.id == message.data.authorId
                        handleUser(message.channel, user, isSelf, null)
                    }
                }
            }
        }
    }

    private val setCommand: suspend Command.() -> Unit = {
        class Args: Arguments() {
            val newZoneId by arg("zone id", ZoneIdConverter())
        }

        name = "set"

        action {
            with(parse(::Args)) {
                val idLong = message.data.authorId.value

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
                        newZoneId?.id != oldZoneId -> {
                            UserDataTable.update({ UserDataTable.userId.eq(idLong) }) {
                                it[UserDataTable.zoneId] = newZoneId?.id
                            }
                        }
                        else -> {}
                    }
                    return@transaction oldZoneId
                }

                message.reply(false) {
                    if(oldZoneId == newZoneId?.id) {
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
    }

    private val zonesCommand: suspend Command.() -> Unit = {
        name = "zones"

        action {
            val zones = ZoneId.getAvailableZoneIds().sorted().joinToString("\n").byteInputStream()
            message.reply(false) {
                addFile("zones.txt", zones)
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