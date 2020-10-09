package me.qbosst.bossbot.bot.commands.misc.colour

import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.entities.database.GuildColoursData
import me.qbosst.bossbot.util.embed.FieldMenuEmbed
import me.qbosst.bossbot.util.makeSafe
import me.qbosst.bossbot.util.toHex
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.awt.Color as Colour

object ColoursCommand: Command(
    "colours",
    "Provides a list of colours that are available",
    aliases = listOf("colors"),
    usage = listOf("[page number]", "guild [page number]", "system [page number]"),
    guildOnly = false,
    botPermissions = listOf(Permission.MESSAGE_EMBED_LINKS)
) {
    private const val MAX_COLOURS_PER_PAGE = 12

    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        var index = 0

        // Gets the list of colours that should be displayed in the message
        val colours: Map<String, Colour> = if(args.isNotEmpty()) when
        {
            // Specifies that only guild custom colours should be shown on the menu
            args[0].toLowerCase() == "guild" ->
            {
                index++;
                GuildColoursData.get(event.guild).clone()
            }
            // Specifies that only system colours should be shown
            args[0].toLowerCase() == "system" ->
            {
                index++;
                systemColours
            }
            args[0].toIntOrNull() != null -> GuildColoursData.get(event.guild).clone().plus(systemColours)
            else ->
            {
                event.channel.sendMessage("${args[0].makeSafe()} is not a valid page number").queue()
                return
            }
        }
        // If no arguments were given, both system colours and guild colours will be shown
        else
            GuildColoursData.get(event.guild).clone().plus(systemColours)

        // Gets the page number of the menu
        val page =
                if(args.size > index)
                    if(args[index].toIntOrNull() != null)
                        args[index].toInt()
                    else
                    {
                        event.channel.sendMessage("${args[index].makeSafe()} is not a valid page number").queue()
                        return
                    }
                else
                    0

        // Creates and sends the menu
        val menu = FieldMenuEmbed(MAX_COLOURS_PER_PAGE, colours.map { it.toField() })
        event.channel.sendMessage(menu.createPage(EmbedBuilder(), page).build()).queue()
    }

    /**
     *  Converts an entry of <string, colour> to a message embed field.
     */
    private fun Map.Entry<String, Colour>.toField(): MessageEmbed.Field
    {
        return MessageEmbed.Field(key, value.red.toHex() + value.green.toHex() + value.blue.toHex(), true)
    }
}