package qbosst.bossbot.database.data

import net.dv8tion.jda.api.entities.Member
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.transactions.transaction
import qbosst.bossbot.config.Config
import qbosst.bossbot.database.tables.GuildUserDataTable
import qbosst.bossbot.util.FixedCache
import qbosst.bossbot.util.Key

data class GuildUserData(
        val experience: Int = 0,
        val message_count: Int = 0,
        val text_chat_time: Long = 0,
        val voice_chat_time: Long = 0
)
{
    companion object
    {
        private val cache = FixedCache<Key, GuildUserData>(Config.Values.DEFAULT_CACHE_SIZE.getInt())
        private val EMPTY = GuildUserData()

        fun get(member: Member): GuildUserData
        {
            val key = Key.Type.USER_GUILD.genKey("", member.idLong, member.guild.idLong)
            if(cache.contains(key))
            {
                return cache.get(key)!!
            }
            else
            {
                val data = transaction {
                    GuildUserDataTable
                            .select { GuildUserDataTable.guild_id.eq(member.guild.idLong) and GuildUserDataTable.user_id.eq(member.idLong) }
                            .fetchSize(1)
                            .map {
                                GuildUserData(
                                        experience = it[GuildUserDataTable.experience],
                                        message_count = it[GuildUserDataTable.message_count],
                                        text_chat_time = it[GuildUserDataTable.text_chat_time],
                                        voice_chat_time = it[GuildUserDataTable.voice_chat_time]
                                )
                            }
                            .singleOrNull()
                } ?: EMPTY
                cache.put(key, data)
                return data
            }
        }

        fun update(guildId: Long, userId: Long, insert: (InsertStatement<Number>) -> Unit, update: (ResultRow, UpdateStatement) -> Unit)
        {
            cache.pull(Key.Type.USER_GUILD.genKey("", userId, guildId))
            transaction {
                val old: ResultRow? = GuildUserDataTable
                        .select { GuildUserDataTable.guild_id.eq(guildId) and GuildUserDataTable.user_id.eq(userId)}
                        .fetchSize(1)
                        .singleOrNull()

                if(old == null)
                {
                    GuildUserDataTable
                            .insert {
                                it[GuildUserDataTable.guild_id] = guildId
                                it[GuildUserDataTable.user_id] = userId
                                insert.invoke(it)
                            }
                }
                else
                {
                    GuildUserDataTable
                            .update ({ GuildUserDataTable.guild_id.eq(guildId) and GuildUserDataTable.user_id.eq(userId) })
                            {
                                update.invoke(old, it)
                            }
                }
            }
        }

        fun update(member: Member, insert: (InsertStatement<Number>) -> Unit, update: (ResultRow, UpdateStatement) -> Unit)
        {
            update(member.guild.idLong, member.idLong, insert, update)
        }
    }
}