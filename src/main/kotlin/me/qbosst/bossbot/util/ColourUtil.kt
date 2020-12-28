package me.qbosst.bossbot.util

import me.qbosst.bossbot.database.managers.GuildColoursManager
import me.qbosst.bossbot.database.managers.GuildColoursManager.getColours
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.requests.restaction.MessageAction
import javafx.scene.paint.Color as ColourFX
import java.awt.Color as ColourAWT
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import javax.imageio.ImageIO
import kotlin.math.roundToInt
import kotlin.random.Random

object ColourUtil
{
    val systemColours =
        mutableMapOf<Class<*>, (Field) -> ColourAWT>()
            .apply {
                put(ColourAWT::class.java) { it[null] as ColourAWT }
                put(ColourFX::class.java) { (it[null] as ColourFX).toJavaAwtColour() }
            }
            .map { (key, value) ->
                key.fields
                    .filter { field -> Modifier.isPublic(field.modifiers) }
                    .map { field -> Pair(field.name.toLowerCase(), value.invoke(field) ) }
            }
            .flatten().toMap()

    /**
     *  Converts an integer (0-255) into a string in hex
     */
    fun Int.toHex(): String = "%02x".format(coerceIn(0, 255))

    /**
     *  Creates a picture for how the colour looks like
     *
     *  @param colour The colour that you want to draw
     *
     *  @return Picture of colour in bytes
     */
    private fun drawPicture(colour: java.awt.Color): ByteArray
    {
        val bi = BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB)
        val ig2 = bi.createGraphics()

        ig2.background = colour
        ig2.clearRect(0, 0, bi.width, bi.height)

        return ByteArrayOutputStream()
            .also { outputStream -> ImageIO.write(bi, "png", outputStream) }
            .toByteArray()
    }

    /**
     *  Converts a javafx.scene.paint.Color object to java.awt.Color
     */
    fun ColourFX.toJavaAwtColour(): ColourAWT = toString().split(2).mapNotNull { it.toIntOrNull(16) }
        .let { (r, g, b, a) -> ColourAWT(r, g, b, a) }

    fun getColourByHex(hex: String): ColourAWT? = when
    {
        hex.isHex(6) -> ColourAWT(hex.toInt(16), false)
        hex.isHex(8) -> hex.split(2).map { it.toInt(16) }.let { (r, g, b, a) -> ColourAWT(r, g, b, a) }
        else -> null
    }

    fun Collection<ColourAWT>.blend(): ColourAWT
    {
        val ratio: Float = 1f / size
        var (r, g, b, a) = listOf(0, 0, 0, 0)
        forEach { colour ->
            r += (colour.red * ratio).roundToInt()
            g += (colour.green * ratio).roundToInt()
            b += (colour.blue * ratio).roundToInt()
            a += (colour.alpha * ratio).roundToInt()
        }

        return safeCreateColour(r, g, b, a)
    }

    fun mixColours(vararg colourRatio: Pair<ColourAWT, Float>): ColourAWT
    {
        var percentage = 0.0F
        var index = 0
        var (r, g, b, a) = listOf(0, 0, 0, 0)

        while (percentage <= 1 && colourRatio.size > index)
        {
            val (colour, ratio) = colourRatio[index]
            r += (colour.red * ratio).roundToInt()
            g += (colour.green * ratio).roundToInt()
            b += (colour.blue * ratio).roundToInt()
            a += (colour.alpha * ratio).roundToInt()

            percentage += ratio
            index++
        }

        return safeCreateColour(r, g, b, a)
    }

    fun MessageChannel.sendColourEmbed(colour: ColourAWT): MessageAction
    {
        val (r, g, b, a) = listOf(colour.red.toHex(), colour.green.toHex(), colour.blue.toHex(), colour.alpha.toHex())
        val embed = EmbedBuilder()
            .addField("Red", "${colour.red} `(${r})` `[${colour.red * 100 / 255}%]`", true)
            .addField("Green", "${colour.green} `(${g})` `[${colour.green * 100 / 255}%]`", true)
            .addField("Blue", "${colour.blue} `(${b})` `[${colour.blue * 100 / 255}%]`", true)
            .addField("Alpha", "${colour.alpha} `(${r})` `[${colour.alpha * 100 / 255}%]`", false)
            .setFooter("RGB: ${r+g+b} | RGBA: ${r+g+b+a}")
            .setThumbnail("attachment://$FILE_NAME")
            .build()

        return sendMessage(embed)
            .addFile(drawPicture(colour), FILE_NAME)
    }

    /**
     *  Generates a random colour
     *
     *  @param hasAlpha whether the opacity of the colour should be randomly generated as well.
     */
    fun Random.nextColour(hasAlpha: Boolean = false): ColourAWT =
        if(hasAlpha) ColourAWT(nextInt(255), nextInt(255), nextInt(255), nextInt(255)) else ColourAWT(nextInt(0xffffff))

    private fun safeCreateColour(r: Int, g: Int, b: Int, a: Int) =
        ColourAWT(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255), a.coerceIn(0, 255))

    fun getColourByQuery(query: String, guild: Guild?) =
        getColourByHex(query) ?: systemColours[query.toLowerCase()] ?: guild?.getColours()?.get(query)


    private const val FILE_NAME = "colour.png"
}