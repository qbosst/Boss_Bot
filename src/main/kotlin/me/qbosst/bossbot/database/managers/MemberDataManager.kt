package me.qbosst.bossbot.database.managers

import me.qbosst.bossbot.bot.BossBot
import me.qbosst.bossbot.database.tables.MemberDataTable
import net.dv8tion.jda.api.entities.Member
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

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

    fun update(guildId: Long, userId: Long, insert: (InsertStatement<Number>) -> Unit, update: (MemberData, UpdateStatement) -> Unit)
    {
        transaction {
            val old: MemberData? = pull(genKey(guildId, userId)) ?: MemberDataTable
                    .select { MemberDataTable.guild_id.eq(guildId) and MemberDataTable.user_id.eq(userId)}
                    .fetchSize(1)
                    .map { row ->
                        MemberData(
                                experience = row[MemberDataTable.experience],
                                message_count = row[MemberDataTable.message_count],
                                text_chat_time = row[MemberDataTable.text_chat_time],
                                voice_chat_time = row[MemberDataTable.voice_chat_time]
                        )
                    }
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

            val new = MemberDataTable
                    .select { MemberDataTable.guild_id.eq(guildId) and MemberDataTable.user_id.eq(userId) }
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

            onUpdate(guildId, userId, old ?: EMPTY, new)
        }
    }

    fun update(member: Member, insert: (InsertStatement<Number>) -> Unit, update: (MemberData, UpdateStatement) -> Unit) = update(member.guild.idLong, member.idLong, insert, update)

    fun onUpdate(guildId: Long, userId: Long, old: MemberData, new: MemberData)
    {
        val guild = BossBot.SHARDS_MANAGER.getGuildById(guildId)
        val user = BossBot.SHARDS_MANAGER.getUserById(userId)
        BossBot.LOG.debug(
                "Updated stats for " + if (user == null) "User $userId" else "U:${user.asTag} ($userId)" +
                        " in " + if(guild == null) "Guild $guildId " else "G:${guild.name} ($guildId)"
        )
    }

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