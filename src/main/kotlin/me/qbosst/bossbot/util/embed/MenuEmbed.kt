package me.qbosst.bossbot.util.embed

import me.qbosst.bossbot.util.assertNumber
import net.dv8tion.jda.api.EmbedBuilder

/**
 *  Class to make menu themed embeds.
 *
 *  @param maxObjectsPerPage The max amount of objects that can be in the page
 *  @param objects The objects to create the page from
 */
abstract class MenuEmbed<T>(private val maxObjectsPerPage: Int, protected val objects: List<T>)
{
    /**
     *  Creates the menu embed
     *
     *  @param embed The embed to create the menu on
     *  @param page The page of the menu to create
     *
     *  @return EmbedBuilder that has the menu on it
     */
    fun createPage(embed: EmbedBuilder, page: Int): EmbedBuilder
    {
        // Clear any records in the embed
        clearMenu(embed)
        val maxPages = getMaxPages()
        val pg = assertNumber(0, maxPages, page-1)

        // Adds the records into the page
        for (i in maxObjectsPerPage * pg until
                if (maxObjectsPerPage * pg + maxObjectsPerPage > objects.size)
                    objects.size
                else
                    maxObjectsPerPage * pg + maxObjectsPerPage
        ) {
            addObjectToPage(embed, i)
        }

        if(isEmpty(embed))
            embed.appendDescription("None!")
        else
        {
            // Adds a page number
            val sb = StringBuilder(embed.build().footer?.text ?: "")
            if(sb.isNotEmpty())
                sb.append(" | ")
            sb.append("Page ${pg+1} / ${maxPages+1}")
            embed.setFooter(sb.toString())
        }
        return embed
    }

    /**
     *  Adds a record onto the menu
     *
     *  @param embed The embed to add the record to
     *  @param index The index of the record needing to be added
     */
    protected abstract fun addObjectToPage(embed: EmbedBuilder, index: Int)

    /**
     *  Checks if the menu is empty
     *
     *  @param embed The embed to check if empty
     *
     *  @return whether the embed is empty or not
     */
    abstract fun isEmpty(embed: EmbedBuilder): Boolean

    /**
     *  Clears the menu
     *
     *  @param embed The embed to clear the menu of
     */
    abstract fun clearMenu(embed: EmbedBuilder)

    /**
     *  Gets the maximum amount of pages that the embed has
     *
     *  @return The maximum amount of pages
     */
    private fun getMaxPages(): Int
    {
        var maxPages: Int = objects.size / maxObjectsPerPage
        if(objects.size % maxObjectsPerPage == 0 && maxPages > 0)
            maxPages -= 1

        return maxPages
    }
}