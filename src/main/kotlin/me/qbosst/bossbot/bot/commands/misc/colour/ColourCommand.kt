package me.qbosst.bossbot.bot.commands.misc.colour

import com.google.common.base.Splitter
import javafx.scene.paint.Color
import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.entities.database.GuildColoursData
import me.qbosst.bossbot.util.getGuildOrNull
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
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import javax.imageio.ImageIO

object ColourCommand: Command(
        "colour",
        "Shows information about a specific colour",
        usage = listOf("<colour name | hex>"),
        examples = listOf("purple", "0feeed"),
        aliases = listOf("color"),
        guildOnly = false,
        botPermissions = listOf(Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES)
) {

    init
    {
        // Loads sub commands
        addCommands(setOf(ColourBlendCommand, ColourCreateCommand, ColourRandomCommand, ColourRemoveCommand))
    }

    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        // Tries to get a valid colour either from name or hex code
        if(args.isNotEmpty())
        {
            val colour = getColourByHex(args[0]) ?: systemColours[args[0]] ?: GuildColoursData.get(event.getGuildOrNull()).get(args[0])

            // If colour was found, send embed otherwise return error message
            if(colour != null)
                sendColourEmbed(event.channel, colour).queue()
            else
                event.channel.sendMessage("`${args[0].makeSafe()}` is not a valid colour!").queue()
        }
        else
            event.channel.sendMessage("Please enter the hex code or name of a colour!").queue()
    }

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

    /**
     *  Creates the embed for the colour
     *
     *  @param colour the colour to create the embed of
     *
     *  @return the embed containing information of the colour
     */
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

    /**
     *  Creates action for sending the embed
     *
     *  @param channel the channel to send the embed to
     *  @param colour the colour that the embed should contain
     *
     *  @return the message action to send the embed.
     */
    fun sendColourEmbed(channel: MessageChannel, colour: java.awt.Color): MessageAction
    {
        val name = "colour.png"
        return channel.sendMessage(embedColour(colour).setThumbnail("attachment://$name").build()).addFile(drawPicture(colour), name)
    }
}

/**
 *  Converts a javafx.scene.paint.Color object to java.awt.Color
 */
fun javafx.scene.paint.Color.toJavaAwtColor(): java.awt.Color
{
    // Get the hex value of the colour and return it into the java.awt.Color object.
    val hex = Splitter.fixedLength(2).split(this.toString().substring(2, 10)).map { it.toInt(16) }
    return java.awt.Color(hex[0], hex[1], hex[2], hex[3])
}

/**
 *  Converts a hex string into a colour.
 *
 *  @param hex The hex in string
 *
 *  @return Color the colour based from the hex code. Null if an invalid string was given.
 */
fun getColourByHex(hex: String): java.awt.Color?
{
    return when
    {
        hex.isHex(6) -> java.awt.Color(hex.toInt(16), false)
        hex.isHex(8) ->
        {
            val splitHex = Splitter.fixedLength(2).split(hex).map { it.toInt(16) }
            java.awt.Color(splitHex[0], splitHex[1], splitHex[2], splitHex[3])
        }
        else -> null
    }
}

// These are the default system colours
val systemColours = run {
    val classes = mapOf<Class<*>, (Field) -> java.awt.Color>(
            Pair(java.awt.Color::class.java, { it[null] as java.awt.Color }),
            Pair(Color::class.java, { (it[null] as Color).toJavaAwtColor() })
    )
    val colours = mutableMapOf<String, java.awt.Color>()

    // Load default colours
    for(clazz in classes)
        for(field in clazz.key.declaredFields)
            try
            {
                if(!Modifier.isPublic(field.modifiers))
                    continue

                colours[field.name.toLowerCase()] = clazz.value.invoke(field)
            }
            catch (e: IllegalAccessException) {}

    colours
}