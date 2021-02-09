package me.qbosst.bossbot.util.embed

import dev.kord.rest.builder.message.EmbedBuilder

class MenuEmbed<T>(
    private val maxPerPage: Int,
    val objects: List<T>,
    private val pageBuilder: EmbedBuilder.(T, Boolean) -> Unit
) {

    fun buildPage(page: Int = 0, embed: EmbedBuilder = EmbedBuilder()) {
        val maxPages = getMaxPages()
        val pg = (page-1).coerceIn(0, maxPages)

        var isFirst = true
        for(i in maxPerPage*pg until (maxPerPage * pg + maxPerPage).coerceAtMost(objects.size)) {
            pageBuilder.invoke(embed, objects[i], isFirst)
            isFirst = false
        }

        embed.footer {
            this.text = buildString {
                if(this.isNotEmpty()) {
                    append(" | ")
                }
                append("Page ${pg+1} / ${maxPages+1}")
            }
        }
    }

    private fun getMaxPages(): Int {
        var maxPages = objects.size / maxPerPage
        if(objects.size % maxPerPage == 0 && maxPages > 0) {
            maxPages -= 1
        }
        return maxPages
    }
}