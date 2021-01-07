package me.qbosst.bossbot.database.manager

import me.qbosst.bossbot.database.tables.UserSpaceMessagesTable
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

object UserSpaceMessagesManager: TableManager<Long, List<Long>>() {

    override fun delete(key: Long) {
        transaction {
            UserSpaceMessagesTable.deleteWhere { UserSpaceMessagesTable.userId.eq(key) }
        }
    }

    override fun retrieve(key: Long): List<Long> = transaction {
        UserSpaceMessagesTable
            .select { UserSpaceMessagesTable.userId.eq(key) }
            .map { row -> row[UserSpaceMessagesTable.messageId] }
    }

    fun addMessageId(userId: Long, messageId: Long) = transaction {
        UserSpaceMessagesTable.insert {
            it[UserSpaceMessagesTable.userId] = userId
            it[UserSpaceMessagesTable.messageId] = messageId
        }
        pull(userId)
    }


}