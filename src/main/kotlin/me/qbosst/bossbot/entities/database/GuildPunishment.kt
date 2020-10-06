package me.qbosst.bossbot.entities.database

import me.qbosst.bossbot.database.tables.GuildPunishmentDataTable
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

/**
 *  Class to keep track of member punishment data
 */
data class GuildPunishment (
        val targetId: Long,
        val issuerId: Long,
        val reason: String?,
        val duration: Long,
        val date: Instant,
        val type: Type
) {

    /**
     *  Logs a punishment to the database
     *
     *  @param guild The guild of which the punishment took place
     *  @param transaction The database transaction to use to log the punishment
     */
    fun log(guild: Guild, transaction: Transaction)
    {
        GuildPunishmentDataTable
                .insert {
                    it[guild_id] = guild.idLong
                    it[target_id] = this@GuildPunishment.targetId
                    it[issuer_id] = this@GuildPunishment.issuerId
                    it[reason] = this@GuildPunishment.reason
                    it[duration] = this@GuildPunishment.duration
                    it[date] = this@GuildPunishment.date.epochSecond
                    it[type] = this@GuildPunishment.type.ordinal
                }
                .execute(transaction)
    }

    fun log(guild: Guild)
    {
        transaction { log(guild, this) }
    }

    fun getTarget(guild: Guild): Member?
    {
        return guild.getMemberById(targetId)
    }

    fun getIssuer(guild: Guild): Member?
    {
        return guild.getMemberById(issuerId)
    }

    companion object
    {
        fun create(target: Member, issuer: Member, reason: String?, duration: Long, date: Instant, type: Type): GuildPunishment
        {
            return GuildPunishment(target.idLong, issuer.idLong, reason, duration, date, type)
        }
    }

    enum class Type(val colourRaw: Int, val pastTenseName: String, val displayName: String, val isTimed: Boolean)
    {
        WARN(0xffff00, "warned", "warn", false),
        MUTE(0xffd200, "muted", "mute", true),
        KICK(0xffa500, "kicked", "kick", false),
        TEMP_BAN(0xff5300, "temporarily banned", "temporary ban", true),
        BAN(0xff0000, "banned", "ban", false)
        ;

        companion object
        {
            fun ordinalOf(ordinal: Int): Type?
            {
                return enumValues<Type>().firstOrNull { it.ordinal == ordinal }
            }
        }
    }
}