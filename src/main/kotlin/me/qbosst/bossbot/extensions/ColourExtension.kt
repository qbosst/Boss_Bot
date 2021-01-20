package me.qbosst.bossbot.extensions

import com.gitlab.kordlib.cache.map.MapLikeCollection
import com.gitlab.kordlib.cache.map.lruLinkedHashMap
import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.commands.Command
import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.GroupCommand
import com.kotlindiscord.kord.extensions.commands.converters.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.utils.hasPermission
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import me.qbosst.bossbot.converters.ColourCoalescingConverter
import me.qbosst.bossbot.converters.ColourConverter
import me.qbosst.bossbot.converters.lengthyString
import me.qbosst.bossbot.database.tables.GuildColoursTable
import me.qbosst.bossbot.util.ColourUtil
import me.qbosst.bossbot.util.ColourUtil.blend
import me.qbosst.bossbot.util.ColourUtil.nextColour
import me.qbosst.bossbot.util.ext.insertOrIgnore
import me.qbosst.bossbot.util.ext.reply
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.random.Random
import java.awt.Color as Colour

class ColourExtension(bot: ExtensibleBot, cacheSize: Int): Extension(bot) {

    private val cache = MapLikeCollection.lruLinkedHashMap<Snowflake, Map<String, Colour>>(cacheSize)

    override val name: String = "colours"

    override suspend fun setup() {
        group(colourGroup)
    }

    private val colourGroup: suspend GroupCommand.() -> Unit = {
        class Args: Arguments() {
            val colour by coalescingColour("colour", includeDefault = true, includeGuild = true)
        }

        name = "colour"

        signature(::Args)
        action {
            with(parse(::Args)) {
                message.reply(false) {
                    ColourUtil.buildColourEmbed(this, colour, "colour.png")
                }
            }
        }

        command(randomCommand)
        command(blendCommand)
        command(createCommand)
        command(removeCommand)
        command(updateCommand)
    }

    private val randomCommand: suspend Command.() -> Unit = {
        class Args: Arguments() {
            val isAlpha by defaultingBoolean("isAlpha", false)
        }

        name = "random"
        description = "Generates and displays a random colour"

        signature(::Args)
        action {
            with(parse(::Args)) {
                message.reply(false) {
                    val colour = Random.nextColour(isAlpha)
                    ColourUtil.buildColourEmbed(this, colour, "colour.png")
                }
            }
        }
    }

    private val blendCommand: suspend Command.() -> Unit = {
        class Args: Arguments() {
            val colours by colour("colours", includeDefault = true, includeGuild = true).toMulti()
        }

        name = "blend"
        description = "Blends colours together, and displays the result"

        signature(::Args)
        action {
            with(parse(::Args)) {
                val blended = colours.blend()
                message.reply(false) {
                    ColourUtil.buildColourEmbed(this, blended, "colour.png")
                }
            }
        }
    }

    private val createCommand: suspend Command.() -> Unit = {
        class Args: Arguments() {
            val name by lengthyString("name", GuildColoursTable.MAX_COLOUR_NAME_LENGTH)
            val colour by coalescingColour("colour", includeDefault = false, includeGuild = false)
        }

        name = "create"
        description = "Creates a guild-colour"

        check { event -> event.guildId != null }
        check { event -> event.member!!.hasPermission(Permission.ManageEmojis) }

        signature(::Args)
        action {
            with(parse(::Args)) {
                val guildId = message.data.guildId.value!!

                val isInserted = transaction {
                    GuildColoursTable.insertOrIgnore {
                        it[GuildColoursTable.guildId] = guildId.value
                        it[GuildColoursTable.name] = this@with.name
                        it[GuildColoursTable.value] = colour.rgb
                    }
                }

                message.reply(false) {
                    if(isInserted) {
                        content = "inserted"
                        cache.remove(guildId)
                    } else {
                        content = "record already exists"
                    }
                }
            }
        }
    }

    private val removeCommand: suspend Command.() -> Unit = {
        class Args: Arguments() {
            val name by lengthyString("colour name", GuildColoursTable.MAX_COLOUR_NAME_LENGTH)
        }

        name = "remove"
        description = "Removes a guild-colour"
        aliases = arrayOf("delete")

        check { event -> event.guildId != null }
        check { event -> event.member!!.hasPermission(Permission.ManageEmojis) }

        signature(::Args)
        action {
            with(parse(::Args)) {
                val guildId = message.data.guildId.value!!

                // deletes records & gives the amount of records deleted
                val deleted = transaction {
                    GuildColoursTable.deleteWhere {
                        GuildColoursTable.guildId.eq(guildId.value) and GuildColoursTable.name.eq(name)
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

    private val updateCommand: suspend Command.() -> Unit = {
        class Args: Arguments() {
            val name by lengthyString("name", GuildColoursTable.MAX_COLOUR_NAME_LENGTH)
            val colour by coalescingColour("new colour", includeDefault = false, includeGuild = false)
        }

        name = "update"

        check { event -> event.guildId != null }
        check { event -> event.member!!.hasPermission(Permission.ManageEmojis) }

        signature(::Args)
        action {
            with(parse(::Args)) {
                val guildId = message.data.guildId.value!!

                // updates records & gives the amount of records updated
                val updated = transaction {
                    GuildColoursTable.update(
                        { GuildColoursTable.guildId.eq(guildId.value) and GuildColoursTable.name.eq(name) }
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

    private suspend fun getColoursById(id: Snowflake): Map<String, Colour> {
        // return if colours in cache
        if(cache.contains(id)) {
            return cache.get(id)!!
        }

        // make database call
        val colours = transaction {
            GuildColoursTable
                .select { GuildColoursTable.guildId.eq(id.value) }.asSequence()
                .map { row -> row[GuildColoursTable.name] to Colour(row[GuildColoursTable.value], true) }
                .toMap()
        }

        // cache and return result
        cache.put(id, colours)
        return colours
    }

    private fun Arguments.colour(displayName: String, includeDefault: Boolean = true, includeGuild: Boolean = true) =
        arg(displayName, ColourConverter(buildConverter(includeDefault, includeGuild)))

    private fun Arguments.coalescingColour(
        displayName: String,
        includeDefault: Boolean = true,
        includeGuild: Boolean = true
    ) = arg(displayName, ColourCoalescingConverter(buildConverter(includeDefault, includeGuild)))

    @OptIn(ExperimentalStdlibApi::class)
    private fun buildConverter(
        includeDefault: Boolean,
        includeGuild: Boolean,
    ): suspend (CommandContext) -> Map<String, Colour> = { ctx ->
        buildMap {
            if(includeDefault) {
                // put system colours
                putAll(ColourUtil.systemColours)
            }

            if(includeGuild) {
                // put guild colours
                val guildId = ctx.message.data.guildId.value
                if(guildId != null) {
                    val guildColours = getColoursById(guildId)
                    putAll(guildColours)
                }
            }
        }
    }
}