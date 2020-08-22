package me.qbosst.bossbot.database.tables

import me.qbosst.bossbot.database.data.GuildPunishment
import net.dv8tion.jda.api.entities.Member
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

object GuildPunishmentDataTable : Table() {

    const val max_reason_length = 512

    val guild_id = long("GUILD_ID").default(0L)
    val target_id = long("TARGET_ID").default(0L)
    val issuer_id = long("ISSUER_ID").default(0L)

    val reason = varchar("MAX_REASON_LENGTH", max_reason_length).nullable()
    val duration = long("DURATION").default(0L)
    val date = long("DATE").default(Instant.EPOCH.epochSecond)
    val type = integer("TYPE").default(-1)

    override val primaryKey: PrimaryKey?
        get() = PrimaryKey(guild_id, target_id, issuer_id, type, date)

    override val tableName: String
        get() = "GUILD_USER_PUNISHMENT_HISTORY"

    fun getHistory(guild_id: Long, user_id: Long, reason: String = ""): List<GuildPunishment>
    {
        return transaction {
            val search: Op<Boolean> = Op.build { GuildPunishmentDataTable.guild_id.eq(guild_id) and target_id.eq(user_id)}
            slice(target_id, issuer_id, GuildPunishmentDataTable.reason, GuildPunishmentDataTable.duration, date, type)
                    .select { if(!reason.isNullOrEmpty()) search and Op.build { GuildPunishmentDataTable.reason.eq(reason) } else search }
                    .map {
                        GuildPunishment(
                                target_id = it[target_id],
                                issuer_id = it[issuer_id],
                                reason = it[GuildPunishmentDataTable.reason],
                                duration = it[GuildPunishmentDataTable.duration],
                                date = Instant.ofEpochSecond(it[date]),
                                type = GuildPunishment.Type.ordinalOf(it[type])!!
                        )
                    }
        }
    }

    fun getHistory(member: Member, reason: String = ""): List<GuildPunishment>
    {
        return getHistory(member.guild.idLong, member.idLong, reason)
    }
}