package me.qbosst.bossbot.database.manager

import me.qbosst.bossbot.database.tables.UserDataTable
import me.qbosst.jda.ext.util.TimeUtil
import net.dv8tion.jda.api.entities.User
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.ZoneId

object UserDataManager: TableManager<Long, UserData>() {

    override fun retrieve(key: Long): UserData = transaction {
        return@transaction UserDataTable
            .select { UserDataTable.userId.eq(key) }
            .singleOrNull()
            ?.let { row ->
                UserData(
                    userId = row[UserDataTable.userId],
                    zoneId = TimeUtil.zoneIdOf(row[UserDataTable.zoneId])
                )
            }
            ?: UserData(key)
    }

    override fun delete(key: Long) {
        transaction {
            UserDataTable.deleteWhere { UserDataTable.userId.eq(key) }
            pull(key)
        }
    }

    fun <T> update(key: Long, column: Column<T>, value: T): T? = transaction {
        pull(key)
        val old = UserDataTable
            .slice(column)
            .select { UserDataTable.userId.eq(key) }
            .singleOrNull()

        if(old == null) {
            UserDataTable.insert {
                it[userId] = key
                it[column] = value
            }
        }
        else {
            UserDataTable.update ({ UserDataTable.userId.eq(key) }) {
                it[column] = value
            }
        }

        return@transaction old?.getOrNull(column)
    }
}

data class UserData(
    val userId: Long,
    val zoneId: ZoneId? = null
)

val User.userData: UserData
    get() = UserDataManager.getOrRetrieve(this.idLong)

