package me.qbosst.bossbot.bot.commands.misc.embed

import com.fasterxml.jackson.core.JsonParseException
import me.qbosst.bossbot.bot.commands.meta.Command
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.MessageEmbed.EMBED_MAX_LENGTH_BOT
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.exceptions.ParsingException
import net.dv8tion.jda.api.utils.data.DataObject
import net.dv8tion.jda.internal.entities.EntityBuilder

object EmbedCommand: Command(
        "embed",
        "Creates an embed based on json",
        botPermissions = listOf(Permission.MESSAGE_EMBED_LINKS)
)
{
    init
    {
        addCommands(EmbedTemplateCommand, EmbedGetCommand)
    }

    override fun execute(event: MessageReceivedEvent, args: List<String>)
    {
        if(args.isNotEmpty())
        {
            process(event.channel, args.joinToString(" ").toByteArray())
        }
        else
        {
            val first = event.message.attachments.firstOrNull { it.fileExtension == "json" }
            if(first != null)
            {
                first.downloadToFile().thenAccept()
                {
                    process(event.channel, it.readBytes())
                }
            }
            else
            {
                event.channel.sendMessage("invalid!").queue()
            }
        }
    }

    private fun process(channel: MessageChannel, content: ByteArray)
    {
        try
        {
            val obj = DataObject.fromJson(content)
            if(!obj.hasKey("type"))
                obj.put("type", "rich")

            val embed = EntityBuilder(channel.jda).createMessageEmbed(obj)
            when
            {
                embed.isEmpty -> channel.sendMessage("This embed is empty!").queue()
                embed.length > EMBED_MAX_LENGTH_BOT -> channel.sendMessage("This embed is too large!").queue()
                else -> channel.sendMessage(embed).queue()
            }
        }
        catch (e: ParsingException)
        {
            if(e.cause is JsonParseException)
            {
                channel.sendMessage("Exception caught while trying to parse json ```${(e.cause as JsonParseException).message}```").queue()
            }
            else
            {
                channel.sendMessage("Caught Exception: $e").queue()
            }
        }
    }

}