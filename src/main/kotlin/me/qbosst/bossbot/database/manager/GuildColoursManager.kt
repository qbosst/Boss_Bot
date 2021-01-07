package me.qbosst.bossbot.database.manager

import me.qbosst.bossbot.database.tables.GuildColoursTable
import net.dv8tion.jda.api.entities.Guild
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.Color as Colour

object GuildColoursManager: TableManager<Long, GuildColours>() {

    override fun retrieve(key: Long): GuildColours = transaction {
        val colours = GuildColoursTable
            .select { GuildColoursTable.guildId.eq(key) }
            .map { row -> Pair(row[GuildColoursTable.name], Colour(row[GuildColoursTable.value], true)) }
            .toMap()

        return@transaction GuildColours(colours)
    }

    override fun delete(key: Long) {
        transaction {
            GuildColoursTable.deleteWhere { GuildColoursTable.guildId.eq(key) }
            pull(key)
        }
    }

    fun delete(key: Long, name: String) {
        transaction {
            GuildColoursTable.deleteWhere { GuildColoursTable.guildId.eq(key) and GuildColoursTable.name.eq(name) }
            pull(key)
        }
    }

    fun create(key: Long, name: String, colour: Colour): Boolean = transaction {

        val old = if(contains(key)) get(key)!![name] else GuildColoursTable
            .select { GuildColoursTable.guildId.eq(key) and GuildColoursTable.name.eq(name) }
            .singleOrNull()
            ?.let { row -> Colour(row[GuildColoursTable.value]) }

        if(old == null) {
            GuildColoursTable.insert {
                it[GuildColoursTable.guildId] = key
                it[GuildColoursTable.name] = name
                it[GuildColoursTable.value] = colour.rgb
            }
            pull(key)
        }

        return@transaction old == null
    }

    fun update(key: Long, name: String, colour: Colour): Colour? = transaction {

        val old = if(contains(key)) get(key)!![name] else GuildColoursTable
            .select { GuildColoursTable.guildId.eq(key) and GuildColoursTable.name.eq(name) }
            .singleOrNull()
            ?.let { row -> Colour(row[GuildColoursTable.value]) }

        if(old != null) {
            GuildColoursTable.update ({ GuildColoursTable.guildId.eq(key) and GuildColoursTable.name.eq(name) }) {
                it[GuildColoursTable.value] = colour.rgb
            }
            pull(key)
        }

        return@transaction old
    }

}

data class GuildColours(val map: Map<String, Colour>) {
    operator fun contains(colour: String) = colour in map
    operator fun get(colour: String) = map[colour]

    val size: Int
        get() = map.size
}

val Guild.colours: GuildColours
    get() = GuildColoursManager.getOrRetrieve(this.idLong)