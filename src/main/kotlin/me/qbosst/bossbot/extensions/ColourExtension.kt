package me.qbosst.bossbot.extensions

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.GroupCommand
import com.kotlindiscord.kord.extensions.commands.MessageCommand
import com.kotlindiscord.kord.extensions.commands.converters.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.hasPermission
import dev.kord.cache.map.MapLikeCollection
import dev.kord.cache.map.lruLinkedHashMap
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.MessageBehavior
import me.qbosst.bossbot.converters.coalescedColour
import me.qbosst.bossbot.converters.colourList
import me.qbosst.bossbot.converters.maxLengthString
import me.qbosst.bossbot.database.tables.GuildColoursTable
import me.qbosst.bossbot.util.ColourUtil
import me.qbosst.bossbot.util.ColourUtil.blend
import me.qbosst.bossbot.util.ColourUtil.nextColour
import me.qbosst.bossbot.util.ext.insertOrIgnore
import me.qbosst.bossbot.util.ext.reply
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import kotlin.random.Random
import java.awt.Color as Colour

class ColourExtension(bot: ExtensibleBot, cacheSize: Int): Extension(bot) {
    private val cache = MapLikeCollection.lruLinkedHashMap<Long, Map<String, Colour>>(cacheSize)

    override val name: String = "colour"

    override suspend fun setup() {
        group(colourGroup)
    }

    private val colourGroup: suspend GroupCommand.() -> Unit = {
        class Args: Arguments() {
            val colour by coalescedColour(
                displayName = "colour",
                description = "The colour to display",
                colourProvider = buildConverter(includeDefault = true, includeGuild = true),
                shouldThrow = true
            )
        }
        name = "colour"

        signature(::Args)
        action {
            with(parse(::Args)) {
                message.replyColourEmbed(colour)
            }
        }

        command(randomCommand)
        command(blendCommand)
        command(createCommand)
        command(updateCommand)
        command(removeCommand)
    }

    private val randomCommand: suspend MessageCommand.() -> Unit = {
        class Args: Arguments() {
            val isAlpha by defaultingBoolean("isAlpha", "Whether a colour should have a random opacity", false)
        }

        name = "random"

        signature(::Args)
        action {
            with(parse(::Args)) {
                val colour = Random.nextColour(isAlpha)
                message.replyColourEmbed(colour)
            }
        }
    }

    private val blendCommand: suspend MessageCommand.() -> Unit = {
        class Args: Arguments() {
            val colours by colourList(
                displayName = "colours",
                description = "The colours to blend together",
                colourProvider = buildConverter(includeDefault = true, includeGuild = true),
                required = true
            )
        }

        name = "blend"

        signature(::Args)
        action {
            with(parse(::Args)) {
                val colour = colours.blend()
                message.replyColourEmbed(colour)
            }
        }
    }

    private val createCommand: suspend MessageCommand.() -> Unit = {
        class Args: Arguments() {
            val name by maxLengthString("name", "The name of the colour", GuildColoursTable.MAX_COLOUR_NAME_LENGTH)
            val colour by coalescedColour("colour", "The colour to create", shouldThrow = true)
        }

        name = "create"
        description = "Creates a guild-colour"

        check { event -> event.guildId != null }
        check { event -> event.member!!.hasPermission(Permission.ManageEmojis) }

        signature(::Args)
        action {
            with(parse(::Args)) {
                val guildId = guild!!.id.value

                val isInserted = transaction {
                    GuildColoursTable.insertOrIgnore {
                        it[GuildColoursTable.guildId] = guildId
                        it[GuildColoursTable.name] = this@with.name
                        it[GuildColoursTable.value] = this@with.colour.rgb
                    }
                }

                message.reply(false) {
                    if(isInserted) {
                        content = "inserted"
                        cache.remove(guildId)
                    } else {
                        content = "colour with this name already exists"
                    }
                }
            }
        }
    }

    private val removeCommand: suspend MessageCommand.() -> Unit = {
        name = "remove"
        description = "Removes a guild-colour"
        aliases = arrayOf("delete")

        val parser = object: Arguments() {
            val name by maxLengthString("colour name", "The name of the colour to remove", GuildColoursTable.MAX_COLOUR_NAME_LENGTH)
        }

        check { event -> event.guildId != null }
        check { event -> event.member!!.hasPermission(Permission.ManageEmojis) }

        signature { parser }
        action {
            with(parse { parser }) {
                val guildId = guild!!.id.value

                // deletes records & gives the amount of records deleted
                val deleted = transaction {
                    GuildColoursTable.deleteWhere {
                        GuildColoursTable.guildId.eq(guildId) and GuildColoursTable.name.eq(name)
                    }
                }

                message.reply(false) {
                    if(deleted == 0) {
                        content = "Could not delete"
                    } else {
                        content = "Successfully deleted"
                        cache.remove(guildId)
                    }
                }
            }
        }
    }

    private val updateCommand: suspend MessageCommand.() -> Unit = {
        name = "update"

        check { event -> event.guildId != null }
        check { event -> event.member!!.hasPermission(Permission.ManageEmojis) }

        val parser = object: Arguments() {
            val name by maxLengthString("name", "The name of the colour to update", GuildColoursTable.MAX_COLOUR_NAME_LENGTH)
            val colour by coalescedColour("new colour", "The colour to update the name with", shouldThrow = true)
        }

        signature { parser }
        action {
            with(parse { parser }) {
                val guildId = guild!!.id.value

                // updates records & gives the amount of records updated
                val updated = transaction {
                    GuildColoursTable.update(
                        { GuildColoursTable.guildId.eq(guildId) and GuildColoursTable.name.eq(name) }
                    ) {
                        it[GuildColoursTable.value] = colour.rgb
                    }
                }

                message.reply(false) {
                    if(updated == 0) {
                        content = "Could not update"
                    } else {
                        content = "Updated colour"
                        cache.remove(guildId)
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun buildConverter(
        includeDefault: Boolean,
        includeGuild: Boolean
    ): suspend (CommandContext) -> Map<String, Colour> = { ctx ->
        buildMap {
            if(includeDefault) {
                putAll(ColourUtil.systemColours)
            }

            if(includeGuild) {
                val guildId = ctx.getGuild()?.id?.value
                if(guildId != null) {
                    val guildColours = getColoursById(guildId)
                    putAll(guildColours)
                }
            }
        }
    }

    private suspend fun getColoursById(id: Long): Map<String, Colour> {
        // return if colours in cache
        if(cache.contains(id)) {
            return cache.get(id)!!
        }

        // make database call
        val colours = transaction {
            GuildColoursTable
                .select { GuildColoursTable.guildId.eq(id) }.asSequence()
                .map { row -> row[GuildColoursTable.name] to Colour(row[GuildColoursTable.value], true) }
                .toMap()
        }

        // cache and return result
        cache.put(id, colours)
        return colours
    }

    private suspend fun MessageBehavior.replyColourEmbed(colour: Colour) =
        reply(false) {
            ColourUtil.buildColourEmbed(this, colour, "colour.png")
        }
}