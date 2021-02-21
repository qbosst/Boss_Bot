package me.qbosst.bossbot.util

import kotlin.math.roundToInt

class Colour {
    val rgb: Int

    constructor(rgb: Int) {
        this.rgb = (0xff000000.toInt() or rgb)
    }

    constructor(rgba: Int, hasAlpha: Boolean) {
        this.rgb = if(hasAlpha) rgba else (0xff000000.toInt() or rgba)
    }

    constructor(red: Int, green: Int, blue: Int, alpha: Int) {
        require(red in 0..255) { "Red should be in range of 0..255 but was $red" }
        require(green in 0..255) { "Green should be in range of 0..255 but was $green" }
        require(blue in 0..255) { "Blue should be in range of 0..255 but was $blue" }
        require(alpha in 0..255) { "Alpha should be in range of 0..255 but was $alpha" }

        this.rgb = ((alpha and 0xFF) shl 24) or
                ((red and 0xFF) shl 16) or
                ((green and 0xFF) shl 8) or
                ((blue and 0xFF))
    }

    constructor(red: Int, green: Int, blue: Int): this(red, green, blue, 255)

    val alpha: Int get() = (rgb shr 24) and 0xff
    val red: Int get() = (rgb shr 16) and 0xff
    val green: Int get() = (rgb shr 8) and 0xff
    val blue: Int get() = rgb and 0xff

    override fun toString(): String = "Colour(r=${red},green=${green},blue=${blue},a=${alpha})"

    companion object {
        // red colours
        val INDIAN_RED = Colour(0xCD5C5C)
        val LIGHT_CORAL = Colour(0xF08080)
        val SALMON = Colour(0xFA8072)
        val DARK_SALMON = Colour(0xE9967A)
        val CRIMSON = Colour(0xDC143C)
        val RED = Colour(0xFF0000)
        val FIRE_BRICK = Colour(0xB22222)
        val DARK_RED = Colour(0x8B0000)

        // pink colours
        val PINK = Colour(0xFFC0CB)
        val LIGHT_PINK = Colour(0xFFB6C1)
        val HOT_PINK = Colour(0xFF69B4)
        val DEEP_PINK = Colour(0xFF1493)
        val MEDIUM_VIOLET_RED = Colour(0xC71585)
        val PALE_VIOLET_RED = Colour(0xDB7093)

        // orange colours
        val CORAL = Colour(0xFF7F50)
        val TOMATO = Colour(0xFF6347)
        val ORANGE_RED = Colour(0xFF4500)
        val DARK_ORANGE = Colour(0xFF8C00)
        val ORANGE = Colour(0xFFA500)

        // yellow colours
        val GOLD = Colour(0xFFD700)
        val YELLOW = Colour(0xFFFF00)
        val LIGHT_YELLOW = Colour(0xFFFFE0)
        val LEMON_CHIFFON = Colour(0xFFFACD)
        val LIGHT_GOLDENROD_YELLOW = Colour(0xFAFAD2)
        val PAPAYA_WHIP = Colour(0xFFEFD5)
        val MOCCASIN = Colour(0xFFE4B5)
        val PEACH_PUFF = Colour(0xFFDAB9)
        val PALE_GOLDENROD = Colour(0xEEE8AA)
        val KHAKI = Colour(0xF0E68C)
        val DARK_KHAKI = Colour(0xBDB76B)

        // purple colours
        val LAVENDER = Colour(0xE6E6FA)
        val THISTLE = Colour(0xD8BFD8)
        val PLUM = Colour(0xDDA0DD)
        val VIOLET = Colour(0xEE82EE)
        val ORCHID = Colour(0xDA70D6)
        val FUCHSIA = Colour(0xFF00FF)
        val MAGENTA = Colour(0xFF00FF)
        val MEDIUM_ORCHID = Colour(0xBA55D3)
        val MEDIUM_PURPLE = Colour(0x9370DB)
        val REBECCA_PURPLE = Colour(0x663399)
        val BLUE_VIOLET = Colour(0x8A2BE2)
        val DARK_VIOLET = Colour(0x9400D3)
        val DARK_ORCHID = Colour(0x9932CC)
        val DARK_MAGENTA = Colour(0x8B008B)
        val PURPLE = Colour(0x800080)
        val INDIGO = Colour(0x4B0082)
        val SLATE_BLUE = Colour(0x6A5ACD)
        val DARK_SLATE_BLUE = Colour(0x483D8B)

        // green colours
        val GREEN_YELLOW = Colour(0xADFF2F)
        val CHARTREUSE = Colour(0x7FFF00)
        val LAWN_GREEN = Colour(0x7CFC00)
        val LIME = Colour(0x00FF00)
        val LIME_GREEN = Colour(0x32CD32)
        val PALE_GREEN = Colour(0x98FB98)
        val LIGHT_GREEN = Colour(0x90EE90)
        val MEDIUM_SPRING_GREEN = Colour(0x00FA9A)
        val SPRING_GREEN = Colour(0x00FF7F)
        val MEDIUM_SEA_GREEN = Colour(0x3CB371)
        val SEA_GREEN = Colour(0x2E8B57)
        val FOREST_GREEN = Colour(0x228B22)
        val GREEN = Colour(0x008000)
        val DARK_GREEN = Colour(0x006400)
        val YELLOW_GREEN = Colour(0x9ACD32)
        val OLIVE_DRAB = Colour(0x6B8E23)
        val OLIVE = Colour(0x808000)
        val DARK_OLIVE_GREEN = Colour(0x556B2F)
        val MEDIUM_AQUAMARINE = Colour(0x66CDAA)
        val DARK_SEA_GREEN = Colour(0x8FBC8B)
        val LIGHT_SEA_GREEN = Colour(0x20B2AA)
        val DARK_CYAN = Colour(0x008B8B)
        val TEAL = Colour(0x008080)

        // blue colours
        val AQUA = Colour(0x00FFFF)
        val CYAN = Colour(0x00FFFF)
        val LIGHT_CYAN = Colour(0xE0FFFF)
        val PALE_TURQUOISE = Colour(0xAFEEEE)
        val AQUAMARINE = Colour(0x7FFFD4)
        val TURQUOISE = Colour(0x40E0D0)
        val MEDIUM_TURQUOISE = Colour(0x48D1CC)
        val DARK_TURQUOISE = Colour(0x00CED1)
        val CADET_BLUE = Colour(0x5F9EA0)
        val STEEL_BLUE = Colour(0x4682B4)
        val LIGHT_STEEL_BLUE = Colour(0xB0C4DE)
        val POWDER_BLUE = Colour(0xB0E0E6)
        val LIGHT_BLUE = Colour(0xADD8E6)
        val SKY_BLUE = Colour(0x87CEEB)
        val LIGHT_SKY_BLUE = Colour(0x87CEFA)
        val DEEP_SKY_BLUE = Colour(0x00BFFF)
        val DODGER_BLUE = Colour(0x1E90FF)
        val CORNFLOWER_BLUE = Colour(0x6495ED)
        val MEDIUM_SLATE_BLUE = Colour(0x7B68EE)
        val ROYAL_BLUE = Colour(0x4169E1)
        val BLUE = Colour(0x0000FF)
        val MEDIUM_BLUE = Colour(0x0000CD)
        val DARK_BLUE = Colour(0x00008B)
        val NAVY = Colour(0x000080)
        val MIDNIGHT_BLUE = Colour(0x191970)

        // brown colours
        val CORNSILK = Colour(0xFFF8DC)
        val BLANCHED_ALMOND = Colour(0xFFEBCD)
        val BISQUE = Colour(0xFFE4C4)
        val NAVAJO_WHITE = Colour(0xFFDEAD)
        val WHEAT = Colour(0xF5DEB3)
        val BURLY_WOOD = Colour(0xDEB887)
        val TAN = Colour(0xD2B48C)
        val ROSY_BROWN = Colour(0xBC8F8F)
        val SANDY_BROWN = Colour(0xF4A460)
        val GOLDENROD = Colour(0xDAA520)
        val DARK_GOLDENROD = Colour(0xB8860B)
        val PERU = Colour(0xCD853F)
        val CHOCOLATE = Colour(0xD2691E)
        val SADDLE_BROWN = Colour(0x8B4513)
        val SIENNA = Colour(0xA0522D)
        val BROWN = Colour(0xA52A2A)
        val MAROON = Colour(0x800000)

        // white colours
        val WHITE = Colour(0xFFFFFF)
        val SNOW = Colour(0xFFFAFA)
        val HONEY_DEW = Colour(0xF0FFF0)
        val MINT_CREAM = Colour(0xF5FFFA)
        val AZURE = Colour(0xF0FFFF)
        val ALICE_BLUE = Colour(0xF0F8FF)
        val GHOST_WHITE = Colour(0xF8F8FF)
        val WHITE_SMOKE = Colour(0xF5F5F5)
        val SEA_SHELL = Colour(0xFFF5EE)
        val BEIGE = Colour(0xF5F5DC)
        val OLD_LACE = Colour(0xFDF5E6)
        val FLORAL_WHITE = Colour(0xFFFAF0)
        val IVORY = Colour(0xFFFFF0)
        val ANTIQUE_WHITE = Colour(0xFAEBD7)
        val LINEN = Colour(0xFAF0E6)
        val LAVENDER_BLUSH = Colour(0xFFF0F5)
        val MISTY_ROSE = Colour(0xFFE4E1)

        // gray colours
        val GAINSBORO = Colour(0xDCDCDC)
        val LIGHT_GRAY = Colour(0xD3D3D3)
        val SILVER = Colour(0xC0C0C0)
        val DARK_GRAY = Colour(0xA9A9A9)
        val GRAY = Colour(0x808080)
        val DIM_GRAY = Colour(0x696969)
        val LIGHT_SLATE_GRAY = Colour(0x778899)
        val SLATE_GRAY = Colour(0x708090)
        val DARK_SLATE_GRAY = Colour(0x2F4F4F)
        val BLACK = Colour(0x000000)

        fun blend(colours: Collection<Colour>): Colour {
            val ratio = 1f / colours.size
            var r = 0
            var g = 0
            var b = 0
            var a = 0

            colours.forEach { colour ->
                r += (colour.red * ratio).roundToInt()
                g += (colour.green * ratio).roundToInt()
                b += (colour.blue * ratio).roundToInt()
                a += (colour.alpha * ratio).roundToInt()
            }

            return Colour(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255), a.coerceIn(0, 255))
        }

        fun random(hasAlpha: Boolean): Colour = (0..255)
            .let { ints -> Colour(ints.random(), ints.random(), ints.random(), if(hasAlpha) ints.random() else 255) }
    }
}

fun rgba(red: Int, green: Int, blue: Int, alpha: Int) = Colour(red, green, blue, alpha)

fun rgb(red: Int, green: Int, blue: Int) = Colour(red, green, blue)

fun rgba(rgba: Int) = Colour(rgba, true)

fun rgb(rgb: Int) = Colour(rgb)

val Colour.kColour: dev.kord.common.Color get() = dev.kord.common.Color(rgb)

val Colour.jColour: java.awt.Color get() = java.awt.Color(rgb, true)