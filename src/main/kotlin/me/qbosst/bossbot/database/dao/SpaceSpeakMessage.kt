package me.qbosst.bossbot.database.dao

import me.qbosst.bossbot.database.tables.SpaceSpeakTable
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class SpaceSpeakMessage(id: EntityID<Long>): LongEntity(id) {
    companion object: LongEntityClass<SpaceSpeakMessage>(SpaceSpeakTable)

    val messageId get() = id.value
    var userId by SpaceSpeakTable.userId
    var isAnonymous by SpaceSpeakTable.isAnonymous
    var isPublic by SpaceSpeakTable.isPublic

    override fun toString(): String = "SpaceSpeakMessage(messageId=${messageId},userId=${userId},isAnonymous=${isAnonymous},isPublic=${isPublic})"

}