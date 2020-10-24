package me.qbosst.bossbot.util.embed

import net.dv8tion.jda.api.EmbedBuilder

class DescriptionMenuEmbed(maxObjectsPerPage: Int, objects: List<String>) : MenuEmbed<String>(maxObjectsPerPage, objects)
{

    override fun addObjectToPage(embed: EmbedBuilder, index: Int)
    {
        embed.appendDescription(objects[index])
    }

    override fun isEmpty(embed: EmbedBuilder): Boolean = embed.descriptionBuilder.isEmpty()

    override fun clearMenu(embed: EmbedBuilder)
    {
        embed.descriptionBuilder.clear()
    }
}