package me.qbosst.bossbot.extensions

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.authorId
import com.kotlindiscord.kord.extensions.utils.isNullOrBot
import dev.kord.cache.map.MapLikeCollection
import dev.kord.cache.map.lruLinkedHashMap
import dev.kord.core.cache.data.MessageData
import dev.kord.core.event.message.MessageCreateEvent
import me.qbosst.bossbot.database.tables.MemberDataTable
import me.qbosst.bossbot.util.cache.lruLinkedHashMap
import mu.KotlinLogging
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.Duration
import java.time.OffsetDateTime

private const val SECONDS = 60L
private const val EXPERIENCE = 10
private val log = KotlinLogging.logger("Economy Extension")
private typealias ID = Pair<Long, Long>

class EconomyExtension(bot: ExtensibleBot, cacheSize: Int, messageCacheSize: Int = cacheSize): BaseExtension(bot) {
    override val name: String = "economy"

    private var messageCache = MapLikeCollection.lruLinkedHashMap<ID, MemberMessageData>(
        messageCacheSize,
        this.bot.kord
    ) { member, (lastMessageDate, messageCount) ->
        if(lastMessageDate.plusSeconds(SECONDS).isAfter(OffsetDateTime.now())) {
            log.error { "An entry from message cache has been removed without the cooldown for experience being finished. The cache size NEEDS to be increased" }
        } else if(messageCount > 0) {
            member.update {
                return@update copy(messageCount = this.messageCount + messageCount)
            }
        }
    }

    private var dataCache = MapLikeCollection.lruLinkedHashMap<ID, MemberData>(cacheSize)

    override suspend fun setup() {
        event<MessageCreateEvent> {
            check(
                { event -> event.guildId != null },
                { event -> event.message.author.isNullOrBot().not() }
            )
            action {
                val key = event.message.data.dbId

                val now = OffsetDateTime.now()
                val memberData = messageCache.get(key)
                    ?: MemberMessageData(now, 0).also { data -> messageCache.put(key, data) }
                memberData.messageCount+=1

                if(event.message.content.startsWith(this@EconomyExtension.bot.prefix)) {
                    return@action
                }

                if(memberData.lastMessageDate.plusSeconds(SECONDS).isBefore(now)) {
                    key.update {
                        copy(
                            experience = this.experience + EXPERIENCE,
                            textChatTime = this.textChatTime + SECONDS,
                            messageCount = this.messageCount + memberData.messageCount
                        )
                    }
                    memberData.apply {
                        lastMessageDate = now
                        messageCount = 0
                    }
                }
            }
        }
    }

    private suspend fun ID.update(body: MemberData.() -> MemberData) = newSuspendedTransaction {
        val (guildId, userId) = this@update
        val old = this@update.getData(this)
        val new = body.invoke(old ?: MemberData.EMPTY)

        when {
            new == old -> return@newSuspendedTransaction

            old == null -> {
                MemberDataTable.insert {
                    it[MemberDataTable.guildId] = guildId
                    it[MemberDataTable.userId] = userId
                    it[experience] = new.experience
                    it[messageCount] = new.messageCount
                    it[textChatTime] = new.textChatTime
                    it[voiceChatTime] = new.voiceChatTime
                }
            }

            else -> {
                dataCache.remove(this@update)
                MemberDataTable.update ({ MemberDataTable.guildId.eq(guildId) and MemberDataTable.userId.eq(userId) }) {
                    if(old.experience != new.experience) {
                        it[experience] = new.experience
                    }

                    if(old.messageCount != new.messageCount) {
                        it[messageCount] = new.messageCount
                    }

                    if(old.textChatTime != new.textChatTime) {
                        it[textChatTime] = new.textChatTime
                    }

                    if(old.voiceChatTime != new.voiceChatTime) {
                        it[voiceChatTime] = new.voiceChatTime
                    }
                }
            }
        }
    }

    private suspend fun ID.getData(transaction: Transaction): MemberData? {
        if(dataCache.contains(this)) {
            return dataCache.get(this)!!
        }

        val (guildId, userId) = this

        val data = with(transaction) {
            MemberDataTable
                .select { MemberDataTable.guildId.eq(guildId) and MemberDataTable.userId.eq(userId) }
                .singleOrNull()
                ?.let { row ->
                    MemberData(
                        experience = row[MemberDataTable.experience],
                        messageCount = row[MemberDataTable.messageCount],
                        voiceChatTime = row[MemberDataTable.voiceChatTime],
                        textChatTime = row[MemberDataTable.textChatTime]
                    )
                }
        }

        dataCache.put(this, data ?: MemberData.EMPTY)
        return data
    }

    private val MessageData.dbId: ID get() = guildId.value!!.value to this.author.id.value
}

private data class MemberMessageData(var lastMessageDate: OffsetDateTime, var messageCount: Int)

private class MemberVoiceData(val join: OffsetDateTime, isMuted: Boolean) {
    var mute: OffsetDateTime? = if(isMuted) join else null
        set(value) {
            field?.let { date -> secondsMuted += Duration.between(date, OffsetDateTime.now()).seconds }
            field = value
        }

    var secondsMuted: Long = 0
        private set

    val voiceChatTime: Duration get() = Duration.between(join, OffsetDateTime.now())
}

private data class MemberData(
    val experience: Int = 0,
    val messageCount: Int = 0,
    val voiceChatTime: Long = 0,
    val textChatTime: Long = 0
) {
    companion object {
        val EMPTY = MemberData()
    }
}