package me.qbosst.bossbot.bot.commands.misc.embed

import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.util.EmbedUtil
import me.qbosst.bossbot.util.JSONUtil
import me.qbosst.bossbot.util.loadObjects
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object EmbedCommand: Command(
        "embed",
        description = "Creates and sends a Message Embed based on JSON input",
        usages = listOf("<json>", "(file.json)"),
        guildOnly = false,
        botPermissions = listOf(Permission.MESSAGE_EMBED_LINKS),
        children = listOf(EmbedGetCommand, EmbedTemplateCommand)
)
{
    override fun execute(event: MessageReceivedEvent, args: List<String>, flags: Map<String, String?>)
    {
        if(args.isNotEmpty())
            process(event, args.joinToString(" ").toByteArray())
        else
        {
            val json = event.message.attachments.firstOrNull { it.fileExtension == "json" }
            if(json != null)
                json.downloadToFile().thenAccept()
                {
                    process(event, it.readBytes())
                }
            else
                event.channel.sendMessage("Please provide JSON to parse to create the embed").queue()
        }
    }

    private fun process(event: MessageReceivedEvent, content: ByteArray)
    {
        // Tries to convert content into JSON
        JSONUtil.parseJson(content,
                // Successfully parsed JSON
                { data ->
                    EmbedUtil.parseEmbed(event.jda, data,
                            // Successfully parsed Embed
                            { embed ->
                                event.channel.sendMessage(embed).queue(
                                        {},
                                        // Unsuccessfully sent Embed
                                        {
                                            event.channel.sendMessage("Failed to send embed: ${it.localizedMessage}").queue()
                                        })
                            },
                            // Unsuccessfully parsed Embed
                            { error, type ->
                                event.channel.sendMessage(type.errorMessage.invoke(error)).queue()
                            })
                },
                // Unsuccessfully parsed JSON
                { _, message ->
                    event.channel.sendMessage(message).queue()
                })
    }
}