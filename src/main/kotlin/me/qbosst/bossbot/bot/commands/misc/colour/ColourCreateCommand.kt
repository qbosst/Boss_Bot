package me.qbosst.bossbot.bot.commands.misc.colour

import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.database.tables.GuildColoursDataTable
import me.qbosst.bossbot.entities.database.GuildColoursData
import me.qbosst.bossbot.util.getGuildOrNull
import me.qbosst.bossbot.util.makeSafe
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

/**
 *  Creates a custom colour for a guild
 */
object ColourCreateCommand: Command(
        "create",
        "Creates a custom colour for your guild",
        usage = listOf("<hex value> <name>"),
        userPermissions = listOf(Permission.MANAGE_EMOTES, Permission.MANAGE_SERVER),
        guildOnly = true
)
{
    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        if(args.isNotEmpty())
        {
            // Gets colour by hex value
            var colour = getColourByHex(args[0]) ?: kotlin.run {
                event.channel.sendMessage("`${args[0].makeSafe()}` is not a valid hex!")
                return
            }

            if(args.size > 1)
            {
                // Gets the name of the colour
                val name = args[1]
                val data = GuildColoursData.get(event.getGuildOrNull())
                when
                {
                    // Makes sure that the guild hasn't passed its limit
                    data.values().size >= GuildColoursDataTable.MAX_COLOURS_PER_GUILD ->
                        event.channel.sendMessage("This guild has reached the maximum amount of colours per guild (${GuildColoursDataTable.MAX_COLOURS_PER_GUILD}!)").queue()

                    // Makes sure that name is valid length
                    name.length > GuildColoursDataTable.MAX_COLOUR_NAME_LENGTH ->
                        event.channel.sendMessage("Colour names cannot be longer than ${GuildColoursDataTable.MAX_COLOUR_NAME_LENGTH} characters long!").queue()

                    // Makes sure that the name doesn't have the same name as a system colour
                    systemColours[name] != null ->
                        event.channel.sendMessage("${name.makeSafe()} is a system default colour and cannot be modified.").queue()
                    else ->
                    {
                        // Tries to add colour to guild.
                        if(GuildColoursData.add(event.guild, name, colour))
                            event.channel.sendMessage("${name.makeSafe()} has been successfully created!").queue()
                        else
                            event.channel.sendMessage("${name.makeSafe()} already exists!").queue()
                    }
                }
            }
            else
                event.channel.sendMessage("Please provide the name of this colour").queue()
        }
        else
            event.channel.sendMessage("Please provide the hex value of the colour you would like to create.").queue()
    }
}