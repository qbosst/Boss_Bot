package me.qbosst.bossbot.bot.commands.misc.colour

import com.google.common.base.Splitter
import javafx.scene.paint.Color
import me.qbosst.bossbot.bot.BossBot
import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.entities.database.GuildColoursData
import me.qbosst.bossbot.util.isHex
import me.qbosst.bossbot.util.makeSafe
import me.qbosst.bossbot.util.toHex
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.requests.restaction.MessageAction
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.lang.reflect.Modifier
import javax.imageio.ImageIO
import java.awt.Color as Colour

object ColourCommand: Command(
        "colour",
        "Shows information about a specific colour",
        usage = listOf("<colour>"),
        examples = listOf("purple", "0feeed"),
        aliases = listOf("color"),
        guildOnly = false,
        botPermissions = listOf(Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES)
) {

    init
    {
        addCommands(setOf(ColourRandomCommand, ColourBlendCommand, ColourCreateCommand, ColourRemoveCommand, ColourMixCommand))
    }

    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        if(args.isNotEmpty())
        {
            val colour = GuildColoursData.get(if(event.isFromGuild) event.guild else null).get(args[0]) ?: systemColours[args[0]] ?: getColourByHex(args[0])
            if(colour != null)
            {
                sendColourEmbed(event.channel, colour).queue()
            }
            else
            {
                event.channel.sendMessage("`${args[0].makeSafe()}` is not a valid colour!").queue()
            }
        }
        else
        {
            event.channel.sendMessage("Please mention a valid colour!").queue()
        }
    }

    private fun drawPicture(colour: Colour): ByteArray
    {
        val bi = BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB)
        val ig2 = bi.createGraphics()

        ig2.background = colour
        ig2.clearRect(0, 0, bi.width, bi.height)
        val baos = ByteArrayOutputStream()

        return try
        {
            ImageIO.write(bi, "png", baos)
            baos.toByteArray()
        }
        catch (e: IOException)
        {
            LoggerFactory.getLogger(ColourCommand::class.java).error("Exception caught while trying to draw image for a colour: $e")
            ByteArray(0)
        }
    }

    private fun embedColour(colour: java.awt.Color): EmbedBuilder
    {
        val red = colour.red.toHex(); val green = colour.green.toHex(); val blue = colour.blue.toHex(); val alpha = colour.alpha.toHex()
        return EmbedBuilder()
                .addField("Red", "${colour.red} `($red)` `[${colour.red * 100 / 255}%]`", true)
                .addField("Green", "${colour.green} `($green)` `[${colour.green * 100 / 255}%]`", true)
                .addField("Blue", "${colour.blue} `($blue)` `[${colour.blue * 100 / 255}%]`", true)
                .addField("Alpha", "${colour.alpha} `($alpha)` `[${colour.alpha * 100 / 255}%]`", false)
                .setFooter("RGB : ${red + green + blue} | RGBA : ${red + green + blue + alpha}")
                .setColor(colour)
    }

    fun sendColourEmbed(channel: MessageChannel, colour: Colour): MessageAction
    {
        val name = "colour.png"
        return channel.sendMessage(embedColour(colour).setThumbnail("attachment://$name").build()).addFile(drawPicture(colour), name)
    }
}

fun Color.toJavaAwtColor(): Colour
{
    val hex = Splitter.fixedLength(2).split(this.toString().substring(2, 10)).map { it.toInt(16) }
    return java.awt.Color(hex[0], hex[1], hex[2], hex[3])
}

fun getColourByHex(hex: String): Colour?
{
    return when
    {
        hex.isHex() -> Colour(hex.toInt(16), false)
        hex.isHex(8) ->
        {
            val splitHex = Splitter.fixedLength(2).split(hex).map { it.toInt(16) }
            Colour(splitHex[0], splitHex[1], splitHex[2], splitHex[3])
        }
        else -> null
    }
}

val systemColours = run {
    val classes = arrayOf(Colour::class.java, Color::class.java)
    val colours = mutableMapOf<String, java.awt.Color>()

    for(clazz in classes)
    {
        val fields = clazz.declaredFields
        loop@ for (field in fields) {
            try
            {
                val modifiers = field.modifiers
                if (!Modifier.isPublic(modifiers))
                {
                    continue@loop
                }

                var c: Colour = when
                {
                    clazz.isAssignableFrom(Colour::class.java) -> field[null] as Colour
                    clazz.isAssignableFrom(Color::class.java) -> (field[null] as Color).toJavaAwtColor()
                    else ->
                    {
                        BossBot.LOG.warn("Created null colour")
                        continue@loop
                    }
                }

                colours[field.name.toLowerCase()] = c
            }
            catch (e: IllegalAccessException) {}
        }
    }

    colours
}