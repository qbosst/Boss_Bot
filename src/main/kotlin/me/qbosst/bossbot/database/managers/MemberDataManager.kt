package me.qbosst.bossbot.database.managers

import me.qbosst.bossbot.database.tables.MemberDataTable
import net.dv8tion.jda.api.entities.Member
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

object MemberDataManager: TableManager<Pair<Long, Long>, MemberDataManager.MemberData>()
{
    override fun retrieve(key: Pair<Long, Long>): MemberData = transaction {
        MemberDataTable
                .select { MemberDataTable.guildId.eq(key.first) and MemberDataTable.userId.eq(key.second) }
                .fetchSize(1)
                .map { row ->
                    MemberData(
                            experience = row[MemberDataTable.experience],
                            message_count = row[MemberDataTable.messageCount],
                            text_chat_time = row[MemberDataTable.textChatTime],
                            voice_chat_time = row[MemberDataTable.voiceChatTime]
                    )
                }
                .singleOrNull() ?: MemberData()
    }

    fun get(member: Member) = getOrRetrieve(genKey(member))

    fun get(guildId: Long, userId: Long) = getOrRetrieve(genKey(guildId, userId))

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
                    .select { MemberDataTable.guildId.eq(guildId) and MemberDataTable.userId.eq(userId) }
                    .fetchSize(1)
                    .map { row ->
                        MemberData(
                                experience = row[MemberDataTable.experience],
                                message_count = row[MemberDataTable.messageCount],
                                text_chat_time = row[MemberDataTable.textChatTime],
                                voice_chat_time = row[MemberDataTable.voiceChatTime]
                        )
                    }
                    .singleOrNull()

            // Gets the updated member data to replace with
            val updated = update.invoke(old ?: MemberData())

            when {
                // If nothing changed, return
                updated == old -> return@transaction

                // If the member doesn't have a record, create one and set the values
                old == null ->
                    MemberDataTable.insert {
                        it[this.guildId] = guildId
                        it[this.userId] = userId
                        it[experience] = updated.experience
                        it[messageCount] = updated.message_count
                        it[textChatTime] = updated.text_chat_time
                        it[voiceChatTime] = updated.voice_chat_time
                    }

                // Else update the values that have changed
                else ->
                    MemberDataTable.update ({ MemberDataTable.guildId.eq(guildId) and MemberDataTable.userId.eq(userId) }) {
                        if(old.experience != updated.experience)
                            it[experience] = updated.experience

                        if(old.message_count != updated.message_count)
                            it[messageCount] = updated.message_count

                        if(old.text_chat_time != updated.text_chat_time)
                            it[textChatTime] = updated.text_chat_time

                        if(old.voice_chat_time != updated.voice_chat_time)
                            it[voiceChatTime] = updated.voice_chat_time
                    }
            }

            // Runs method
            onUpdate(guildId, userId, old ?: MemberData(), updated)
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
}

fun Member.getMemberData(): MemberDataManager.MemberData = MemberDataManager.get(this)