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

object PlayCommand : MusicCommand(
        "play",
        connect = true,
        botPermissions = listOf(Permission.MESSAGE_ADD_REACTION)
)
{
    //private val youtube = YouTube.Builder(NetHttpTransport(), JacksonFactory()) {}.setApplicationName("youtube-cmdline-search-sample").build()

    override fun onSelfNotConnected(event: MessageReceivedEvent, args: List<String>)
    {
        val channel = event.member!!.voiceState!!.channel!!
        if(connect(channel))
        {
            run(event, args)
        }
        else
        {
            event.channel.sendMessage("I do not have the following permissions for voice channel `${channel.name}`; `${fullBotPermissions.joinToString("`, `")}`").queue()
        }
    }

    override fun run(event: MessageReceivedEvent, args: List<String>)
    {
        if(args.isNotEmpty())
        {
            val search = args.joinToString(" ")
            event.channel.sendMessage("Searching for `${search.maxLength()}`...").queue()
            { message ->
                val searchArgument = if(!isUrl(search))
                {
                    // Creates the query for youtube
                    val queryUrl = "https://www.youtube.com/results?search_query=${search.replace("\\s+".toRegex(), "+")}"

                    // Gets the full html response from youtube
                    val response = IOUtils.toString(URL(queryUrl), Charset.forName("UTF-8"))
                    val matcher = Pattern.compile("\"videoId(s)?\":\"\\w+\"").matcher(response)

                    // Filters the full html response for the first video id it finds, if there is none there were no results with that query
                    val videoId = if(matcher.find()) matcher.group() else null
                    if(videoId != null)
                    {
                        "youtube.com/watch?v=${JSONObject("{$videoId}")["videoId"]}"
                    }
                    else
                    {
                        message.editMessage("I could not find anything relating to`${search.maxLength()}`. Please try different keywords.").queue()
                        return@queue
                    }

                    /*
                    I am searching for videos this way instead of using the official youtube data api
                    as i could only use 10,000 quotas per day and searching a video would cost 100 quotas.
                    The bot at the moment is too small for me to apply to a bigger allowance so i came up with this
                    solution, as it will let me query more videos.
                     */
                }
                else
                {
                    search
                }
                GuildMusicManager.get(event.guild).loadAndPlay(message, searchArgument)
            }
        }
        else
        {
            event.channel.sendMessage("Please provide the song url or keywords of the song that you would like to play").queue()
        }
    }

    private fun isUrl(input: String): Boolean
    {
        return try
        {
            URL(input)
            true
        } catch (e: MalformedURLException)
        {
            false
        }
    }
}