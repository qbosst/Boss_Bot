package me.qbosst.bossbot.bot.commands.misc.embed

import me.qbosst.bossbot.bot.argumentInvalid
import me.qbosst.bossbot.bot.argumentMissing
import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.util.extensions.LONG_REGEX
import me.qbosst.bossbot.util.extensions.toPrettyJson
import me.qbosst.bossbot.util.extensions.getTextChannelByString
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.utils.data.DataArray

object EmbedGetCommand: Command(
        "get",
        description = "Gets the embeds from a message",
        usages = listOf("<message id> [#channel]"),
        botPermissions = listOf(Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_HISTORY)
)
{
    override fun execute(event: MessageReceivedEvent, args: List<String>, flags: Map<String, String?>)
    {
        val messageId = kotlin.run {
            val arg = (args.getOrNull(0) ?: kotlin.run {
                event.channel.sendMessage(argumentMissing("message Id")).queue(); return
            })
            if(!arg.matches(LONG_REGEX))
            {
                event.channel.sendMessage(argumentInvalid(arg, "message Id")).queue(); return
            }
            arg.toLong()
        }

        val channel = kotlin.run {
            val arg = args.getOrNull(1) ?: return@run event.textChannel
            val channel = event.guild.getTextChannelByString(arg) ?: kotlin.run {
                event.channel.sendMessage("I could not find that channel").queue(); return
            }
            if(!event.guild.selfMember.hasPermission(channel, botPermissions))
            {
                event.channel.sendMessage("I do not have the correct permissions for ${channel.asMention}").queue();
                return
            }
            channel
        }

        channel.retrieveMessageById(messageId).queue(
                {
                    val array = DataArray.empty()
                    for(embed in it.embeds)
                        array.add(embed.toData())

                    if(array.isEmpty)
                        event.channel.sendMessage("There are no embeds in this message").queue()
                    else
                        event.channel.sendFile(array.toPrettyJson(), "${messageId}.json").queue()
                },
                {
                    when
                    {
                        // Message was not found
                        it is ErrorResponseException && it.errorCode == 10008 ->
                            event.channel.sendMessage("I could not find any message with the id of `${messageId}` in this channel.").queue()
                        // Other error
                        else ->
                            event.channel.sendMessage("Error while trying to retrieve message $messageId from ${channel.asMention}: `${it.localizedMessage}`").queue()
                    }
                }
        )
    }
}