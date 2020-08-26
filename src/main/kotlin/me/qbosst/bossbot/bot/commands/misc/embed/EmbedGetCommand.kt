package me.qbosst.bossbot.bot.commands.misc.embed

import me.qbosst.bossbot.bot.commands.meta.Command
import me.qbosst.bossbot.entities.JSONEmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.json.JSONArray
import org.json.JSONObject
import java.time.OffsetDateTime

object EmbedGetCommand : Command(
        "get",
        botPermissions = listOf(Permission.MESSAGE_ATTACH_FILES)
) {
    override fun execute(event: MessageReceivedEvent, args: List<String>) {
        val messageId = if(args.isNotEmpty())
        {
            args[0].toLongOrNull() ?: kotlin.run {
                event.channel.sendMessage("That is not a valid ID!").queue()
                return
            }
        }
        else
        {
            event.channel.sendMessage("Please mention a message id!").queue()
            return
        }

        event.channel.retrieveMessageById(messageId).map { it.embeds }.queue()
        {
            if(it.isNotEmpty())
            {
                if(it.size == 1)
                {
                    event.channel.sendFile(it[0].getJson().toString(4).toByteArray(), "${messageId}.json").queue()
                }
                else
                {
                    val array = JSONArray()
                    for(embed in it)
                    {
                        array.put(embed.getJson())
                    }
                    event.channel.sendFile(array.toString(4).toByteArray(), "${messageId}.json").queue()
                }
            }
            else
            {
                event.channel.sendMessage("There is no embeds in this message!").queue()
            }
        }
    }

    fun getJson(
            description: String?,
            timestamp: OffsetDateTime?,
            image: MessageEmbed.ImageInfo?,
            thumbnail: MessageEmbed.Thumbnail?,
            colorRaw: Int,
            title: String?,
            title_url: String?,
            author: MessageEmbed.AuthorInfo?,
            footer: MessageEmbed.Footer?,
            fields: Collection<MessageEmbed.Field>
    ): JSONObject
    {
        val json = JSONObject()

        if(!description.isNullOrBlank()) json.put(JSONEmbedBuilder.JsonName.DESCRIPTION.directory, description)
        if(timestamp != null) json.put(JSONEmbedBuilder.JsonName.TIMESTAMP.directory, timestamp.toEpochSecond())
        if(image != null) json.put(JSONEmbedBuilder.JsonName.IMAGE.directory, image.url)
        if(thumbnail != null) json.put(JSONEmbedBuilder.JsonName.THUMBNAIL.directory, thumbnail.url)
        json.put(JSONEmbedBuilder.JsonName.COLOUR.directory, colorRaw)

        if(title != null)
        {
            json.put(JSONEmbedBuilder.JsonName.TITLE.directory, JSONObject()
                    .put(JSONEmbedBuilder.JsonName.TITLE_TEXT.directory, title)
                    .put(JSONEmbedBuilder.JsonName.TITLE_URL.directory, title_url)
            )
        }

        if(author != null)
        {
            json.put(JSONEmbedBuilder.JsonName.AUTHOR.directory, JSONObject()
                    .put(JSONEmbedBuilder.JsonName.AUTHOR_TEXT.directory, author.name)
                    .put(JSONEmbedBuilder.JsonName.AUTHOR_URL.directory, author.url)
                    .put(JSONEmbedBuilder.JsonName.AUTHOR_ICON_URL.directory, author.iconUrl)
            )
        }

        if(footer != null)
        {
            json.put(JSONEmbedBuilder.JsonName.FOOTER.directory, JSONObject()
                    .put(JSONEmbedBuilder.JsonName.FOOTER_TEXT.directory, footer.text)
                    .put(JSONEmbedBuilder.JsonName.FOOTER_URL.directory, footer.iconUrl)
            )
        }

        if(fields.isNotEmpty())
        {
            val jsonFields = JSONArray()
            for(field in fields)
            {
                jsonFields.put(JSONObject()
                        .put(JSONEmbedBuilder.JsonName.FIELDS_NAME.directory, field.name)
                        .put(JSONEmbedBuilder.JsonName.FIELDS_VALUE.directory, field.value)
                        .put(JSONEmbedBuilder.JsonName.FIELDS_INLINE.directory, field.isInline)
                )
            }
            json.put(JSONEmbedBuilder.JsonName.FIELDS.directory, jsonFields)
        }

        return json
    }
}

fun MessageEmbed.getJson(): JSONObject
{
    return EmbedGetCommand.getJson(description, timestamp, image, thumbnail, colorRaw, title, url, author, footer, fields)
}