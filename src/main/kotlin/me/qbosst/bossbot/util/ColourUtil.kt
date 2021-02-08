package me.qbosst.bossbot.util

import dev.kord.common.Color as KordColour
import javafx.scene.paint.Color as FXColour
import java.awt.Color as AWTColour
import kotlin.math.roundToInt
import kotlin.random.Random

fun rgba(r: Int, g: Int, b: Int, a: Int) =
    AWTColour(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255), a.coerceIn(0, 255))

fun rgba(value: Int) = AWTColour(value, true)

fun rgb(r: Int, g: Int, b: Int) = rgba(r, g, b, 255)

fun rgb(value: Int) = AWTColour(value, false)

fun Random.nextColour() = KordColour(nextInt(0xffffff))

fun FXColour.toAWT(): AWTColour =
    AWTColour(red.toFloat(), green.toFloat(), blue.toFloat(), opacity.toFloat())

fun Collection<AWTColour>.blend(): AWTColour {
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