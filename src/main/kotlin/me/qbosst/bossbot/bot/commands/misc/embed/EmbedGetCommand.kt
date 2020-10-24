package me.qbosst.bossbot.bot.commands.misc.embed

import me.qbosst.bossbot.bot.commands.meta.Command
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.json.JSONArray

/**
 *  This command is used to get already existing messages and convert the embeds in that message into json representations
 */
object EmbedGetCommand : Command(
        "get",
        botPermissions = listOf(Permission.MESSAGE_ATTACH_FILES),
        guildOnly = false
)
{

    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        // Gets message id
        val messageId = if (args.isNotEmpty()) args[0].toLongOrNull() ?: kotlin.run()
        {
            event.channel.sendMessage("That is not a valid ID!").queue()
            return
        }
        else
        {
            event.channel.sendMessage("Please mention a message id!").queue()
            return
        }

        // Retrieves message from channel
        event.channel.retrieveMessageById(messageId).map { it.embeds }.queue(
                {
                    if(it.isNotEmpty())

                        // Returns json representation of embed
                        if(it.size == 1)
                            event.channel.sendFile(it.first().toData().toJson(), "${messageId}.json").queue()
                        else
                        {
                            val array = JSONArray()
                            for(embed in it)
                                array.put(embed.toData().toJson())
                            event.channel.sendFile(array.toString(4).toByteArray(), "${messageId}.json").queue()
                        }
                    else
                        event.channel.sendMessage("There are not embeds in this message.").queue()
                },
                {
                    event.channel.sendMessage("Exception caught: $it").queue()
                }
        )
    }
}