package me.qbosst.bossbot.database.managers

import me.qbosst.bossbot.bot.BossBot
import me.qbosst.bossbot.database.tables.MemberDataTable
import net.dv8tion.jda.api.entities.Member
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
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

    /**
     *  Updates a members stats
     *
     *  @param guildId The guild ID of the member
     *  @param userId The user ID of the member
     *  @param update A consumer that gives in the old member data and should return the new member data
     *
     */
    fun update(guildId: Long, userId: Long, update: (MemberData) -> MemberData)
    {
        transaction {
            // Gets the old member data before the update. Tries to pull it from cache before querying the database for it to reduce database calls
            val old: MemberData? = pull(genKey(guildId, userId)) ?: MemberDataTable
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
                    .singleOrNull()

            // Gets the updated member data to replace with
            val updated = update.invoke(old ?: EMPTY)

            when {
                // If nothing changed, return
                updated == old -> return@transaction

                // If the member doesn't have a record, create one and set the values
                old == null ->
                    MemberDataTable.insert {
                        it[guild_id] = guildId
                        it[user_id] = userId
                        it[experience] = updated.experience
                        it[message_count] = updated.message_count
                        it[text_chat_time] = updated.text_chat_time
                        it[voice_chat_time] = updated.voice_chat_time
                    }

                // Else update the values that have changed
                else ->
                    MemberDataTable.update ({ MemberDataTable.guild_id.eq(guildId) and MemberDataTable.user_id.eq(userId) }) {
                        if(old.experience != updated.experience)
                            it[experience] = updated.experience

                        if(old.message_count != updated.message_count)
                            it[message_count] = updated.message_count

                        if(old.text_chat_time != updated.text_chat_time)
                            it[text_chat_time] = updated.text_chat_time

                        if(old.voice_chat_time != updated.voice_chat_time)
                            it[voice_chat_time] = updated.voice_chat_time
                    }
            }

            // Runs method
            onUpdate(guildId, userId, old ?: EMPTY, updated)
        }
    }

    fun update(member: Member, update: (MemberData) -> MemberData) = update(member.guild.idLong, member.idLong, update)

    /**
     *  This method is run when member data has been changed.
     *
     *  @param guildId The guild id of the member
     *  @param userId The user id of the member
     *  @param old The data before the member data update
     *  @param new The data after the member data update
     */
    fun onUpdate(guildId: Long, userId: Long, old: MemberData, new: MemberData)
    {
        // Puts the new value in cache
        putCache(genKey(guildId, userId), new)
        //val guild = BossBot.api.getGuildById(guildId)
        //val user = BossBot.api.getUserById(userId)
    }

    /**
     *  Generates a key to represent a member
     *
     *  @param guildId The guild id of the member
     *  @param userId The user id of the member
     *
     *  @return Pair of Long objects, representing a member
     */
    private fun genKey(guildId: Long, userId: Long): Pair<Long, Long> = Pair(guildId, userId)

    /**
     *  Generates a key to represent a member
     *
     *  @param member The member object
     *
     *  @return Pair of Long objects, representing a member
     */
    private fun genKey(member: Member): Pair<Long, Long> = genKey(member.guild.idLong, member.idLong)

    /**
     *  Holds stats about a certain member
     *
     *  @param experience The experience of the member
     *  @param message_count The amount of messages that the member has sent (that the bot has logged)
     *  @param text_chat_time The amount of estimated time that the user has been chatting on text channels for
     *  @param voice_chat_time The amount of time the member has spent in voice channels
     */
    data class MemberData(
            val experience: Int = 0,
            val message_count: Int = 0,
            val text_chat_time: Long = 0,
            val voice_chat_time: Long = 0
    )
    {
        /**
         *  Clones this class and allows to change stats.
         *
         *  @param experience The new experience. Default is the current experience
         *  @param message_count The new message count. Default is the current message count
         *  @param text_chat_time The new text chat time. Default is the current text chat time.
         *  @param voice_chat_time The new voice chat time. Default is the current voice chat time.
         *
         *  @return New MemberData class
         */
        fun clone(
                experience: Int = this.experience, message_count: Int = this.message_count,
                text_chat_time: Long = this.text_chat_time, voice_chat_time: Long = this.voice_chat_time): MemberData
        {
            return MemberData(experience, message_count, text_chat_time, voice_chat_time)
        }

        override fun equals(other: Any?): Boolean
        {
            if(other !is MemberData)
                return false
            return experience == other.experience &&
                    message_count == other.message_count &&
                    text_chat_time == other.text_chat_time &&
                    voice_chat_time == other.voice_chat_time
        }

        override fun hashCode(): Int
        {
            var result = experience
            result = 31 * result + message_count
            result = 31 * result + text_chat_time.hashCode()
            result = 31 * result + voice_chat_time.hashCode()
            return result
        }

        fun isEmpty(): Boolean = experience == 0 && message_count == 0 && text_chat_time == 0L && voice_chat_time == 0L
    }
}

fun Member.getMemberData(): MemberDataManager.MemberData = MemberDataManager.get(this)