package me.qbosst.bossbot.database.data

import me.qbosst.bossbot.config.Config
import me.qbosst.bossbot.database.tables.GuildColoursDataTable
import me.qbosst.bossbot.exception.ReachedMaxAmountException
import me.qbosst.bossbot.util.FixedCache
import net.dv8tion.jda.api.entities.Guild
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.Color as Colour

data class GuildColoursData private constructor(
        private val colours: Map<String, Colour>
)
{
    fun get(name: String): Colour?
    {
        return colours[name]
    }

    fun contains(name: String): Boolean
    {
        return colours.contains(name)
    }

    fun values(): Collection<Colour>
    {
        return colours.values
    }

    fun keySet(): Set<String>
    {
        return colours.keys
    }

    fun clone(): Map<String, Colour>
    {
        return colours
    }

    companion object
    {
        private val cache = FixedCache<Long, GuildColoursData>(Config.Values.DEFAULT_CACHE_SIZE.getInt())
        private val EMPTY = GuildColoursData(mapOf())

        fun get(guild: Guild?): GuildColoursData
        {
            if(guild == null)
            {
                return EMPTY
            }
            if(cache.contains(guild.idLong))
            {
                return cache.get(guild.idLong)!!
            }
            else
            {
                val data = GuildColoursData(
                        transaction
                        {
                            GuildColoursDataTable
                                    .select { GuildColoursDataTable.guild_id.eq(guild.idLong) }
                                    .map {
                                        Pair(it[GuildColoursDataTable.name], Colour(
                                                it[GuildColoursDataTable.red],
                                                it[GuildColoursDataTable.green],
                                                it[GuildColoursDataTable.blue],
                                                it[GuildColoursDataTable.alpha]
                                        ))
                                    }
                                    .toMap()
                        })
                cache.put(guild.idLong, data)
                return data
            }
        }

        fun remove(guild: Guild, name: String): Boolean
        {
            return transaction {
                val contains: Boolean = GuildColoursDataTable
                        .slice(GuildColoursDataTable.name)
                        .select { GuildColoursDataTable.guild_id.eq(guild.idLong) and GuildColoursDataTable.name.eq(name) }
                        .map { true }
                        .firstOrNull() ?: false

                if(contains)
                {
                    GuildColoursDataTable
                            .deleteWhere { GuildColoursDataTable.guild_id.eq(guild.idLong) and GuildColoursDataTable.name.eq(name) }
                    cache.pull(guild.idLong)
                }

                return@transaction contains
            }
        }

        fun update(guild: Guild, name: String, colour: Colour): Colour?
        {
            return transaction {
                val old: Colour? = GuildColoursDataTable
                        .slice(GuildColoursDataTable.red, GuildColoursDataTable.green, GuildColoursDataTable.blue, GuildColoursDataTable.alpha)
                        .select { GuildColoursDataTable.guild_id.eq(guild.idLong) and GuildColoursDataTable.name.eq(name) }
                        .map { Colour(
                                it[GuildColoursDataTable.red],
                                it[GuildColoursDataTable.green],
                                it[GuildColoursDataTable.blue],
                                it[GuildColoursDataTable.green]
                        ) }
                        .firstOrNull()

                if(old != null)
                {
                    GuildColoursDataTable
                            .update ({ GuildColoursDataTable.guild_id.eq(guild.idLong) and GuildColoursDataTable.name.eq(name) })
                            {
                                it[red] = colour.red
                                it[green] = colour.green
                                it[blue] = colour.blue
                                it[alpha] = colour.alpha
                            }
                }
                return@transaction old
            }
        }

        fun add(guild: Guild, name: String, colour: Colour): Boolean
        {
            return transaction {
                val names: List<String> = GuildColoursDataTable
                        .select { GuildColoursDataTable.guild_id.eq(guild.idLong) }
                        .map { it[GuildColoursDataTable.name].toLowerCase() }

                if(names.size > Config.Values.MAX_COLOURS_PER_GUILD.getInt())
                {
                    throw ReachedMaxAmountException("This guild has reached the max amount of colours it can have!")
                }
                else if(!names.contains(name.toLowerCase()))
                {
                    GuildColoursDataTable
                            .insert {
                                it[guild_id] = guild.idLong
                                it[GuildColoursDataTable.name] = name
                                it[red] = colour.red
                                it[green] = colour.green
                                it[blue] = colour.blue
                                it[alpha] = colour.alpha
                            }
                }

                return@transaction !names.contains(name.toLowerCase())
            }
        }
    }
}