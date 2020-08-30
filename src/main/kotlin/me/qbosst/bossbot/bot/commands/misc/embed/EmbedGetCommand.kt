package me.qbosst.bossbot.bot.commands.misc.embed

import me.qbosst.bossbot.bot.commands.meta.Command
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.json.JSONArray

object EmbedGetCommand : Command(
        "get",
        botPermissions = listOf(Permission.MESSAGE_ATTACH_FILES)
) {
    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        val messageId = if (args.isNotEmpty())
        {
            args[0].toLongOrNull() ?: kotlin.run()
            {
                event.channel.sendMessage("That is not a valid ID!").queue()
                return
            }
        }
        else
        {
            event.channel.sendMessage("Please mention a message id!").queue()
            return
        }

        event.channel.retrieveMessageById(messageId).map { it.embeds }.queue(
                {
                    if (it.isNotEmpty()) {
                        if (it.size == 1) {
                            event.channel.sendFile(it.first().toData().toJson(), "${messageId}.json").queue()
                        } else {
                            val array = JSONArray()
                            for (embed in it) {
                                array.put(embed.toData().toJson())
                            }
                            event.channel.sendFile(array.toString(4).toByteArray(), "${messageId}.json").queue()
                        }
                    } else {
                        event.channel.sendMessage("There are no embeds in this message!").queue()
                    }
                },
                {
                    event.channel.sendMessage("Exception caught: $it").queue()
                }
        )
    }
}