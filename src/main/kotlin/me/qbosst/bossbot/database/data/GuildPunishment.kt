package me.qbosst.bossbot.database.data

import me.qbosst.bossbot.database.tables.GuildPunishmentDataTable
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

data class GuildPunishment (
        val target_id: Long,
        val issuer_id: Long,
        val reason: String?,
        val duration: Long,
        val date: Instant,
        val type: Type
) {

    fun log(guild: Guild, transaction: Transaction)
    {
        GuildPunishmentDataTable
                .insert {
                    it[guild_id] = guild.idLong
                    it[target_id] = this@GuildPunishment.target_id
                    it[issuer_id] = this@GuildPunishment.issuer_id
                    it[reason] = this@GuildPunishment.reason
                    it[duration] = this@GuildPunishment.duration
                    it[date] = this@GuildPunishment.date.epochSecond
                    it[type] = this@GuildPunishment.type.ordinal
                }
                .execute(transaction)
    }

    fun log(guild: Guild)
    {
        transaction {
            log(guild, this)
        }
    }


    fun getTarget(guild: Guild): Member?
    {
        return guild.getMemberById(target_id)
    }

    fun getIssuer(guild: Guild): Member?
    {
        return guild.getMemberById(issuer_id)
    }

    companion object
    {
        fun create(target: Member, issuer: Member, reason: String?, duration: Long, date: Instant, type: Type): GuildPunishment
        {
            return GuildPunishment(target.idLong, issuer.idLong, reason, duration, date, type)
        }
    }

    enum class Type(val pastTenseName: String)
    {
        WARN("warned"),
        MUTE("muted"),
        KICK("kicked"),
        TEMPORARY_BAN("temporarily banned"),
        BAN("banned")
        ;

        companion object
        {
            fun ordinalOf(ordinal: Int): Type?
            {
                return values().firstOrNull { it.ordinal == ordinal }
            }
        }
    }
}