package me.qbosst.bossbot.bot.commands.misc.colour

import me.qbosst.bossbot.bot.argumentInvalid
import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.database.managers.GuildColoursManager
import me.qbosst.bossbot.util.embed.FieldMenuEmbed
import me.qbosst.bossbot.util.getGuildOrNull
import me.qbosst.bossbot.util.toHex
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.awt.Color as Colour

object ColoursCommand: Command(
    "colours",
    "Provides a list of colours that are available",
    aliases_raw = listOf("colors"),
    usage_raw = listOf("[page number]", "guild [page number]", "system [page number]"),
    guildOnly = false,
    botPermissions = listOf(Permission.MESSAGE_EMBED_LINKS)
) {
    private const val MAX_COLOURS_PER_PAGE = 12

    override fun execute(event: MessageReceivedEvent, args: List<String>, flags: Map<String, String?>)
    {
        var index = 0

        // Gets the list of colours that should be displayed in the message
        val colours: Map<String, Colour> = if(args.isNotEmpty()) when
        {
            // Specifies that only guild custom colours should be shown on the menu
            args[0].toLowerCase() == "guild" ->
            {
                index++
                GuildColoursManager.get(event.getGuildOrNull()).colours
            }
            // Specifies that only system colours should be shown
            args[0].toLowerCase() == "system" ->
            {
                index++
                systemColours
            }
            args[0].toIntOrNull() != null -> GuildColoursManager.get(event.getGuildOrNull()).colours.plus(systemColours)
            else ->
            {
                event.channel.sendMessage(argumentInvalid(args[0], "page number")).queue()
                return
            }
        }
        // If no arguments were given, both system colours and guild colours will be shown
        else
            GuildColoursManager.get(event.getGuildOrNull()).colours.plus(systemColours)

        // Gets the page number of the menu
        val page =
                if(args.size > index)
                    if(args[index].toIntOrNull() != null)
                        args[index].toInt()
                    else
                    {
                        event.channel.sendMessage(argumentInvalid(args[index], "page number")).queue()
                        return
                    }
                else
                    0

        // Creates and sends the menu
        val menu = FieldMenuEmbed(MAX_COLOURS_PER_PAGE, colours.map { it.toField() })
        event.channel.sendMessage(menu.createPage(EmbedBuilder()
            .setColor(event.getGuildOrNull()?.selfMember?.color)
            , page).build()).queue()
    }

    /**
     *  Converts an entry of <string, colour> to a message embed field.
     */
    private fun Map.Entry<String, Colour>.toField(): MessageEmbed.Field
    {
        return MessageEmbed.Field(key, value.red.toHex() + value.green.toHex() + value.blue.toHex(), true)
    }
}