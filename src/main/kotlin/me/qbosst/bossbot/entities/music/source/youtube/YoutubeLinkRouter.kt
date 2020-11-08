package me.qbosst.bossbot.entities.music.source.youtube

import com.sedmelluq.discord.lavaplayer.source.youtube.DefaultYoutubeLinkRouter
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeLinkRouter
import java.util.regex.Pattern

class YoutubeLinkRouter: DefaultYoutubeLinkRouter()
{

    override fun <T : Any?> route(link: String, routes: YoutubeLinkRouter.Routes<T>?): T
    {
        val new = when {
            link.startsWith(SEARCH_PREFIX) ->
                link
            isURL(link) ->
                link
            else ->
                "$SEARCH_PREFIX$link"
        }

        return super.route(new, routes)
    }

    private fun isURL(url: String): Boolean = URL_MATCHER.matcher(url).matches()

    companion object {
        private const val SEARCH_PREFIX = "ytsearch:"

        private const val URL_REGEX = "https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&\\/\\/=]*)\$"
        private val URL_MATCHER = Pattern.compile(URL_REGEX)
    }
}