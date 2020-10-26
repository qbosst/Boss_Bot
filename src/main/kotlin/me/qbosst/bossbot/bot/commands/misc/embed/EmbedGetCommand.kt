package me.qbosst.bossbot.bot.commands.misc.embed

import me.qbosst.bossbot.bot.argumentInvalid
import me.qbosst.bossbot.bot.argumentMissing
import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.util.LONG_REGEX
import me.qbosst.bossbot.util.getTextChannelByString
import me.qbosst.bossbot.util.toJSONObject
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import org.json.JSONArray

object EmbedGetCommand: Command(
        "get",
        description = "Gets the embeds from a message",
        usage_raw = listOf("<message id> [#channel]"),
        botPermissions = listOf(Permission.MESSAGE_ATTACH_FILES)
)
{
    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        if(args.isNotEmpty())
        {
            if(args[0].matches(LONG_REGEX))
            {
                val messageId = args[0].toLong()
                val channel = if(args.size > 1)
                    event.guild.getTextChannelByString(args[1]) ?: kotlin.run {
                        event.channel.sendMessage("I could not find that channel").queue()
                        return
                    }
                else
                    event.textChannel

                channel.retrieveMessageById(messageId).queue(
                    {
                        val array = JSONArray()
                        for(embed in it.embeds)
                            array.put(embed.toData().toJSONObject())

                        if(array.isEmpty)
                            event.channel.sendMessage("There are no embeds in this message").queue()
                        else
                            event.channel.sendFile(array.toString(4).toByteArray(), "${messageId}.json").queue()
                    },
                    {
                        when(it) {
                            is ErrorResponseException ->
                                // Message was not found
                                if(it.errorCode == 10008)
                                    event.channel.sendMessage("I could not find any message with the id of `${messageId}` in this channel.").queue()
                                // Other error
                                else
                                    event.channel.sendMessage("Error while trying to retrieve message $messageId: `${it.localizedMessage}`").queue()
                            // Other error
                            else ->
                                event.channel.sendMessage("Error while trying to retrieve message $messageId: `${it.localizedMessage}`").queue()
                        }
                    }
                )
            }
            else
                event.channel.sendMessage(argumentInvalid(args[0], "message Id")).queue()
        }
        else
            event.channel.sendMessage(argumentMissing("message Id")).queue()
    }
}