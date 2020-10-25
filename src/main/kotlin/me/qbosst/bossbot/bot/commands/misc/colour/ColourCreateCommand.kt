package me.qbosst.bossbot.bot.commands.misc.colour

import me.qbosst.bossbot.bot.argumentInvalid
import me.qbosst.bossbot.bot.argumentMissing
import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.database.managers.GuildColoursManager
import me.qbosst.bossbot.database.tables.GuildColoursTable
import me.qbosst.bossbot.util.getGuildOrNull
import me.qbosst.bossbot.util.maxLength
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

/**
 *  Creates a custom colour for a guild
 */
object ColourCreateCommand: Command(
        "create",
        "Creates a custom colour for your guild",
        usage_raw = listOf("<hex value> <name>"),
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
                event.channel.sendMessage(argumentInvalid(args[0], "hex")).queue()
                return
            }

            if(args.size > 1)
            {
                // Gets the name of the colour
                val name = args[1]
                val data = GuildColoursManager.get(event.getGuildOrNull())
                when
                {
                    // Makes sure that the guild hasn't passed its limit
                    data.values().size >= GuildColoursTable.MAX_COLOURS_PER_GUILD ->
                        event.channel.sendMessage("This guild has reached the maximum amount of colours per guild (${GuildColoursTable.MAX_COLOURS_PER_GUILD}!)").queue()

                    // Makes sure that name is valid length
                    name.length > GuildColoursTable.MAX_COLOUR_NAME_LENGTH ->
                        event.channel.sendMessage("Colour names cannot be longer than ${GuildColoursTable.MAX_COLOUR_NAME_LENGTH} characters long!").queue()

                    // Makes sure that the name doesn't have the same name as a system colour
                    systemColours[name] != null ->
                        event.channel.sendMessage("${name.maxLength()} is a system default colour and cannot be modified.").queue()
                    else ->
                    {
                        // Tries to add colour to guild.
                        if(GuildColoursManager.add(event.guild, name, colour))
                            event.channel.sendMessage("${name.maxLength()} has been successfully created!").queue()
                        else
                            event.channel.sendMessage("${name.maxLength()} already exists!").queue()
                    }
                }
            }
            else
                event.channel.sendMessage(argumentMissing("name of this colour")).queue()
        }
        else
            event.channel.sendMessage(argumentMissing("hex value of the colour you would like to create")).queue()
    }
}