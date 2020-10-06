package me.qbosst.bossbot.entities.database

import me.qbosst.bossbot.config.Config
import me.qbosst.bossbot.database.tables.GuildColoursDataTable
import me.qbosst.bossbot.exception.ReachedMaxAmountException
import me.qbosst.bossbot.util.FixedCache
import net.dv8tion.jda.api.entities.Guild
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.Color as Colour

/**
 *  Class used to store data about a guild's custom colours
 */
data class GuildColoursData private constructor(
        private val colours: Map<String, Colour>
)
{
    /**
     *  Gets a guild custom colour
     *
     *  @param name The name of the custom colour
     *
     *  @return Colour. Null if no colour was found with that name
     */
    fun get(name: String): Colour?
    {
        return colours[name]
    }

    /**
     *  Checks if guild has a custom colour
     *
     *  @param name The name of the custom colour to check
     *
     *  @return if guild has custom colour
     */
    fun contains(name: String): Boolean
    {
        return colours.contains(name)
    }

    /**
     *  Gets all the guild's custom colours
     *
     *  @return Collection of colours
     */
    fun values(): Collection<Colour>
    {
        return colours.values
    }

    /**
     *  Returns a cloned map of the custom colours
     *
     *  @return map of colours
     */
    fun clone(): Map<String, Colour>
    {
        return colours.toMap()
    }

    companion object
    {
        private val cache = FixedCache<Long, GuildColoursData>(Config.Values.DEFAULT_CACHE_SIZE.getIntOrDefault())
        private val EMPTY = GuildColoursData(mapOf())

        /**
         *  Gets a guild's custom colours
         *
         *  @param guild The guild to get the custom colours of
         *
         *  @return The GuildColoursData object that contains all the guild's custom colour data
         */
        fun get(guild: Guild?): GuildColoursData
        {
            if(guild == null)
                return EMPTY
            if(cache.contains(guild.idLong))
                return cache.get(guild.idLong)!!
            else
            {
                // Retrieves the guild's custom colours from the database
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
                // Caches it and returns it
                cache.put(guild.idLong, data)
                return data
            }
        }

        /**
         *  Removes a custom colour from a guild
         *
         *  @param guild The guild that the custom colour should be removed from
         *  @param name The name of the colour that should be removed
         *
         *  @return if the colour was successfully removed
         */
        fun remove(guild: Guild, name: String): Boolean
        {
            // Removes a custom colour from a guild
            return transaction {

                // Checks if the record exists
                val contains: Boolean = GuildColoursDataTable
                        .slice(GuildColoursDataTable.name)
                        .select { GuildColoursDataTable.guild_id.eq(guild.idLong) and GuildColoursDataTable.name.eq(name) }
                        .map { true }
                        .firstOrNull() ?: false

                // If it exists, delete it and invalidate cache
                if(contains)
                {
                    GuildColoursDataTable
                            .deleteWhere { GuildColoursDataTable.guild_id.eq(guild.idLong) and GuildColoursDataTable.name.eq(name) }
                    cache.pull(guild.idLong)
                }

                // Return whether the record was deleted
                return@transaction contains
            }
        }

        /**
         *  Updates an already existing colour
         *
         *  @param guild The guild to update the custom colour of
         *  @param name The name of the custom colour to update
         *  @param colour The colour to update it with
         *
         *  @return The old colour that was stored in the database. Null if no record was updated.
         */
        fun update(guild: Guild, name: String, colour: Colour): Colour?
        {
            return transaction {

                // Get's the old custom colour
                val old: Colour? = GuildColoursDataTable
                        .slice(GuildColoursDataTable.red, GuildColoursDataTable.green, GuildColoursDataTable.blue, GuildColoursDataTable.alpha)
                        .select { GuildColoursDataTable.guild_id.eq(guild.idLong) and GuildColoursDataTable.name.eq(name) }
                        .map { Colour(
                                it[GuildColoursDataTable.red],
                                it[GuildColoursDataTable.green],
                                it[GuildColoursDataTable.blue],
                                it[GuildColoursDataTable.green]
                        ) }
                        .singleOrNull()

                // If the record exists, update it in the database with the new record and invalidate cache
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
                    cache.pull(guild.idLong)
                }
                return@transaction old
            }
        }

        /**
         *  Adds a new custom colour to a guild
         *
         *  @param guild the guild to add the custom colour to
         *  @param name the name of the custom colour
         *  @param colour the colour
         *
         *  @throws ReachedMaxAmountException Throws this exception if the guild has reached the maximum amount of colours it can have
         *
         *  @return returns whether the colour was added or not.
         */
        fun add(guild: Guild, name: String, colour: Colour): Boolean
        {
            return transaction {
                // First checks if the current guild colours are cached, if so save the unnecessary database call,
                // otherwise query the database for all the custom colour names that the guild currently has
                val names = cache.get(guild.idLong)?.colours?.keys ?: GuildColoursDataTable
                        .select { GuildColoursDataTable.guild_id.eq(guild.idLong) }
                        .map { it[GuildColoursDataTable.name].toLowerCase() }

                // Checks if guild has reached the maximum amount of colours it can have
                if(names.size > 100)
                    throw ReachedMaxAmountException("This guild has reached the max amount of colours it can have!")

                // If a custom colour does not already exist with this colour, insert it into the database and invalidate cache
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
                    cache.pull(guild.idLong)
                }

                return@transaction !names.contains(name.toLowerCase())
            }
        }
    }
}