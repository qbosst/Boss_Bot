package me.qbosst.bossbot.util

import dev.kord.common.Color as ColourKord
import javafx.scene.paint.Color as ColourFX
import java.awt.Color as ColourAWT
import kotlin.math.roundToInt
import kotlin.random.Random

fun rgba(r: Int, g: Int, b: Int, a: Int) =
    ColourAWT(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255), a.coerceIn(0, 255))

fun Random.nextColour() = ColourKord(nextInt(0xffffff))

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