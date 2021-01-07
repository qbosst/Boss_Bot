package me.qbosst.bossbot.database.manager

import me.qbosst.bossbot.database.tables.MemberDataTable
import net.dv8tion.jda.api.entities.Member
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object MemberDataManager: TableManager<Pair<Long, Long>, MemberData>() {

    override fun retrieve(key: Pair<Long, Long>): MemberData = transaction {
        val (guildId, userId) = key

        return@transaction MemberDataTable
            .select { MemberDataTable.guildId.eq(guildId) and MemberDataTable.userId.eq(userId) }
            .singleOrNull()
            ?.let { row ->
                MemberData(
                    experience = row[MemberDataTable.experience],
                    messageCount = row[MemberDataTable.messageCount],
                    textChatTime = row[MemberDataTable.textChatTime],
                    voiceChatTime = row[MemberDataTable.voiceChatTime]
                )
            }
            ?: MemberData()
    }

    override fun delete(key: Pair<Long, Long>) {
        val (guildId, userId) = key
        MemberDataTable.deleteWhere { MemberDataTable.guildId.eq(guildId) and MemberDataTable.userId.eq(userId) }
    }

    fun update(guildId: Long, userId: Long, update: (MemberData) -> MemberData) = transaction {
        val key = genKey(guildId, userId)
        val old: MemberData? = get(key) ?: MemberDataTable
            .select { MemberDataTable.guildId.eq(guildId) and MemberDataTable.userId.eq(userId) }
            .singleOrNull()
            ?.let { row ->
                MemberData(
                    experience = row[MemberDataTable.experience],
                    messageCount = row[MemberDataTable.messageCount],
                    textChatTime = row[MemberDataTable.textChatTime],
                    voiceChatTime = row[MemberDataTable.voiceChatTime]
                )
            }

        val new = update.invoke(old ?: MemberData())

        when {
            // no changes, so not point updating / inserting
            new == old -> return@transaction

            // record doesnt exist, so we create one
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
            // update record
            else -> {
                pull(key)
                MemberDataTable.update ({ MemberDataTable.guildId.eq(guildId) and MemberDataTable.userId.eq(userId) }) {
                    if(old.experience != new.experience)
                        it[experience] = new.experience

                    if(old.messageCount != new.messageCount)
                        it[messageCount] = new.messageCount

                    if(old.textChatTime != new.textChatTime)
                        it[textChatTime] = new.textChatTime

                    if(old.voiceChatTime != new.voiceChatTime)
                        it[voiceChatTime] = new.voiceChatTime
                }
            }
        }
    }

    fun update(member: Member, update: (MemberData) -> MemberData) = update(member.guild.idLong, member.idLong, update)

    fun getOrRetrieve(member: Member) = getOrRetrieve(member.key())

    fun getOrRetrieve(guildId: Long, userId: Long) = getOrRetrieve(genKey(guildId, userId))
}

private fun genKey(guildId: Long, userId: Long) = Pair(guildId, userId)

private fun Member.key() = genKey(guild.idLong, idLong)

data class MemberData(
    val experience: Int = 0,
    val messageCount: Int = 0,
    val textChatTime: Long = 0,
    val voiceChatTime: Long = 0
)

val Member.memberData: MemberData
    get() = MemberDataManager.getOrRetrieve(this)