package me.qbosst.bossbot.bot.commands.music

import me.qbosst.bossbot.entities.music.GuildMusicManager
import me.qbosst.bossbot.util.maxLength
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.apache.commons.io.IOUtils
import org.json.JSONObject
import java.net.MalformedURLException
import java.net.URL
import java.nio.charset.Charset
import java.util.regex.Pattern

object PlayCommand: MusicCommand(
        "play",
        description = "Plays music through the bot",
        usage = listOf("<track url|track name|playlist>"),
        botPermissions = listOf(Permission.VOICE_SPEAK),
        connect = true
)
{
    override fun run(event: MessageReceivedEvent, args: List<String>)
    {
        if(args.isNotEmpty())
        {
            val query = args.joinToString(" ")
            event.channel.sendMessage("Searching for... `${query.maxLength(64)}`").queue()
            {
                val searchArgument = if(!isUrl(query))
                {
                    // Creates the query for youtube
                    val queryUrl = "https://www.youtube.com/results?search_query=${query.replace(Regex("\\s+"), "+")}"

                    // Gets the full HTML response from youtube
                    val response = IOUtils.toString(URL(queryUrl), Charset.forName("UTF-8"))
                    val matcher = Pattern.compile("\"videoId(s)?\":\"\\w+\"").matcher(response)

                    // Filters the full HTML response for the first video id it finds, if there is none there were no results with that query
                    val videoId = if(matcher.find()) matcher.group() else null
                    if(videoId != null)
                        "youtube.com/watch?v=${JSONObject("{$videoId}")["videoId"]}"
                    else
                    {
                        it.editMessage("I could not find anything relating to `${query.maxLength()}`. Please try different keywords.").queue()
                        return@queue
                    }
                } else query

                GuildMusicManager.get(event.guild).scheduler.loadAndPlay(it, searchArgument)
            }
        }
        else
            event.channel.sendMessage("Please provide the song you would like to play").queue()
    }

    private fun isUrl(input: String): Boolean = try { URL(input); true } catch (e: MalformedURLException) { false }
}