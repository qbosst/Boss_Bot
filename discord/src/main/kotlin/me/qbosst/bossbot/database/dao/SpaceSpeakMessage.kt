package me.qbosst.bossbot.database.dao

import me.qbosst.bossbot.database.tables.SpaceSpeakTable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class SpaceSpeakMessage(id: EntityID<Long>): LongEntity(id) {
    companion object: LongEntityClass<SpaceSpeakMessage>(SpaceSpeakTable)

    val messageId: Long get() = id.value
    var userId: Long? by SpaceSpeakTable.userId
}