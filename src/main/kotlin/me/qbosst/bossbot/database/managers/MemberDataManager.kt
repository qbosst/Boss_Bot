package me.qbosst.bossbot.database.managers

import me.qbosst.bossbot.database.tables.MemberDataTable
import net.dv8tion.jda.api.entities.Member
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.transactions.transaction

object MemberDataManager: Manager<Pair<Long, Long>, MemberDataManager.MemberData>()
{

    private val EMPTY = MemberData()

    override fun getDatabase(key: Pair<Long, Long>): MemberData
    {
        return transaction {
            MemberDataTable
                    .select { MemberDataTable.guild_id.eq(key.first) and MemberDataTable.user_id.eq(key.second) }
                    .fetchSize(1)
                    .map { row ->
                        MemberData(
                                experience = row[MemberDataTable.experience],
                                message_count = row[MemberDataTable.message_count],
                                text_chat_time = row[MemberDataTable.text_chat_time],
                                voice_chat_time = row[MemberDataTable.voice_chat_time]
                        )
                    }
                    .singleOrNull() ?: EMPTY
        }
    }

    fun get(member: Member) = get(genKey(member))

    fun get(guildId: Long, userId: Long) = get(genKey(guildId, userId))

    fun update(guildId: Long, userId: Long, insert: (InsertStatement<Number>) -> Unit, update: (ResultRow, UpdateStatement) -> Unit)
    {
        pull(genKey(guildId, userId))
        transaction {
            val old: ResultRow? = MemberDataTable
                    .select { MemberDataTable.guild_id.eq(guildId) and MemberDataTable.user_id.eq(userId)}
                    .fetchSize(1)
                    .singleOrNull()

            if(old == null)
                MemberDataTable
                        .insert {
                            it[MemberDataTable.guild_id] = guildId
                            it[MemberDataTable.user_id] = userId
                            insert.invoke(it)
                        }
            else
                MemberDataTable
                        .update ({ MemberDataTable.guild_id.eq(guildId) and MemberDataTable.user_id.eq(userId) })
                        {
                            update.invoke(old, it)
                        }
        }
    }

    fun update(member: Member, insert: (InsertStatement<Number>) -> Unit, update: (ResultRow, UpdateStatement) -> Unit) = update(member.guild.idLong, member.idLong, insert, update)

    private fun genKey(guildId: Long, userId: Long): Pair<Long, Long> = Pair(guildId, userId)

    private fun genKey(member: Member): Pair<Long, Long> = genKey(member.guild.idLong, member.idLong)

    data class MemberData(
            val experience: Int = 0,
            val message_count: Int = 0,
            val text_chat_time: Long = 0,
            val voice_chat_time: Long = 0
    )
}

fun Member.getMemberData(): MemberDataManager.MemberData = MemberDataManager.get(this)