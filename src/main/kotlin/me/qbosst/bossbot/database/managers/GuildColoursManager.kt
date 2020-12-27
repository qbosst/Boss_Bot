package me.qbosst.bossbot.database.managers

import me.qbosst.bossbot.database.tables.GuildColoursTable
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.Color

object GuildColoursManager: TableManager<Long, GuildColoursManager.GuildColours>()
{
    override fun retrieve(key: Long): GuildColours = transaction {
        GuildColours(
                GuildColoursTable
                        .select { GuildColoursTable.guildId.eq(key) }
                        .map { row -> Pair(
                                row[GuildColoursTable.name],
                                Color(
                                        row[GuildColoursTable.red],
                                        row[GuildColoursTable.green],
                                        row[GuildColoursTable.blue],
                                        row[GuildColoursTable.alpha]
                                )) }
                        .toMap()
        )
    }

    /**
     *  Gets a guild's custom colours
     *  @param guild The guild to get the custom colours of
     *  @return The GuildColoursData object that contains all the guild's custom colour data
     */
    fun get(guild: Guild?): GuildColours = getOrRetrieve(guild?.idLong ?: -1)

    /**
     *  Removes a custom colour from a guild
     *
     *  @param guild The guild that the custom colour should be removed from
     *  @param name The name of the colour that should be removed
     *
     *  @return if the colour was successfully removed
     */
    fun remove(guild: Guild, name: String): Boolean = transaction {
        val key = guild.idLong
        val exists = if(isCached(key)) (name in get(key)!!) else GuildColoursTable
                .slice(GuildColoursTable.name)
                .select { GuildColoursTable.guildId.eq(key) and GuildColoursTable.name.eq(name) }
                .map { true }.singleOrNull() ?: false

        if(exists)
            GuildColoursTable
                    .deleteWhere { GuildColoursTable.guildId.eq(key) and GuildColoursTable.name.eq(name) }
                    .also { pull(key) }

        return@transaction exists
    }

    /**
     *  Updates an already existing colour
     *
     *  @param guild The guild to update the custom colour of
     *  @param name The name of the custom colour to update
     *  @param new The colour to update it with
     *
     *  @return The old colour that was stored in the database. Null if no record was updated.
     */
    fun update(guild: Guild, name: String, new: Color): Color? = transaction {
        val key = guild.idLong
        val old: Color? = if(isCached(key)) get(key)!![name] else GuildColoursTable
                .slice(GuildColoursTable.red, GuildColoursTable.green, GuildColoursTable.blue, GuildColoursTable.alpha)
                .select { GuildColoursTable.guildId.eq(key) and GuildColoursTable.name.eq(name) }
                .map { row -> Color(
                        row[GuildColoursTable.red],
                        row[GuildColoursTable.green],
                        row[GuildColoursTable.blue],
                        row[GuildColoursTable.alpha]
                ) }
                .singleOrNull()

        if(old != null && old != new)
            GuildColoursTable
                    .update ({ GuildColoursTable.guildId.eq(key) and GuildColoursTable.name.eq(name) }) {
                        it[red] = new.red
                        it[green] = new.green
                        it[blue] = new.blue
                        it[alpha] = new.alpha
                    }
                    .also { pull(key) }

        return@transaction old
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
    fun add(guild: Guild, name: String, colour: Color): Boolean = transaction {
        val key = guild.idLong
        val exists: Boolean = if(isCached(key)) (name in get(key)!!) else GuildColoursTable
                .slice(GuildColoursTable.name)
                .select { GuildColoursTable.guildId.eq(key) and GuildColoursTable.name.eq(name) }
                .map { true }.singleOrNull() ?: false

        if(!exists)
            GuildColoursTable
                    .insert {
                        it[guildId] = key
                        it[GuildColoursTable.name] = name
                        it[red] = colour.red
                        it[green] = colour.green
                        it[blue] = colour.blue
                        it[alpha] = colour.alpha
                    }
                    .also { pull(key) }

        return@transaction !exists
    }

    fun clear(guild: Guild) = transaction {
        GuildColoursTable
                .deleteWhere { GuildColoursTable.guildId.eq(guild.idLong) }
                .also { pull(guild.idLong) }
    }

    fun Guild.getColours(): GuildColours = getOrRetrieve(idLong)

    fun MessageReceivedEvent.getColours(): GuildColours = if(isFromGuild) guild.getColours() else GuildColours(mapOf())

    data class GuildColours(val colours: Map<String, Color>)
    {
        /**
         *  Gets a guild custom colour
         *  @param name The name of the custom colour
         *  @return Colour. Null if no colour was found with that name
         */
        operator fun get(name: String): Color? = colours[name]

        /**
         *  Checks if guild has a custom colour
         *  @param name The name of the custom colour to check
         *  @return if guild has custom colour
         */
        operator fun contains(name: String): Boolean = colours.containsKey(name)

        /**
         *  Gets all the guild's custom colours
         *  @return Collection of colours
         */
        val values: Collection<Color>
            get() = colours.values

        val keySet: Set<String>
            get() = colours.keys
    }
}