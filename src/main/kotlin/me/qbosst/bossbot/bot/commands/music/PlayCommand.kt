package me.qbosst.bossbot.bot.commands.music

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import me.qbosst.bossbot.util.makeSafe
import me.qbosst.bossbot.util.maxLength
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object PlayCommand: MusicCommand(
        "play",
        "Plays a tracks from a url or keywords provided",
        usages = listOf("<track url>", "<track name>"),
        examples = listOf(
                "https://open.spotify.com/track/3DlgDXIYtnWtJKiB8bZQMv",
                "spotify:track:7Es2OBtD2DPHfz6gqSWH8Z",
                "https://www.youtube.com/watch?v=Z6V398wkgKk",
                "Energy (Stay Far Away) Skepta"
        ),
        autoConnect = true
)
{
    override fun run(event: MessageReceivedEvent, args: List<String>)
    {
        if(args.isNotEmpty())
        {
            val query = args.joinToString(" ")
            val handler = event.guild.getAudioHandler()

            event.channel.sendMessage("Loading `${query.makeSafe().maxLength(64)}`...").queue() { message ->
                handler.loadAndPlay(query, object: AudioLoadResultHandler
                {
                    override fun trackLoaded(track: AudioTrack)
                    {
                        track.userData = event.author.idLong
                        handler.queue(track)
                        message.editMessage("Added `${track.info.title.maxLength(64)}` to queue").queue()
                    }

                    override fun playlistLoaded(playlist: AudioPlaylist)
                    {
                        if(playlist.isSearchResult)
                        {
                            trackLoaded(playlist.tracks.first())
                        }
                        else
                        {
                            for(track in playlist.tracks)
                            {
                                track.userData = event.author.idLong
                                handler.queue(track)
                            }
                            message.editMessage("Added `${playlist.tracks.size}` tracks from the playlist `${playlist.name.makeSafe().maxLength(64)}` to queue").queue()
                        }
                    }

                    override fun noMatches()
                    {
                        message.editMessage("I could not find anything from `${query.makeSafe().maxLength(64)}`.")
                                .queue()
                    }

                    override fun loadFailed(exception: FriendlyException)
                    {
                        message.editMessage("Could not load track: `${exception.localizedMessage}`").queue()
                    }
                })
            }
        }
        else
            event.channel.sendMessage("Please provide a url or keywords for the track you would like to play").queue()
    }
}