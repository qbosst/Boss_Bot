package me.qbosst.bossbot.util

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.requests.restaction.MessageAction
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import javax.imageio.ImageIO
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.staticProperties
import kotlin.reflect.jvm.isAccessible
import javafx.scene.paint.Color as ColourFX
import java.awt.Color as ColourAWT

object ColourUtil {

    @OptIn(ExperimentalStdlibApi::class)
    val systemColours = buildMap<String, ColourAWT> {

        val coloursAWT = ColourAWT::class.staticProperties.asSequence()
            .mapNotNull { prop -> runCatching { Pair(prop.name, prop.get() as ColourAWT) }.getOrElse { null } }
            .map { (name, colour) -> Pair(name.toLowerCase(), colour) }
            .toMap()
        putAll(coloursAWT)

        val coloursFX = ColourFX::class.staticProperties.asSequence()
            .mapNotNull { prop -> runCatching { Pair(prop.name, prop.get() as ColourFX) }.getOrElse { null } }
            .map { (name, colour) -> Pair(name.toLowerCase(), colour.toAWTColour()) }
            .toMap()
        putAll(coloursFX)
    }

    fun drawColour(colour: ColourAWT): ByteArray {
        val bi = BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB)
        val ig2 = bi.createGraphics()

        ig2.background = colour
        ig2.clearRect(0, 0,bi.width, bi.height)

        return ByteArrayOutputStream()
            .also { outputStream -> ImageIO.write(bi, "png", outputStream) }
            .toByteArray()
    }

    fun ColourFX.toAWTColour(): ColourAWT = ColourAWT(red.toFloat(), green.toFloat(), blue.toFloat(), opacity.toFloat())

    fun Random.nextColour(hasAlpha: Boolean = false): ColourAWT =
        ColourAWT(nextInt(255), nextInt(255), nextInt(255), if(hasAlpha) nextInt(255) else 255)

    fun Collection<ColourAWT>.blend(): ColourAWT {
        val ratio: Float = 1f / size
        var (r, g, b, a) = listOf(0, 0, 0, 0)
        forEach { colour ->
            r += (colour.red * ratio).roundToInt()
            g += (colour.green * ratio).roundToInt()
            b += (colour.blue * ratio).roundToInt()
            a += (colour.alpha * ratio).roundToInt()
        }

        return rgba(r, g, b, a)
    }

    /**
     *  Converts an integer (0-255) into a string in hex
     */
    private fun Int.toHex(): String = "%02x".format(coerceIn(0, 255))

    fun MessageChannel.sendColourEmbed(colour: ColourAWT): MessageAction {
        val (r, g, b, a) = listOf(colour.red.toHex(), colour.green.toHex(), colour.blue.toHex(), colour.alpha.toHex())
        val embed = EmbedBuilder()
            .addField("Red", "${colour.red} `(${r})` `[${colour.red * 100 / 255}%]`", true)
            .addField("Green", "${colour.green} `(${g})` `[${colour.green * 100 / 255}%]`", true)
            .addField("Blue", "${colour.blue} `(${b})` `[${colour.blue * 100 / 255}%]`", true)
            .addField("Alpha", "${colour.alpha} `(${r})` `[${colour.alpha * 100 / 255}%]`", false)
            .setFooter("RGB: ${r+g+b} | ARGB: ${r+g+b+a}")
            .setColor(colour)
            .setThumbnail("attachment://$FILE_NAME")
            .build()

        return sendMessage(embed).addFile(drawColour(colour), FILE_NAME)
    }

    private fun rgba(r: Int, g: Int, b: Int, a: Int) =
        ColourAWT(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255), a.coerceIn(0, 255))

    private const val FILE_NAME = "colour.png"

}