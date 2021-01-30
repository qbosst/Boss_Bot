package me.qbosst.bossbot.extensions

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.converters.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.cache.map.MapLikeCollection
import dev.kord.cache.map.lruLinkedHashMap
import dev.kord.common.annotation.KordPreview
import dev.kord.common.kColor
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.MessageBehavior
import dev.kord.core.behavior.reply
import me.qbosst.bossbot.converters.coalescedColour
import me.qbosst.bossbot.converters.colourList
import me.qbosst.bossbot.converters.maxLengthString
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

@OptIn(ExperimentalStdlibApi::class, KordPreview::class)
class ColourExtension(bot: ExtensibleBot, cacheSize: Int): Extension(bot) {
    private val cache = MapLikeCollection.lruLinkedHashMap<Long, Map<String, Colour>>(cacheSize)

    override val name: String = "colour"

    inner class ColourArgs: Arguments() {
        val colour by coalescedColour("colour", "", true, buildConverter(true, true))
    }

    class RandomColourArgs: Arguments() {
        val isAlpha by defaultingBoolean("opacity", "", false)
    }

    inner class BlendColourArgs: Arguments() {
        val colours by colourList("colours", "", true, buildConverter(true, true))
    }

    class CreateColourArgs: Arguments() {
        val name by maxLengthString("name", "", GuildColoursTable.MAX_COLOUR_NAME_LENGTH)
        val colour by coalescedColour("colour", "", shouldThrow = true)
    }

    class RemoveColourArgs: Arguments() {
        val name by maxLengthString("name", "", GuildColoursTable.MAX_COLOUR_NAME_LENGTH)
    }

    class UpdateColourArgs: Arguments() {
        val name by maxLengthString("name", "", GuildColoursTable.MAX_COLOUR_NAME_LENGTH)
        val colour by coalescedColour("colour", "", shouldThrow = true)
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

                check(::defaultCheck)

                action {
                    val colour = Random.nextColour(arguments.isAlpha)
                    message.reply(colour)
                }
            }

            command(::BlendColourArgs) {
                name = "blend"

                check(::defaultCheck)

                action {
                    val colour = arguments.colours.blend()
                    message.reply(colour)
                }
            }

            command(::CreateColourArgs) {
                name = "create"

                check(::defaultCheck, ::anyGuild)

                action {
                    val id = guild!!.id.value

                    val inserted = transaction {
                        GuildColoursTable.insertOrIgnore {
                            it[GuildColoursTable.guildId] = id
                            it[GuildColoursTable.name] = arguments.name
                            it[GuildColoursTable.value] = arguments.colour.rgb
                        }
                    }

                    if(inserted) {
                        cache.remove(id)
                    }

                    message.reply(false) {
                        content = inserted.toString()
                    }
                }
            }

            command(::RemoveColourArgs) {
                name = "remove"

                check(::defaultCheck, ::anyGuild)

                action {
                    val id = guild!!.id.value

                    val deleted = transaction {
                        GuildColoursTable.deleteWhere {
                            GuildColoursTable.guildId.eq(id) and GuildColoursTable.name.eq(arguments.name)
                        }
                    }

                    if(deleted > 0) {
                        cache.remove(id)
                    }

                    message.reply(false) {
                        content = deleted.toString()
                    }
                }
            }

            command(::UpdateColourArgs) {
                name = "update"

                check(::defaultCheck, ::anyGuild)

                action {
                    val id = guild!!.id.value

                    val updated = transaction {
                        GuildColoursTable.update(
                            where = {
                                GuildColoursTable.guildId.eq(id) and GuildColoursTable.name.eq(arguments.name)
                            },
                            body = {
                                it[GuildColoursTable.value] = arguments.colour.rgb
                            }
                        )
                    }

                    if(updated > 0) {
                        cache.remove(id)
                    }

                    message.reply(false) {
                        content = updated.toString()
                    }
                }
            }

            command {
                name = "list"

                check(::defaultCheck)

                action {
                    message.reply(false) {
                        content = systemColours.map { (key, _) -> key }.joinToString(", ")
                    }
                }
            }
        }
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
        embed {
            fun Int.toHex() = "%02x".format(this)

            val (r, g, b, a) = listOf(
                colour.red.toHex(), colour.green.toHex(), colour.blue.toHex(), colour.alpha.toHex()
            )

            color = colour.kColor
            field {
                name = "Red"
                value = "${colour.red} `${r}` `[${colour.red * 100 / 255}%]`"
                inline = true
            }

            field {
                name = "Green"
                value = "${colour.green} `${g}` `[${colour.green * 100 / 255}%]`"
                inline = true
            }

            field {
                name = "Blue"
                value = "${colour.blue} `${b}` `[${colour.blue * 100 / 255}%]`"
                inline = true
            }

            field {
                name = "Alpha"
                value = "${colour.alpha} `${a}` `[${colour.alpha * 100 / 255}%]`"
                inline = true
            }

            footer { text = "RGB: ${r+g+b} | RGBA: ${r+g+b+a}" }
            thumbnail { url = "attachment://$FILE_NAME" }
        }

        allowedMentions {
            repliedUser = false
        }

        addFile(FILE_NAME, colour.draw())
    }

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
                    val colours = guild.getColours()
                    putAll(colours)
                }
            }
        }
    }

    private suspend fun GuildBehavior.getColours(): Map<String, Colour> {
        val id = this.id.value
        if(cache.contains(id)) {
            return cache.get(id)!!
        }

        val result = transaction {
            GuildColoursTable
                .select { GuildColoursTable.guildId.eq(id) }
                .map { row -> row[GuildColoursTable.name] to Colour(row[GuildColoursTable.value], true) }
                .toMap()
        }
        val colours = if(result.isEmpty()) emptyMap() else result

        cache.put(id, colours)
        return colours
    }

    companion object {
        private const val FILE_NAME = "colour.png"

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

        private fun Random.nextColour(isAlpha: Boolean = false) =
            Colour(nextInt(255), nextInt(255), nextInt(255), if (isAlpha) nextInt(255) else 255)
    }
}