package me.qbosst.bossbot.database.managers

import me.qbosst.bossbot.database.tables.GuildColoursTable
import net.dv8tion.jda.api.entities.Guild
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.Color

object GuildColoursManager: Manager<Long, GuildColoursManager.GuildColours>()
{
    override fun getDatabase(key: Long): GuildColours
    {
        return GuildColours(transaction {
            GuildColoursTable
                    .select { GuildColoursTable.guild_id.eq(key) }
                    .map {
                        Pair(it[GuildColoursTable.name], Color(
                                it[GuildColoursTable.red],
                                it[GuildColoursTable.green],
                                it[GuildColoursTable.blue],
                                it[GuildColoursTable.alpha]
                        ))
                    }
                    .toMap()
        })
    }

    /**
     *  Gets a guild's custom colours
     *  @param guild The guild to get the custom colours of
     *  @return The GuildColoursData object that contains all the guild's custom colour data
     */
    fun get(guild: Guild?): GuildColours = get(guild?.idLong ?: -1)

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
            val contains: Boolean = GuildColoursTable
                    .slice(GuildColoursTable.name)
                    .select { GuildColoursTable.guild_id.eq(guild.idLong) and GuildColoursTable.name.eq(name) }
                    .map { true }
                    .firstOrNull() ?: false

            // If it exists, delete it and invalidate cache
            if(contains)
            {
                GuildColoursTable
                        .deleteWhere { GuildColoursTable.guild_id.eq(guild.idLong) and GuildColoursTable.name.eq(name) }
                pull(guild.idLong)
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
    fun update(guild: Guild, name: String, colour: Color): Color?
    {
        return transaction {

            // Get's the old custom colour
            val old: Color? = GuildColoursTable
                    .slice(GuildColoursTable.red, GuildColoursTable.green, GuildColoursTable.blue, GuildColoursTable.alpha)
                    .select { GuildColoursTable.guild_id.eq(guild.idLong) and GuildColoursTable.name.eq(name) }
                    .map { Color(
                            it[GuildColoursTable.red],
                            it[GuildColoursTable.green],
                            it[GuildColoursTable.blue],
                            it[GuildColoursTable.green]
                    ) }
                    .singleOrNull()

            // If the record exists, update it in the database with the new record and invalidate cache
            if(old != null)
            {
                GuildColoursTable
                        .update ({ GuildColoursTable.guild_id.eq(guild.idLong) and GuildColoursTable.name.eq(name) })
                        {
                            it[red] = colour.red
                            it[green] = colour.green
                            it[blue] = colour.blue
                            it[alpha] = colour.alpha
                        }
                pull(guild.idLong)
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
     *  @return returns whether the colour was added or not.
     */
    fun add(guild: Guild, name: String, colour: Color): Boolean
    {
        return transaction {
            // First checks if the current guild colours are cached, if so save the unnecessary database call,
            // otherwise query the database for all the custom colour names that the guild currently has
            val names = getCached(guild.idLong)?.keySet() ?: GuildColoursTable
                    .select { GuildColoursTable.guild_id.eq(guild.idLong) }
                    .map { it[GuildColoursTable.name].toLowerCase() }

            // If a custom colour does not already exist with this colour, insert it into the database and invalidate cache
            if(!names.contains(name.toLowerCase()))
            {
                GuildColoursTable
                        .insert {
                            it[guild_id] = guild.idLong
                            it[GuildColoursTable.name] = name
                            it[red] = colour.red
                            it[green] = colour.green
                            it[blue] = colour.blue
                            it[alpha] = colour.alpha
                        }
                pull(guild.idLong)
            }

            return@transaction !names.contains(name.toLowerCase())
        }
    }

    fun clear(guild: Guild)
    {
        transaction {
            GuildColoursTable.deleteWhere { GuildColoursTable.guild_id.eq(guild.idLong) }
        }
    }

    data class GuildColours(private val colours: Map<String, Color>)
    {
        /**
         *  Gets a guild custom colour
         *  @param name The name of the custom colour
         *  @return Colour. Null if no colour was found with that name
         */
        fun get(name: String): Color? = colours[name]

        /**
         *  Checks if guild has a custom colour
         *  @param name The name of the custom colour to check
         *  @return if guild has custom colour
         */
        fun contains(name: String): Boolean = colours.contains(name)

        /**
         *  Gets all the guild's custom colours
         *  @return Collection of colours
         */
        fun values(): Collection<Color> = colours.values

        /**
         *  Returns a cloned map of the custom colours
         *  @return map of colours
         */
        fun clone(): Map<String, Color> = colours.toMap()

        /**
         *  Returns all the keys
         *  @return keys
         */
        fun keySet(): Set<String> = colours.keys
    }
}

fun Guild.getColours(): GuildColoursManager.GuildColours = GuildColoursManager.get(this)