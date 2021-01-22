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

class ColourExtension(bot: ExtensibleBot, cacheSize: Int): BaseExtension(bot) {
    private val cache = MapLikeCollection.lruLinkedHashMap<Long, Map<String, Colour>>(cacheSize)

    override val name: String = "colour"

    override suspend fun setup() {
        group(colourGroup())
    }

    private suspend fun colourGroup() = createGroup({
        class Args: Arguments() {
            val colour by coalescedColour("colour", "The colour to display", true, buildConverter(true, true))
        }
        return@createGroup Args()
    }) {
        name = "colour"

        action {
            message.replyColourEmbed(arguments.colour)
        }

        command(randomCommand())
        command(blendCommand())
        command(createColourCommand())
        command(updateColourCommand())
        command(removeColourCommand())
    }

    private suspend fun randomCommand() = createCommand({
        class Args: Arguments() {
            val isAlpha by defaultingBoolean("isAlpha", "Whether a colour should have a random opacity", false)
        }
        return@createCommand Args()
    }) {
        name = "random"

        action {
            val colour = Random.nextColour(arguments.isAlpha)
            message.replyColourEmbed(colour)
        }
    }

    private suspend fun blendCommand() = createCommand({
        class Args: Arguments() {
            val colours by colourList("colours", "The colours to blend together", true, buildConverter(true, true))
        }
        return@createCommand Args()
    }) {
        name = "blend"

        action {
            val colour = arguments.colours.blend()
            message.replyColourEmbed(colour)
        }
    }

    private suspend fun createColourCommand() = createCommand({
        class Args: Arguments() {
            val name by maxLengthString("name", "The name of the colour", GuildColoursTable.MAX_COLOUR_NAME_LENGTH)
            val colour by coalescedColour("colour", "The colour to create", shouldThrow = true)
        }
        return@createCommand Args()
    }) {
        name = "create"

        check { event -> event.guildId != null }
        check { event -> event.member!!.hasPermission(Permission.ManageEmojis) }

        action {
            val guildId = guild!!.id.value

            val isInserted = transaction {
                GuildColoursTable.insertOrIgnore {
                    it[GuildColoursTable.guildId] = guildId
                    it[GuildColoursTable.name] = arguments.name
                    it[GuildColoursTable.value] = arguments.colour.rgb
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

    private suspend fun removeColourCommand() = createCommand({
        class Args: Arguments() {
            val name by maxLengthString("colour name", "The name of the colour to remove", GuildColoursTable.MAX_COLOUR_NAME_LENGTH)
        }
        return@createCommand Args()
    }) {
        name = "remove"
        aliases = arrayOf("delete")

        check { event -> event.guildId != null }
        check { event -> event.member!!.hasPermission(Permission.ManageEmojis) }

        action {
            val guildId = guild!!.id.value

            // deletes records & gives the amount of records deleted
            val deleted = transaction {
                GuildColoursTable.deleteWhere {
                    GuildColoursTable.guildId.eq(guildId) and GuildColoursTable.name.eq(arguments.name)
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

    private suspend fun updateColourCommand() = createCommand({
        class Args: Arguments() {
            val name by maxLengthString("name", "The name of the colour to update", GuildColoursTable.MAX_COLOUR_NAME_LENGTH)
            val colour by coalescedColour("new colour", "The colour to update the name with", shouldThrow = true)
        }
        return@createCommand Args()
    }) {
        name = "update"

        check { event -> event.guildId != null }
        check { event -> event.member!!.hasPermission(Permission.ManageEmojis) }

        action {
            val guildId = guild!!.id.value

            // updates records & gives the amount of records updated
            val updated = transaction {
                GuildColoursTable.update(
                    { GuildColoursTable.guildId.eq(guildId) and GuildColoursTable.name.eq(arguments.name) }
                ) {
                    it[GuildColoursTable.value] = arguments.colour.rgb
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

    private suspend fun MessageBehavior.replyColourEmbed(colour: Colour) = reply(false) {
        ColourUtil.buildColourEmbed(this, colour, "colour.png")
    }
}