package me.qbosst.bossbot.util

import dev.kord.common.kColor
import dev.kord.rest.builder.message.MessageCreateBuilder
import javafx.scene.paint.Color as ColourFX
import java.awt.Color as ColourAWT
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.imageio.ImageIO
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.reflect.full.staticProperties

@OptIn(ExperimentalStdlibApi::class)
object ColourUtil {

    val systemColours = buildMap<String, ColourAWT> {
        // add all java.awt colours to map
        ColourAWT::class.staticProperties.asSequence()
            .mapNotNull { prop -> runCatching { prop.name to prop.get() as ColourAWT }.getOrNull() }
            .forEach { (name, colour) -> put(name.toLowerCase(), colour) }

        // add all javafx colours
        ColourFX::class.staticProperties.asSequence()
            .mapNotNull { prop -> kotlin.runCatching { prop.name to prop.get() as ColourFX }.getOrNull() }
            .map { (name, colour) -> name to colour.toAWT() }
            .forEach { (name, colour) -> put(name.toLowerCase(), colour) }
    }

    fun rgba(r: Int, g: Int, b: Int, a: Int) =
        ColourAWT(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255), a.coerceIn(0, 255))

    fun Random.nextColour(isAlpha: Boolean = false) =
        ColourAWT(nextInt(255), nextInt(255), nextInt(255), if (isAlpha) nextInt(255) else 255)

    fun ColourFX.toAWT(): ColourAWT =
        ColourAWT(red.toFloat(), green.toFloat(), blue.toFloat(), opacity.toFloat())

    fun Collection<ColourAWT>.blend(): ColourAWT {
        val ratio = 1f / size
        var (r, g, b, a) = listOf(0, 0, 0, 0)

        forEach { colour ->
            r += (colour.red * ratio).roundToInt()
            g += (colour.green * ratio).roundToInt()
            b += (colour.blue * ratio).roundToInt()
            a += (colour.alpha * ratio).roundToInt()
        }

        return rgba(r, g, b, a)
    }


    val buildColourEmbed: MessageCreateBuilder.(ColourAWT, String) -> Unit = { colour, fileName ->
        addFile(fileName, colour.draw())

        allowedMentions {
            repliedUser = false
        }

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
            thumbnail { url = "attachment://$fileName" }
        }
    }

    private fun ColourAWT.draw(width: Int = 200, height: Int = 200): InputStream {
        val bi = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val ig2 = bi.createGraphics()

        ig2.background = this
        ig2.clearRect(0, 0, width, height)

        return ByteArrayOutputStream()
            .also { outputStream -> ImageIO.write(bi, "png", outputStream) }
            .toByteArray().inputStream()
    }
}