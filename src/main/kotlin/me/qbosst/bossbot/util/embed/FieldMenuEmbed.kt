package me.qbosst.bossbot.util.embed

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed

class FieldMenuEmbed(maxObjectsPerPage: Int, objects: List<MessageEmbed.Field>) : MenuEmbed<MessageEmbed.Field>(maxObjectsPerPage, objects)
{

    override fun addObjectToPage(embed: EmbedBuilder, index: Int)
    {
        embed.addField(objects[index])
    }

    override fun isEmpty(embed: EmbedBuilder): Boolean = embed.fields.isEmpty()

    override fun clearMenu(embed: EmbedBuilder)
    {
        embed.clearFields()
    }

}