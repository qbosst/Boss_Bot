package music

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager

class GuildMusicManager(val manager: AudioPlayerManager)
{
    val player = manager.createPlayer()
    val scheduler = TrackScheduler(player)

    init {
        player.addListener(scheduler)
    }

    fun getSendHandler(): AudioPlayerSendHandler = AudioPlayerSendHandler(player)
}