package me.qbosst.bossbot.bot.commands.misc.colour

import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.database.tables.GuildColoursDataTable
import me.qbosst.bossbot.entities.database.GuildColoursData
import me.qbosst.bossbot.exception.ReachedMaxAmountException
import me.qbosst.bossbot.util.assertNumber
import me.qbosst.bossbot.util.makeSafe
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.awt.Color as Colour

object ColourCreateCommand: Command(
        "create",
        "Creates a custom colour for your guild",
        usage = listOf("<hex value> <name>", "<red> <green> <blue> <alpha> <name>"),
        userPermissions = listOf(Permission.MANAGE_EMOTES, Permission.MANAGE_SERVER)
) {
    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        if(args.isNotEmpty())
        {
            var colour = getColourByHex(args[0])
            if(colour != null)
            {
                getName(event, args.drop(1), colour)
            }
            else
            {
                val names = listOf("red", "green", "blue", "alpha")
                val values = mutableListOf<Int>()
                for(x in 0..3)
                {
                    if(args.size > x)
                    {
                        val int = args[x].toIntOrNull()
                        if(int == null)
                        {
                            event.channel.sendMessage("${names[x]} must be a numeric value!").queue()
                            return
                        }
                        else
                        {
                            values.add(assertNumber(0, 255, int))
                        }
                    }
                    else
                    {
                        event.channel.sendMessage("Please mention the value for ${names[x]}").queue()
                        return
                    }
                }
                getName(event, args.drop(4), Colour(values[0], values[1], values[2], values[3]))
            }
        }
        else
        {
            event.channel.sendMessage("Please provide colour argument").queue()
        }
    }

    private fun getName(event: MessageReceivedEvent, args: List<String>, colour: Colour)
    {
        if(args.isNotEmpty())
        {
            when
            {
                args[0].length > GuildColoursDataTable.max_colour_name_length ->
                {
                    event.channel.sendMessage("Colour names cannot be longer than ${GuildColoursDataTable.max_colour_name_length} characters long!").queue()
                }
                systemColours[args[0]] == null ->
                {
                    try
                    {
                        if(GuildColoursData.add(event.guild, args[0], colour))
                        {
                            event.channel.sendMessage("${args[0].makeSafe()} has successfully been created!").queue()
                        }
                        else
                        {
                            event.channel.sendMessage("${args[0].makeSafe()} already exists!").queue()
                        }
                    }
                    catch (e: ReachedMaxAmountException)
                    {
                        event.channel.sendMessage("This guild has reached the max amount of colours that it can have!").queue()
                    }
                }
                else ->
                {
                    event.channel.sendMessage("${args[0].makeSafe()} is a system default colour and cannot be modified.").queue()
                }
            }
        }
        else
        {
            event.channel.sendMessage("Please provide the name for this colour").queue()
        }
    }
}