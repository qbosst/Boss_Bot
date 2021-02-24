package me.qbosst.bossbot.extensions

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.commands.CommandContext
import com.kotlindiscord.kord.extensions.commands.converters.SingleToMultiConverter
import com.kotlindiscord.kord.extensions.commands.converters.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.parser.Arguments
import com.kotlindiscord.kord.extensions.extensions.Extension
import dev.kord.cache.api.put
import dev.kord.cache.api.query
import dev.kord.cache.api.remove
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.MessageBehavior
import dev.kord.core.behavior.reply
import me.qbosst.bossbot.commands.converters.SingleToCoalescingConverter
import me.qbosst.bossbot.commands.converters.impl.ColourConverter
import me.qbosst.bossbot.commands.converters.impl.MaxStringConverter
import me.qbosst.bossbot.database.models.GuildColours
import me.qbosst.bossbot.database.tables.GuildColoursTable
import me.qbosst.bossbot.util.Colour
import me.qbosst.bossbot.util.ext.insertOrIgnore
import me.qbosst.bossbot.util.ext.reply
import me.qbosst.bossbot.util.jColour
import me.qbosst.bossbot.util.kColour
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.imageio.ImageIO
import kotlin.reflect.full.memberProperties

private val defaultColours: Map<String, Colour> by lazy {
    Colour.Companion::class.memberProperties.asSequence()
        .filter { prop -> prop.get(Colour.Companion) is Colour }
        .map { prop -> prop.name.toLowerCase() to prop.get(Colour.Companion) as Colour }
        .toMap()
}

private suspend fun GuildBehavior.getColours(transaction: Transaction): GuildColours {
    val idLong = id.value

    return kord.cache.query<GuildColours> { GuildColours::guildId.eq(idLong) }.singleOrNull()
        ?: transaction.run {
            GuildColoursTable
                .select { GuildColoursTable.guildId.eq(idLong) }
                .map { row -> row[GuildColoursTable.name] to Colour(row[GuildColoursTable.value]) }
                .toMap()
                .let { map -> GuildColours(idLong, map) }
        }.also {
            kord.cache.put(it)
        }
}

private suspend fun GuildBehavior.getColours() = newSuspendedTransaction { getColours(this) }

private fun buildConverter(
    includeDefault: Boolean,
    includeGuild: Boolean,
    includeRandom: Boolean
): suspend (CommandContext) -> Map<String, Colour> = { ctx ->
    mutableMapOf<String, Colour>().apply {
        if(includeDefault) {
            putAll(defaultColours)
        }

        if(includeRandom) {
            val colour = Colour.random(false)
            put("rand", colour)
            put("random", colour)
        }

        if(includeGuild) {
            val guild = ctx.getGuild()
            if(guild != null) {
                val guildColours = guild.getColours()
                putAll(guildColours.colours)
            }
        }
    }
}

private fun Colour.draw(width: Int = 200, height: Int = 200): InputStream {
    val bi = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    val ig2 = bi.createGraphics()

    ig2.background = this.jColour
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
        color = colour.kColour
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

class ColourExtension(bot: ExtensibleBot): Extension(bot) {
    override val name: String = "colours"

    class ColourArgs: Arguments() {
        val colour by arg("", "", SingleToCoalescingConverter(ColourConverter(buildConverter(true, true, true))))
    }

    class ColourRandomArgs: Arguments() {
        val hasAlpha by defaultingBoolean("", "", true)
    }

    class ColourBlendArgs: Arguments() {
        val colourList by arg("", "", SingleToMultiConverter(singleConverter = ColourConverter(buildConverter(true, true, true))))
    }

    class ColourCreateArgs: Arguments() {
        val name by arg("name", "", MaxStringConverter(maxLength = GuildColoursTable.MAX_NAME_LENGTH))
        val colour by arg("colour", "", SingleToCoalescingConverter(ColourConverter(buildConverter(false, false, false))))
    }

    class ColourRemoveArgs: Arguments() {
        val name by arg("name", "", MaxStringConverter(maxLength = GuildColoursTable.MAX_NAME_LENGTH))
    }

    class ColourUpdateArgs: Arguments() {
        val name by arg("name", "", MaxStringConverter(maxLength = GuildColoursTable.MAX_NAME_LENGTH))
        val colour by arg("colour", "", SingleToCoalescingConverter(ColourConverter(buildConverter(false, false, false))))
    }

    class ColourListArgs: Arguments() {
        val includeGuild by defaultingBoolean("guild", "", true)
        val includeDefault by defaultingBoolean("system", "", true)
    }

    override suspend fun setup() {
        group(::ColourArgs) {
            name = "colour"

            action {
                message.reply(arguments.colour)
            }

            command(::ColourRandomArgs) {
                name = "random"

                action {
                    val randomColour = Colour.random(arguments.hasAlpha)
                    message.reply(randomColour)
                }
            }

            command(::ColourBlendArgs) {
                name = "blend"

                action {
                    val blendedColour = Colour.blend(arguments.colourList)
                    message.reply(blendedColour)
                }
            }

            command(::ColourCreateArgs) {
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
                        content = if(!isInserted) {
                            "There is already a colour by the name of '${arguments.name}'"
                        } else {
                            "Successfully created `${arguments.name}`"
                        }
                    }
                }
            }

            command(::ColourRemoveArgs) {
                name = "remove"

                check(::anyGuild)

                action {
                    val idLong = guild!!.id.value

                    val deleted = transaction {
                        GuildColoursTable.deleteWhere {
                            GuildColoursTable.guildId.eq(idLong) and GuildColoursTable.name.eq(arguments.name)
                        }
                    }

                    if(deleted > 0) {
                        event.kord.cache.remove<GuildColours> { GuildColours::guildId.eq(idLong) }
                    }

                    message.reply(false) {
                        content = if(deleted == 0) {
                            "There is no colour by the name of `${arguments.name}`"
                        } else {
                            "Successfully deleted `${arguments.name}`"
                        }
                    }
                }
            }

            command(::ColourUpdateArgs) {
                name = "update"

                check(::anyGuild)

                action {
                    val idLong = guild!!.id.value

                    val updated = transaction {
                        GuildColoursTable.update({ GuildColoursTable.guildId.eq(idLong) and GuildColoursTable.name.eq(arguments.name) }) {
                            it[GuildColoursTable.value] = arguments.colour.rgb
                        }
                    }

                    if(updated > 0) {
                        event.kord.cache.remove<GuildColours> { GuildColours::guildId.eq(idLong) }
                    }

                    message.reply(false) {
                        content = if(updated == 0) {
                            "There is no colour by the name of `${arguments.name}`"
                        } else {
                            "Successfully updated `${arguments.name}`"
                        }
                    }
                }
            }

            command(::ColourListArgs) {
                name = "list"

                check(::anyGuild)

                action {
                    val colours = buildConverter(
                        includeDefault = arguments.includeDefault,
                        includeGuild = arguments.includeGuild,
                        includeRandom = false
                    ).invoke(this)

                    message.reply(false) {
                        val coloursStr = colours.map { (key, _) -> key }.joinToString(", ")
                        content = if(coloursStr.isBlank()) "There are no colours to display" else coloursStr
                    }
                }
            }
        }
    }
}