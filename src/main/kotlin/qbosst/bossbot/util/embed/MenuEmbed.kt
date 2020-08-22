package qbosst.bossbot.util.embed

import net.dv8tion.jda.api.EmbedBuilder
import qbosst.bossbot.util.assertNumber

abstract class MenuEmbed<T>(private val maxObjectsPerPage: Int, protected val objects: List<T>)
{

    fun createPage(embed: EmbedBuilder, page: Int): EmbedBuilder
    {
        clearMenu(embed)
        val maxPages = getMaxPages()
        val pg = assertNumber(0, maxPages, page-1)

        for (i in maxObjectsPerPage * pg until
                if (maxObjectsPerPage * pg + maxObjectsPerPage > objects.size)
                {
                    objects.size
                }
                else {
                    maxObjectsPerPage * pg + maxObjectsPerPage
                })
        {
            addObjectToPage(embed, i)
        }

        if(isEmpty(embed))
        {
            embed.appendDescription("None!")
        }
        else
        {
            val sb = StringBuilder(embed.build().footer?.text ?: "")
            if(sb.isNotEmpty()) sb.append(" | ")
            sb.append("Page ${pg+1} / ${maxPages+1}")
            embed.setFooter(sb.toString())
        }
        return embed
    }

    protected abstract fun addObjectToPage(embed: EmbedBuilder, index: Int)

    abstract fun isEmpty(embed: EmbedBuilder): Boolean

    abstract fun clearMenu(embed: EmbedBuilder)

    private fun getMaxPages(): Int
    {
        var maxPages: Int = objects.size / maxObjectsPerPage
        if(objects.size % maxObjectsPerPage == 0 && maxPages > 0)
        {
            maxPages -= 1
        }
        return maxPages
    }
}