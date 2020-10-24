package music

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack

class AudioResultHandler: AudioLoadResultHandler
{
    override fun trackLoaded(track: AudioTrack?) {
        TODO("Not yet implemented")
    }

    override fun playlistLoaded(playlist: AudioPlaylist?) {
        TODO("Not yet implemented")
    }

    override fun noMatches() {
        TODO("Not yet implemented")
    }

    override fun loadFailed(exception: FriendlyException?) {
        TODO("Not yet implemented")
    }

}