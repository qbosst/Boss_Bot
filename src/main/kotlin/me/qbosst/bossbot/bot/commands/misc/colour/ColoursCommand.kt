package me.qbosst.bossbot.bot.commands.misc.colour

import me.qbosst.bossbot.bot.commands.Command
import me.qbosst.bossbot.database.data.GuildColoursData
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
    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        var index = 0
        val colours: Map<String, Colour> = if(args.isNotEmpty())
        {
            when
            {
                args[0].toLowerCase() == "guild" ->
                {
                    index++;
                    GuildColoursData.get(event.guild).clone()
                }
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
        }
        else GuildColoursData.get(event.guild).clone().plus(systemColours)

        val page = if(args.size > index) if(args[index].toIntOrNull() != null) args[index].toInt()
        else
        {
            event.channel.sendMessage("${args[index].makeSafe()} is not a valid page number").queue()
            return
        } else 0

        val menu = FieldMenuEmbed(12, colours.keys.map { toField(it, colours.getValue(it)) })
        event.channel.sendMessage(menu.createPage(EmbedBuilder(), page).build()).queue()

    }

    private fun toField(name: String, colour: Colour): MessageEmbed.Field
    {
        return MessageEmbed.Field(name, colour.red.toHex() + colour.green.toHex() + colour.blue.toHex(), true)
    }
}