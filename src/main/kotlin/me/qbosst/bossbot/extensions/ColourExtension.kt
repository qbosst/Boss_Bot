package me.qbosst.bossbot.extensions

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.converters.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.cache.api.put
import dev.kord.cache.api.query
import dev.kord.cache.api.remove
import dev.kord.common.kColor
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.MessageBehavior
import dev.kord.core.behavior.reply
import me.qbosst.bossbot.converters.coalescedColour
import me.qbosst.bossbot.converters.colourList
import me.qbosst.bossbot.converters.maxLengthString
import me.qbosst.bossbot.database.models.GuildColours
import me.qbosst.bossbot.database.tables.GuildColoursTable
import me.qbosst.bossbot.util.*
import me.qbosst.bossbot.util.ext.insertOrIgnore
import me.qbosst.bossbot.util.ext.reply
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.imageio.ImageIO
import kotlin.random.Random
import kotlin.reflect.full.staticProperties
import java.awt.Color as Colour
import javafx.scene.paint.Color as ColourFX

class ColourExtension(bot: ExtensibleBot): Extension(bot) {
    override val name: String = "colour"

    class ColourArgs: Arguments() {
        val colour by coalescedColour("colour", "todo", true, buildConverter(true, true))
    }

    class RandomColourArgs: Arguments() {
        val generateRandomOpacity by defaultingBoolean("opacity", "", false)
    }

    class BlendColourArgs: Arguments() {
        val colours by colourList("colours", "", true, buildConverter(true, true))
    }

    class CreateColourArgs: Arguments() {
        val name by maxLengthString("name", "", maxLength = GuildColoursTable.MAX_COLOUR_NAME_LENGTH)
        val colour by coalescedColour("colour", "", shouldThrow = true)
    }

    class RemoveColourArgs: Arguments() {
        val name by maxLengthString("name", "", maxLength = GuildColoursTable.MAX_COLOUR_NAME_LENGTH)
    }

    class UpdateColourArgs: Arguments() {
        val name by maxLengthString("name", "", maxLength = GuildColoursTable.MAX_COLOUR_NAME_LENGTH)
        val colour by coalescedColour("colour", "", shouldThrow = true)
    }

    class ColourListArgs: Arguments() {
        val includeGuild by defaultingBoolean("guild", "", true)
        val includeDefault by defaultingBoolean("system", "", true)
    }

    override suspend fun setup() {
        group(::ColourArgs) {
            name = "colour"

            check(::defaultCheck)

            action {
                message.reply(arguments.colour)
            }

            command(::RandomColourArgs) {
                name = "random"

                action {
                    val randomColour = Random.nextColour(arguments.generateRandomOpacity)
                    message.reply(randomColour)
                }
            }

            command(::BlendColourArgs) {
                name = "blend"

                action {
                    val blendedColour = arguments.colours.blend()
                    message.reply(blendedColour)
                }
            }

            command(::CreateColourArgs) {
                name = "create"

                check(::anyGuild)

                action {
                    val idLong = guild!!.id.value

                    // try to insert this record into the database
                    val isInserted = transaction {
                        GuildColoursTable.insertOrIgnore {
                            it[GuildColoursTable.guildId] = idLong
                            it[GuildColoursTable.name] = arguments.name
                            it[GuildColoursTable.value] = arguments.colour.rgb
                        }
                    }

                    // if the record was inserted, invalidate cache
                    if(isInserted) {
                        event.kord.cache.remove<GuildColours> { GuildColours::guildId.eq(idLong) }
                    }

                    message.reply(false) {
                        content = isInserted.toString()
                    }
                }
            }

            command(::RemoveColourArgs) {
                name = "remove"

                check(::anyGuild)

                action {
                    val idLong = guild!!.id.value

                    // get the amount of records that were deleted with this statement (should be either 1 or 0)
                    val deletedRecords = transaction {
                        GuildColoursTable.deleteWhere {
                            GuildColoursTable.guildId.eq(idLong) and GuildColoursTable.name.eq(arguments.name)
                        }
                    }

                    if(deletedRecords > 0) {
                        event.kord.cache.remove<GuildColours> { GuildColours::guildId.eq(idLong) }
                    }

                    message.reply(false) {
                        content = deletedRecords.toString()
                    }
                }
            }

            command(::UpdateColourArgs) {
                name = "update"

                check(::anyGuild)

                action {
                    val idLong = guild!!.id.value

                    val updatedRecords = transaction {
                        GuildColoursTable.update(
                            where = {
                                GuildColoursTable.guildId.eq(idLong) and GuildColoursTable.name.eq(arguments.name)
                            },
                            body = {
                                it[GuildColoursTable.value] = arguments.colour.rgb
                            }
                        )
                    }

                    if(updatedRecords > 0) {
                        event.kord.cache.remove<GuildColours> { GuildColours::guildId.eq(idLong) }
                    }

                    message.reply(false) {
                        content = updatedRecords.toString()
                    }
                }
            }

            command(::ColourListArgs) {
                name = "list"

                check(::defaultCheck)

                action {
                    val colours = buildConverter(
                        includeDefault = arguments.includeDefault,
                        includeGuild = arguments.includeGuild
                    ).invoke(this)

                    message.reply(false) {
                        val coloursStr = colours.map { (key, _) -> key }.joinToString(", ")
                        content = if(coloursStr.isBlank()) "There are no colours to display" else coloursStr
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    companion object {

        val systemColours = buildMap<String, Colour> {
            // add all java.awt colours to map
            Colour::class.staticProperties.asSequence()
                .mapNotNull { prop -> runCatching { prop.name to prop.get() as Colour }.getOrNull() }
                .forEach { (name, colour) -> put(name.toLowerCase(), colour) }

            // add all javafx colours
            ColourFX::class.staticProperties.asSequence()
                .mapNotNull { prop -> kotlin.runCatching { prop.name to prop.get() as ColourFX }.getOrNull() }
                .map { (name, colour) -> name to colour.toAWT() }
                .forEach { (name, colour) -> put(name.toLowerCase(), colour) }
        }

        private fun Colour.draw(width: Int = 200, height: Int = 200): InputStream {
            val bi = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            val ig2 = bi.createGraphics()

            ig2.background = this
            ig2.clearRect(0, 0, width, height)

            return ByteArrayOutputStream()
                .also { outputStream -> ImageIO.write(bi, "png", outputStream) }
                .toByteArray().inputStream()
        }

        private suspend fun MessageBehavior.reply(colour: Colour) = reply {
            val fileName = "colour.png"

            embed {
                fun Int.hex() = "%02x".format(this)

                val (r, g, b, a) = listOf(colour.red.hex(), colour.green.hex(), colour.blue.hex(), colour.alpha.hex())
                color = colour.kColor
                field("Red", true) { "${colour.red} `${r}` [${colour.red * 100 / 255}%]" }
                field("Green", true) { "${colour.green} `${g}` [${colour.green * 100 / 255}%]" }
                field("Blue", true) { "${colour.blue} `${b}` [${colour.blue * 100 / 255}%]" }
                field("Opacity", true) { "${colour.alpha} `${a}` [${colour.alpha * 100 / 255}%]" }

                footer { text = "RGB: ${r+g+b} | RGBA: ${r+g+b+a}" }
                thumbnail { url = "attachment://$fileName" }
            }

            allowedMentions { repliedUser = false }
            addFile(fileName, colour.draw())
        }

        private suspend fun GuildBehavior.getCachedColours(): GuildColours? {
            val idLong = id.value
            val cache = kord.cache

            return cache.query<GuildColours> { GuildColours::guildId.eq(idLong) }.singleOrNull()
        }

        /**
         * @param shouldCache whether to cache the result retrieved from the database
         */
        private suspend fun GuildBehavior.getColours(shouldCache: Boolean = true): GuildColours {
            val idLong = id.value
            val cache = kord.cache

            // check to see if colours are cached, if so return that
            val cached = getCachedColours()
            if(cached != null) {
                return cached
            }

            // get colours from database
            val retrieved = transaction {
                GuildColoursTable.select { GuildColoursTable.guildId.eq(idLong) }.asSequence()
                    .map { row -> row[GuildColoursTable.name] to rgba(row[GuildColoursTable.value]) }
                    .toMap().ifEmpty { emptyMap() }
            }.let { map -> GuildColours(idLong, map) }

            // cache the result
            if(shouldCache) {
                cache.put(retrieved)
            }

            return retrieved
        }

        /**
         * Builds a [me.qbosst.bossbot.converters.impl.ColourConverter.colourProvider]
         */
        private fun buildConverter(
            includeDefault: Boolean,
            includeGuild: Boolean
        ): suspend (CommandContext) -> Map<String, Colour> = { ctx ->
            buildMap {
                if(includeDefault) {
                    putAll(systemColours)
                }
                if(includeGuild) {
                    val guild = ctx.getGuild()
                    if(guild != null) {
                        val colours = guild.getColours().data
                        putAll(colours)
                    }
                }
            }
        }

        private fun Random.nextColour(isAlpha: Boolean = false) =
            rgba(nextInt(255), nextInt(255), nextInt(255), if(isAlpha) nextInt(255) else 255)
    }
}