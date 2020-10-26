package me.qbosst.bossbot.util

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.utils.data.DataObject
import net.dv8tion.jda.internal.entities.EntityBuilder

object EmbedUtil
{
    fun parseEmbed(jda: JDA, data: DataObject, success: (MessageEmbed) -> Unit, error: (Exception, EmbedVariable) -> Unit)
    {
        if(!data.hasKey("type"))
            data.put("type", "rich")

        val raw = try { EntityBuilder(jda).createMessageEmbed(data) } catch (e: Exception) { error.invoke(e, EmbedVariable.INIT ); return }
        val embed = EmbedBuilder()

        try { embed.setDescription(raw.description) } catch (e: Exception) { error.invoke(e, EmbedVariable.DESCRIPTION); return }
        try { embed.setTitle(raw.title, raw.url) } catch (e: Exception) { error.invoke(e, EmbedVariable.TITLE); return }
        try { embed.setTimestamp(raw.timestamp) } catch (e: Exception) { error.invoke(e, EmbedVariable.TIMESTAMP); return }
        try { embed.setColor(raw.colorRaw) } catch (e: Exception) { error.invoke(e, EmbedVariable.COLOUR); return }
        try { embed.setThumbnail(raw.thumbnail?.url) } catch (e: Exception) { error.invoke(e, EmbedVariable.THUMBNAIL); return }
        try { embed.setAuthor(raw.author?.name, raw.author?.url, raw.author?.iconUrl) } catch (e: Exception) { error.invoke(e, EmbedVariable.AUTHOR ); return }
        try { embed.setFooter(raw.footer?.text, raw.footer?.iconUrl) } catch (e: Exception) { error.invoke(e, EmbedVariable.FOOTER); return  }
        try { embed.setImage(raw?.image?.url) } catch (e: Exception) { error.invoke(e, EmbedVariable.IMAGE); return  }
        try { for (field in raw.fields) embed.addField(field) } catch (e: Exception) { error.invoke(e, EmbedVariable.FIELD); return  }
        try { success.invoke(embed.build()) } catch (e: Exception) { error.invoke(e, EmbedVariable.BUILD ); return }
    }

    enum class EmbedVariable(
        varName: String,
        val errorMessage: (Exception) -> String = {"Failed to create ${varName}: `${it.localizedMessage}`"})
    {
        FIELD("Field"),
        DESCRIPTION("Description"),
        COLOUR("Colour"),
        TITLE("Title"),
        TIMESTAMP("Timestamp"),
        THUMBNAIL("Thumbnail"),
        AUTHOR("Author"),
        FOOTER("Footer"),
        IMAGE("Image"),
        BUILD("Embed"),
        INIT("Embed")
        ;
    }
}